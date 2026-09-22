package com.ethlo.time.internal.util;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.ethlo.time.internal.DateTimeFormatException;

public final class LimitedCharArrayIntegerUtil
{
    public static final char DIGIT_9 = '9';
    public static final char ZERO = '0';
    /**
     * The widest field this class is asked to render (nanosecond fractions)
     */
    public static final int MAX_WIDTH = 9;
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final int TABLE_WIDTH = 4;
    private static final int RADIX = 10;
    private static final int TABLE_SIZE = 10_000;
    private static final int[] POW10 = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000};
    private static final char[] INT_CONVERSION_CACHE = new char[TABLE_SIZE * TABLE_WIDTH];

    static
    {
        int offset = 0;
        for (int i = 0; i < TABLE_SIZE; i++)
        {
            writeDigits(INT_CONVERSION_CACHE, offset, TABLE_WIDTH, i);
            offset += TABLE_WIDTH;
        }
    }

    private LimitedCharArrayIntegerUtil()
    {
    }

    /**
     * Straight-line parse of exactly two digits. Same error behaviour as {@link #parsePositiveInt(String, int, int)}.
     * <p>
     * NOTE: Kept deliberately small (no try/catch, a single slow-path call) so the JIT inlines it into the callers.
     * <p>
     * NOTE: {@code c ^ '0'} is the digit value for '0'..'9' and is at least 10 for every other char, because a char is
     * never negative and the xor keeps every bit above the low nibble. One signed compare per digit therefore replaces
     * the subtract, the negative test and the upper-bound test; C2 does not otherwise emit an unsigned compare for the
     * {@code x + MIN_VALUE} or {@code Integer.compareUnsigned} idioms (checked in the disassembly, perf-log S4.2).
     */
    public static int parse2(final String s, final int start)
    {
        if (start + 2 <= s.length())
        {
            final int d0 = s.charAt(start) ^ ZERO;
            final int d1 = s.charAt(start + 1) ^ ZERO;
            if (d0 <= 9 && d1 <= 9)
            {
                return d0 * 10 + d1;
            }
        }
        return parsePositiveInt(s, start, start + 2);
    }

    /**
     * Straight-line parse of exactly four digits. Same error behaviour as {@link #parsePositiveInt(String, int, int)}.
     * <p>
     * NOTE: Kept deliberately small (no try/catch, a single slow-path call) so the JIT inlines it into the callers.
     */
    public static int parse4(final String s, final int start)
    {
        if (start + 4 <= s.length())
        {
            final int d0 = s.charAt(start) ^ ZERO;
            final int d1 = s.charAt(start + 1) ^ ZERO;
            final int d2 = s.charAt(start + 2) ^ ZERO;
            final int d3 = s.charAt(start + 3) ^ ZERO;
            if (d0 <= 9 && d1 <= 9 && d2 <= 9 && d3 <= 9)
            {
                return d0 * 1000 + d1 * 100 + d2 * 10 + d3;
            }
        }
        return parsePositiveInt(s, start, start + 4);
    }

    /**
     * {@link #parse2(String, int)} when the caller has already established that {@code start + 2 <= s.length()},
     * so the fast path is the digit test alone. The slow path is the same, for the same message.
     */
    public static int parse2In(final String s, final int start)
    {
        final int d0 = s.charAt(start) ^ ZERO;
        final int d1 = s.charAt(start + 1) ^ ZERO;
        if (d0 <= 9 && d1 <= 9)
        {
            return d0 * 10 + d1;
        }
        return parsePositiveInt(s, start, start + 2);
    }

    /**
     * {@link #parse4(String, int)} when the caller has already established that {@code start + 4 <= s.length()}.
     */
    public static int parse4In(final String s, final int start)
    {
        final int d0 = s.charAt(start) ^ ZERO;
        final int d1 = s.charAt(start + 1) ^ ZERO;
        final int d2 = s.charAt(start + 2) ^ ZERO;
        final int d3 = s.charAt(start + 3) ^ ZERO;
        if (d0 <= 9 && d1 <= 9 && d2 <= 9 && d3 <= 9)
        {
            return d0 * 1000 + d1 * 100 + d2 * 10 + d3;
        }
        return parsePositiveInt(s, start, start + 4);
    }

    /**
     * {@link #parse2(String, int)} over a window of a {@code char[]}. The window bounds the fast path; the slow
     * path re-parses the window as a String so the error message and index are identical to the String path,
     * with the index relative to {@code windowStart}.
     */
    public static int parse2(final char[] s, final int start, final int windowStart, final int windowEnd)
    {
        if (start + 2 <= windowEnd)
        {
            final int d0 = s[start] ^ ZERO;
            final int d1 = s[start + 1] ^ ZERO;
            if (d0 <= 9 && d1 <= 9)
            {
                return d0 * 10 + d1;
            }
        }
        return parsePositiveInt(new String(s, windowStart, windowEnd - windowStart), start - windowStart, start - windowStart + 2);
    }

    /**
     * {@link #parse4(String, int)} over a window of a {@code char[]}, see {@link #parse2(char[], int, int, int)}.
     */
    public static int parse4(final char[] s, final int start, final int windowStart, final int windowEnd)
    {
        if (start + 4 <= windowEnd)
        {
            final int d0 = s[start] ^ ZERO;
            final int d1 = s[start + 1] ^ ZERO;
            final int d2 = s[start + 2] ^ ZERO;
            final int d3 = s[start + 3] ^ ZERO;
            if (d0 <= 9 && d1 <= 9 && d2 <= 9 && d3 <= 9)
            {
                return d0 * 1000 + d1 * 100 + d2 * 10 + d3;
            }
        }
        return parsePositiveInt(new String(s, windowStart, windowEnd - windowStart), start - windowStart, start - windowStart + 4);
    }

    /**
     * {@link #parse2(char[], int, int, int)} when the caller has already established that the two characters at
     * {@code base + rel} are inside the window, so the fast path is the digit test alone. The slow path is the same,
     * for the same message.
     * <p>
     * The position is taken as {@code base + rel} rather than as one {@code start} argument on purpose: the slow path
     * is an uncommon trap once C2 has inlined this, and every local the interpreter would need there has to be
     * materialised in a register or stack slot before the branch. A {@code start = offset + 5} local is one such value
     * per call; {@code base} is the caller's window start, already live, and {@code rel} a constant that costs nothing
     * (perf-log S6.6).
     */
    public static int parse2In(final char[] s, final int base, final int rel, final int windowStart, final int windowEnd)
    {
        final int d0 = s[base + rel] ^ ZERO;
        final int d1 = s[base + rel + 1] ^ ZERO;
        if (d0 <= 9 && d1 <= 9)
        {
            return d0 * 10 + d1;
        }
        return parsePositiveInt(new String(s, windowStart, windowEnd - windowStart), base + rel - windowStart, base + rel - windowStart + 2);
    }

    /**
     * {@link #parse4(char[], int, int, int)} when the caller has already established that the four characters at
     * {@code base + rel} are inside the window. See {@link #parse2In} for why the position is two arguments.
     */
    public static int parse4In(final char[] s, final int base, final int rel, final int windowStart, final int windowEnd)
    {
        final int d0 = s[base + rel] ^ ZERO;
        final int d1 = s[base + rel + 1] ^ ZERO;
        final int d2 = s[base + rel + 2] ^ ZERO;
        final int d3 = s[base + rel + 3] ^ ZERO;
        if (d0 <= 9 && d1 <= 9 && d2 <= 9 && d3 <= 9)
        {
            return d0 * 1000 + d1 * 100 + d2 * 10 + d3;
        }
        return parsePositiveInt(new String(s, windowStart, windowEnd - windowStart), base + rel - windowStart, base + rel - windowStart + 4);
    }

    public static int parsePositiveInt(final String strNum, int startInclusive, int endExclusive)
    {
        int result = 0;
        try
        {
            for (int i = startInclusive; i < endExclusive; i++)
            {
                final char c = strNum.charAt(i);
                if (c < ZERO || c > DIGIT_9)
                {
                    ErrorUtil.raiseUnexpectedCharacter(strNum, i, '0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
                }
                result = (result * 10) + (c - ZERO);
            }
        }
        catch (StringIndexOutOfBoundsException exc)
        {
            ErrorUtil.raiseUnexpectedEndOfText(strNum, startInclusive);
        }

        return result;
    }

    /**
     * Writes <code>value</code> into <code>buf</code> as exactly <code>charLength</code> zero-padded digits.
     * <p>
     * The value must fit in the requested number of characters. Silently truncating here was the cause of
     * malformed output for out-of-range years and for fractional seconds, so it is now rejected outright.
     *
     * @param value      The non-negative value to write
     * @param buf        The buffer to write into
     * @param offset     The offset in the buffer to start writing at
     * @param charLength The exact number of characters to write
     * @throws DateTimeFormatException if the value cannot be represented in the given number of characters
     */
    public static void toString(final int value, final char[] buf, final int offset, final int charLength)
    {
        if (charLength < 1 || charLength > MAX_WIDTH || value < 0 || value >= POW10[charLength])
        {
            throw new DateTimeFormatException("Value " + value + " cannot be represented in " + charLength + " character(s)");
        }

        if (charLength <= TABLE_WIDTH)
        {
            // value < 10^charLength <= 10^4, so it is always within the cached range
            final int srcPos = (value * TABLE_WIDTH) + (TABLE_WIDTH - charLength);
            System.arraycopy(INT_CONVERSION_CACHE, srcPos, buf, offset, charLength);
            return;
        }

        writeDigits(buf, offset, charLength, value);
    }

    /**
     * Writes the value right-aligned and zero-padded into <code>[offset, offset + charLength)</code>, touching
     * no other part of the buffer. The caller has already verified that the value fits.
     */
    private static void writeDigits(final char[] buf, final int offset, final int charLength, int value)
    {
        int pos = offset + charLength - 1;
        while (pos >= offset)
        {
            buf[pos--] = DIGITS[value % RADIX];
            value /= RADIX;
        }
    }

    /**
     * Scales a nanosecond value down to the given number of fraction digits, truncating the remainder.
     *
     * @param nano           The nanosecond value, 0 - 999,999,999
     * @param fractionDigits The number of fraction digits to keep, 1 - 9
     * @return The scaled value, which is guaranteed to fit in <code>fractionDigits</code> digits
     */
    /**
     * "00".."99" as consecutive pairs, so a two-digit field is two loads and two stores with no division and no
     * call. {@link #toString(int, char[], int, int)} copies from the four-digit table with
     * {@code System.arraycopy}, which for two characters is call overhead rather than a copy (perf-log S10.1).
     */
    private static final char[] PAIRS = new char[200];

    static
    {
        for (int i = 0; i < 100; i++)
        {
            PAIRS[i << 1] = (char) ('0' + i / 10);
            PAIRS[(i << 1) + 1] = (char) ('0' + i % 10);
        }
    }

    /**
     * Writes a value in [0, 99] as two digits. The caller guarantees the range; a calendar field read from
     * {@code java.time} is in it by construction.
     */
    public static void write2(final char[] buf, final int offset, final int value)
    {
        buf[offset] = PAIRS[value << 1];
        buf[offset + 1] = PAIRS[(value << 1) + 1];
    }

    /**
     * Writes a value in [0, 9999] as four digits: two pairs, one division.
     */
    public static void write4(final char[] buf, final int offset, final int value)
    {
        final int hi = value / 100;
        write2(buf, offset, hi);
        write2(buf, offset + 2, value - hi * 100);
    }

    /**
     * Writes a value in [0, 999] as three digits: one digit and a pair.
     */
    private static void write3(final char[] buf, final int offset, final int value)
    {
        final int hi = value / 100;
        buf[offset] = (char) ('0' + hi);
        write2(buf, offset + 1, value - hi * 100);
    }

    private static final byte[] PAIRS_BYTES = new byte[200];

    static
    {
        for (int i = 0; i < 200; i++)
        {
            PAIRS_BYTES[i] = (byte) PAIRS[i];
        }
    }

    /**
     * {@link #write2(char[], int, int)} into a Latin-1 {@code byte[]}, for formatting straight into a byte
     * destination. Not for a {@code String} result: {@code String(char[], int, int)} compresses with a SIMD
     * intrinsic and beats the {@code Charset} constructor over a {@code byte[]} (perf-log S10.2).
     */
    public static void write2(final byte[] buf, final int offset, final int value)
    {
        buf[offset] = PAIRS_BYTES[value << 1];
        buf[offset + 1] = PAIRS_BYTES[(value << 1) + 1];
    }

    public static void write4(final byte[] buf, final int offset, final int value)
    {
        final int hi = value / 100;
        write2(buf, offset, hi);
        write2(buf, offset + 2, value - hi * 100);
    }

    private static void write3(final byte[] buf, final int offset, final int value)
    {
        final int hi = value / 100;
        buf[offset] = (byte) ('0' + hi);
        write2(buf, offset + 1, value - hi * 100);
    }

    /**
     * {@link #writeFraction(char[], int, int, int)} into a Latin-1 {@code byte[]}.
     */
    public static void writeFraction(final byte[] buf, final int offset, final int nano, final int fractionDigits)
    {
        switch (fractionDigits)
        {
            case 3:
                write3(buf, offset, nano / 1_000_000);
                return;
            case 6:
            {
                final int micros = nano / 1_000;
                final int millis = micros / 1_000;
                write3(buf, offset, millis);
                write3(buf, offset + 3, micros - millis * 1_000);
                return;
            }
            case 9:
            {
                final int millis = nano / 1_000_000;
                final int rest = nano - millis * 1_000_000;
                final int micros = rest / 1_000;
                write3(buf, offset, millis);
                write3(buf, offset + 3, micros);
                write3(buf, offset + 6, rest - micros * 1_000);
                return;
            }
            default:
            {
                int value = scaleNanos(nano, fractionDigits);
                for (int i = offset + fractionDigits - 1; i >= offset; i--)
                {
                    buf[i] = (byte) ('0' + value % RADIX);
                    value /= RADIX;
                }
            }
        }
    }

    /**
     * Writes the first {@code fractionDigits} digits of a nanosecond value, in [0, 999 999 999], truncating the
     * rest. Three, six and nine digits are written as groups of three with one division each; any other count
     * goes through the general path.
     */
    public static void writeFraction(final char[] buf, final int offset, final int nano, final int fractionDigits)
    {
        switch (fractionDigits)
        {
            case 3:
                write3(buf, offset, nano / 1_000_000);
                return;
            case 6:
            {
                final int micros = nano / 1_000;
                final int millis = micros / 1_000;
                write3(buf, offset, millis);
                write3(buf, offset + 3, micros - millis * 1_000);
                return;
            }
            case 9:
            {
                final int millis = nano / 1_000_000;
                final int rest = nano - millis * 1_000_000;
                final int micros = rest / 1_000;
                write3(buf, offset, millis);
                write3(buf, offset + 3, micros);
                write3(buf, offset + 6, rest - micros * 1_000);
                return;
            }
            default:
                toString(scaleNanos(nano, fractionDigits), buf, offset, fractionDigits);
        }
    }

    public static int scaleNanos(final int nano, final int fractionDigits)
    {
        return nano / POW10[MAX_WIDTH - fractionDigits];
    }
}
