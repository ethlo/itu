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
     */
    public static int parse2(final String s, final int start)
    {
        if (start + 2 <= s.length())
        {
            final int d0 = s.charAt(start) - ZERO;
            final int d1 = s.charAt(start + 1) - ZERO;
            if ((d0 | d1) >= 0 && d0 <= 9 && d1 <= 9)
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
            final int d0 = s.charAt(start) - ZERO;
            final int d1 = s.charAt(start + 1) - ZERO;
            final int d2 = s.charAt(start + 2) - ZERO;
            final int d3 = s.charAt(start + 3) - ZERO;
            if ((d0 | d1 | d2 | d3) >= 0 && d0 <= 9 && d1 <= 9 && d2 <= 9 && d3 <= 9)
            {
                return d0 * 1000 + d1 * 100 + d2 * 10 + d3;
            }
        }
        return parsePositiveInt(s, start, start + 4);
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
    public static int scaleNanos(final int nano, final int fractionDigits)
    {
        return nano / POW10[MAX_WIDTH - fractionDigits];
    }
}
