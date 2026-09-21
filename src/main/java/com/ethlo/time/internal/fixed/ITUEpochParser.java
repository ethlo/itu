package com.ethlo.time.internal.fixed;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2026 Morten Haraldsen @ethlo
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

import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.DIGIT_9;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.ZERO;

import java.time.format.DateTimeParseException;

import com.ethlo.time.DateTime;
import com.ethlo.time.Field;
import com.ethlo.time.MutableDateTimeBuffer;
import com.ethlo.time.TimezoneOffset;
import com.ethlo.time.internal.util.DateTimeMath;
import com.ethlo.time.internal.util.ErrorUtil;

/**
 * Parses a Unix epoch count written as a decimal integer ({@code -?[0-9]+}) into civil fields at UTC. The digits
 * are walked here rather than by {@link Long#parseLong(String)} so that a bad character fails with the same kind
 * of exception and error index as the date-time parsers, and so that the {@code char[]} window path never copies.
 * <p>
 * The value must land in years [0, 9999], the range the rest of the library represents; the bounds are derived
 * from {@link DateTimeMath#daysFromCivil(int, int, int)} so that the two cannot drift.
 */
public class ITUEpochParser
{
    public static final long MIN_EPOCH_SECOND = DateTimeMath.daysFromCivil(0, 1, 1) * 86_400;
    public static final long MAX_EPOCH_SECOND = DateTimeMath.daysFromCivil(9999, 12, 31) * 86_400 + 86_399;
    private static final long MIN_EPOCH_MILLI = MIN_EPOCH_SECOND * 1_000;
    private static final long MAX_EPOCH_MILLI = MAX_EPOCH_SECOND * 1_000 + 999;

    /**
     * Seconds from 0000-01-01T00:00:00Z to the epoch. The conversion works on seconds since year 0 so that every
     * value it divides is non-negative (perf-log S7.1)
     */
    private static final long SECONDS_0000_TO_1970 = DateTimeMath.DAYS_0000_TO_1970 * 86_400;

    /**
     * Beyond this many digits the value is out of range whatever it is, and the accumulator would overflow
     */
    private static final int MAX_DIGITS = 18;

    private static final int MILLI_FRACTION_DIGITS = 3;

    private ITUEpochParser()
    {
    }

    public static DateTime parseEpochSecond(final String text)
    {
        final long seconds = parseLong(text, false);
        if (seconds < MIN_EPOCH_SECOND || seconds > MAX_EPOCH_SECOND)
        {
            throw raiseOutOfRange(text, false);
        }
        return toDateTime(seconds + SECONDS_0000_TO_1970, 0, Field.SECOND, 0, text.length());
    }

    public static DateTime parseEpochMilli(final String text)
    {
        final long millis = parseLong(text, true);
        if (millis < MIN_EPOCH_MILLI || millis > MAX_EPOCH_MILLI)
        {
            throw raiseOutOfRange(text, true);
        }
        final long millisSince0000 = millis - MIN_EPOCH_MILLI;
        final long seconds = millisSince0000 / 1_000;
        return toDateTime(seconds, (int) (millisSince0000 - seconds * 1_000) * 1_000_000, Field.NANO, MILLI_FRACTION_DIGITS, text.length());
    }

    public static int parseEpochSecond(final char[] chars, final int offset, final int length, final MutableDateTimeBuffer out)
    {
        sanityCheckInputParams(chars, offset, length, out);
        final long seconds = parseLong(chars, offset, length, false);
        if (seconds < MIN_EPOCH_SECOND || seconds > MAX_EPOCH_SECOND)
        {
            throw raiseOutOfRange(new String(chars, offset, length), false);
        }
        fill(out, seconds + SECONDS_0000_TO_1970, 0, Field.SECOND, 0, length);
        return length;
    }

    public static int parseEpochMilli(final char[] chars, final int offset, final int length, final MutableDateTimeBuffer out)
    {
        sanityCheckInputParams(chars, offset, length, out);
        final long millis = parseLong(chars, offset, length, true);
        if (millis < MIN_EPOCH_MILLI || millis > MAX_EPOCH_MILLI)
        {
            throw raiseOutOfRange(new String(chars, offset, length), true);
        }
        final long millisSince0000 = millis - MIN_EPOCH_MILLI;
        final long seconds = millisSince0000 / 1_000;
        fill(out, seconds, (int) (millisSince0000 - seconds * 1_000) * 1_000_000, Field.NANO, MILLI_FRACTION_DIGITS, length);
        return length;
    }

    /**
     * @param millis Which unit the caller is parsing, for the out-of-range message only
     */
    private static long parseLong(final String text, final boolean millis)
    {
        final int length = text.length();
        if (length == 0)
        {
            throw ErrorUtil.raiseUnexpectedEndOfText(text, 0);
        }
        final boolean negative = text.charAt(0) == '-';
        int idx = negative ? 1 : 0;
        if (idx == length)
        {
            throw ErrorUtil.raiseUnexpectedEndOfText(text, idx);
        }
        if (length - idx > MAX_DIGITS)
        {
            throw raiseOutOfRange(text, millis);
        }
        // Same shape as the char[] walk below (perf-log S7.6 / S7.8): the loop takes the leading digits, or all
        // of them when there are fewer than eight after the sign, and the last eight go straight-line
        final int tailStart = length - 8;
        final int loopEnd = tailStart > idx ? tailStart : length;
        long value = 0;
        for (; idx < loopEnd; idx++)
        {
            final int d = text.charAt(idx) ^ ZERO;
            if (d > 9)
            {
                throw raiseUnexpectedCharacter(text, idx, text.charAt(idx));
            }
            value = value * 10 + d;
        }
        if (loopEnd == tailStart)
        {
            final int d0 = text.charAt(tailStart) ^ ZERO;
            final int d1 = text.charAt(tailStart + 1) ^ ZERO;
            final int d2 = text.charAt(tailStart + 2) ^ ZERO;
            final int d3 = text.charAt(tailStart + 3) ^ ZERO;
            final int d4 = text.charAt(tailStart + 4) ^ ZERO;
            final int d5 = text.charAt(tailStart + 5) ^ ZERO;
            final int d6 = text.charAt(tailStart + 6) ^ ZERO;
            final int d7 = text.charAt(tailStart + 7) ^ ZERO;
            if (d0 > 9 || d1 > 9 || d2 > 9 || d3 > 9 || d4 > 9 || d5 > 9 || d6 > 9 || d7 > 9)
            {
                throw raiseUnexpectedCharacter(text, tailStart);
            }
            final int hi = (d0 * 10 + d1) * 100 + (d2 * 10 + d3);
            final int lo = (d4 * 10 + d5) * 100 + (d6 * 10 + d7);
            value = value * 100_000_000 + (hi * 10_000L + lo);
        }
        return negative ? -value : value;
    }

    /**
     * The straight-line block found a non-digit somewhere in its eight characters; find which for the message
     */
    private static DateTimeParseException raiseUnexpectedCharacter(final String text, final int from)
    {
        for (int idx = from; idx < text.length(); idx++)
        {
            final char c = text.charAt(idx);
            if (c < ZERO || c > DIGIT_9)
            {
                return raiseUnexpectedCharacter(text, idx, c);
            }
        }
        throw new IllegalStateException("No non-digit found: " + text);
    }

    /**
     * As {@link #parseLong(String)} over a window. Error messages and indices are relative to the window, as the
     * date-time {@code char[]} path reports them.
     */
    private static long parseLong(final char[] chars, final int offset, final int length, final boolean millis)
    {
        if (length == 0)
        {
            throw ErrorUtil.raiseUnexpectedEndOfText("", 0);
        }
        final boolean negative = chars[offset] == '-';
        int idx = negative ? 1 : 0;
        if (idx == length)
        {
            throw ErrorUtil.raiseUnexpectedEndOfText(new String(chars, offset, length), idx);
        }
        if (length - idx > MAX_DIGITS)
        {
            throw raiseOutOfRange(new String(chars, offset, length), millis);
        }
        // Two independent accumulations - the leading digits in the loop, the last eight straight-line - joined at
        // the end. The multiply-add chain of a single accumulator is the serial part of the walk (perf-log S7.6).
        // With fewer than eight digits after the sign the loop takes them all and the straight-line block is skipped
        final int tailStart = length - 8;
        final int loopEnd = tailStart > idx ? tailStart : length;
        long value = 0;
        for (; idx < loopEnd; idx++)
        {
            final int d = chars[offset + idx] ^ ZERO;
            if (d > 9)
            {
                throw raiseUnexpectedCharacter(new String(chars, offset, length), idx, chars[offset + idx]);
            }
            value = value * 10 + d;
        }
        if (loopEnd == tailStart)
        {
            final int base = offset + tailStart;
            final int d0 = chars[base] ^ ZERO;
            final int d1 = chars[base + 1] ^ ZERO;
            final int d2 = chars[base + 2] ^ ZERO;
            final int d3 = chars[base + 3] ^ ZERO;
            final int d4 = chars[base + 4] ^ ZERO;
            final int d5 = chars[base + 5] ^ ZERO;
            final int d6 = chars[base + 6] ^ ZERO;
            final int d7 = chars[base + 7] ^ ZERO;
            // Eight compares, as parse4 does: OR-ing the digit values would let 8|4 = 12 fail and ':' (10) pass
            if (d0 > 9 || d1 > 9 || d2 > 9 || d3 > 9 || d4 > 9 || d5 > 9 || d6 > 9 || d7 > 9)
            {
                throw raiseUnexpectedCharacter(new String(chars, offset, length), tailStart, chars, offset, length);
            }
            final int hi = (d0 * 10 + d1) * 100 + (d2 * 10 + d3);
            final int lo = (d4 * 10 + d5) * 100 + (d6 * 10 + d7);
            value = value * 100_000_000 + (hi * 10_000L + lo);
        }
        return negative ? -value : value;
    }

    /**
     * The straight-line block found a non-digit somewhere in its eight characters; find which for the message
     */
    private static DateTimeParseException raiseUnexpectedCharacter(final String text, final int from, final char[] chars, final int offset, final int length)
    {
        for (int idx = from; idx < length; idx++)
        {
            final char c = chars[offset + idx];
            if (c < ZERO || c > DIGIT_9)
            {
                return raiseUnexpectedCharacter(text, idx, c);
            }
        }
        throw new IllegalStateException("No non-digit found: " + text);
    }

    private static DateTimeParseException raiseUnexpectedCharacter(final String text, final int idx, final char c)
    {
        return new DateTimeParseException(String.format("Expected digit at position %d, found %s: %s", idx + 1, c, text), text, idx);
    }

    private static DateTimeParseException raiseOutOfRange(final String text, final boolean millis)
    {
        final long min = millis ? MIN_EPOCH_MILLI : MIN_EPOCH_SECOND;
        final long max = millis ? MAX_EPOCH_MILLI : MAX_EPOCH_SECOND;
        final String unit = millis ? "milliseconds" : "seconds";
        return new DateTimeParseException(String.format("Epoch value outside years 0000-9999 (%d to %d %s): %s", min, max, unit, text), text, 0);
    }

    /**
     * @param secondsSince0000 seconds since 0000-01-01T00:00:00Z, non-negative by the range check above
     */
    private static DateTime toDateTime(final long secondsSince0000, final int nano, final Field field, final int fractionDigits, final int parseLength)
    {
        final long days = secondsSince0000 / 86_400;
        final int date = DateTimeMath.civilFromDaysSince0000((int) days);
        final int secondOfDay = (int) (secondsSince0000 - days * 86_400);
        return new DateTime(field, DateTimeMath.packedYear(date), DateTimeMath.packedMonth(date), DateTimeMath.packedDay(date), DateTimeMath.hourOfDay(secondOfDay), DateTimeMath.minuteOfHour(secondOfDay), DateTimeMath.secondOfMinute(secondOfDay), nano, TimezoneOffset.UTC, fractionDigits, parseLength);
    }

    private static void fill(final MutableDateTimeBuffer out, final long secondsSince0000, final int nano, final Field field, final int fractionDigits, final int parseLength)
    {
        final long days = secondsSince0000 / 86_400;
        final int date = DateTimeMath.civilFromDaysSince0000((int) days);
        final int secondOfDay = (int) (secondsSince0000 - days * 86_400);
        out.set(field, DateTimeMath.packedYear(date), DateTimeMath.packedMonth(date), DateTimeMath.packedDay(date), DateTimeMath.hourOfDay(secondOfDay), DateTimeMath.minuteOfHour(secondOfDay), DateTimeMath.secondOfMinute(secondOfDay), nano, fractionDigits, 0, parseLength);
    }

    private static void sanityCheckInputParams(final char[] chars, final int offset, final int length, final MutableDateTimeBuffer out)
    {
        if (chars == null)
        {
            throw new NullPointerException("chars cannot be null");
        }
        if (out == null)
        {
            throw new NullPointerException("buffer cannot be null");
        }
        if (offset < 0)
        {
            throw new IndexOutOfBoundsException(String.format("offset cannot be negative, was %d", offset));
        }
        if (length < 0)
        {
            throw new IndexOutOfBoundsException(String.format("length cannot be negative, was %d", length));
        }
        if (offset + length > chars.length)
        {
            throw new IndexOutOfBoundsException(String.format("offset %d plus length %d exceeds the array length of %d", offset, length, chars.length));
        }
    }
}
