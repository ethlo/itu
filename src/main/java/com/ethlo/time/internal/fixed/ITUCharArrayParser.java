package com.ethlo.time.internal.fixed;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2024 Morten Haraldsen (ethlo)
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

import static com.ethlo.time.MutableDateTimeBuffer.NO_OFFSET;
import static com.ethlo.time.internal.fixed.ITUParser.DATE_SEPARATOR;
import static com.ethlo.time.internal.fixed.ITUParser.MAX_FRACTION_DIGITS;
import static com.ethlo.time.internal.fixed.ITUParser.MINUS;
import static com.ethlo.time.internal.fixed.ITUParser.PLUS;
import static com.ethlo.time.internal.fixed.ITUParser.RADIX;
import static com.ethlo.time.internal.fixed.ITUParser.TIME_SEPARATOR;
import static com.ethlo.time.internal.fixed.ITUParser.ZULU_LOWER;
import static com.ethlo.time.internal.fixed.ITUParser.ZULU_UPPER;
import static com.ethlo.time.internal.util.ErrorUtil.assertFractionDigits;
import static com.ethlo.time.internal.util.ErrorUtil.raiseMissingGranularity;
import static com.ethlo.time.internal.util.ErrorUtil.raiseUnexpectedCharacter;
import static com.ethlo.time.internal.util.ErrorUtil.raiseUnexpectedEndOfText;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.ZERO;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.parse2;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.parse2In;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.parse4;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.parse4In;

import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.ethlo.time.Field;
import com.ethlo.time.MutableDateTimeBuffer;
import com.ethlo.time.ParseConfig;
import com.ethlo.time.TimezoneOffset;
import com.ethlo.time.internal.util.ArrayUtils;
import com.ethlo.time.internal.util.DateTimeValidator;

/**
 * The zero-allocation counterpart of {@link ITUParser#parseLenient(String, ParseConfig, int)}: the same algorithm
 * over a window {@code [offset, offset + length)} of a {@code char[]}, writing into a {@link MutableDateTimeBuffer}.
 * <p>
 * Contract: for any window, the result (or the exception, its message and {@code getErrorIndex()}) is exactly what
 * {@code ITUParser.parseLenient(new String(chars, offset, length), config, 0)} produces. Indices are therefore
 * relative to the window, and the trailing-junk rule applies to the window regardless of {@code offset} - unlike the
 * String path, which skips that check when parsing from a non-zero offset. Nothing outside the window is read.
 * <p>
 * Values are parsed into locals and stored in one block at the end, which is what makes "the buffer is untouched on
 * failure" free. Any change here must be mirrored in the String parser, or the differential tests will say so.
 */
public final class ITUCharArrayParser
{
    private static final int[] NANO_SCALE = {1_000_000_000, 100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10, 1};
    /**
     * The length of {@code YYYY-MM-DDTHH:MM:SS}: at or beyond it, every field position of the prefix is inside the window
     */
    private static final int FIXED_PREFIX_LENGTH = 19;
    private static final int ZULU_LENGTH = 1;
    private static final int OFFSET_LENGTH = 6;
    private static final int MAX_OFFSET_HOURS = 18;
    private static final int MAX_OFFSET_SECONDS = MAX_OFFSET_HOURS * 3600;

    private ITUCharArrayParser()
    {
    }

    public static int parseLenient(final char[] chars, final int offset, final int length, final ParseConfig parseConfig, final MutableDateTimeBuffer out)
    {
        sanityCheckInputParams(chars, offset, length, out);
        final int end = offset + length;
        if (length < FIXED_PREFIX_LENGTH)
        {
            return parseShort(chars, offset, end, parseConfig, out);
        }

        // NOTE: The whole fixed-width prefix YYYY-MM-DDTHH:MM:SS is present, so nothing below needs to ask whether the
        // window is long enough: only the characters themselves can be wrong. Every error raised here is raised by the
        // same helper the short path uses, so the messages and indices are identical (the differential tests hold this)
        final int year = parse4In(chars, offset, offset, end);
        assertCharAt(chars, offset, end, offset + 4, DATE_SEPARATOR);
        final int month = parse2In(chars, offset + 5, offset, end);
        assertCharAt(chars, offset, end, offset + 7, DATE_SEPARATOR);
        final int day = parse2In(chars, offset + 8, offset, end);
        assertAllowedDateTimeSeparator(chars, offset, end, parseConfig);
        final int hour = parse2In(chars, offset + 11, offset, end);
        assertCharAt(chars, offset, end, offset + 13, TIME_SEPARATOR);
        final int minute = parse2In(chars, offset + 14, offset, end);
        if (chars[offset + 16] != TIME_SEPARATOR)
        {
            // Minute resolution with a timezone, or an error: handleTime raises the same exception as always
            return handleTime(chars, offset, end, parseConfig, out, year, month, day, hour, minute);
        }
        if (length == FIXED_PREFIX_LENGTH)
        {
            final int second = parse2In(chars, offset + 17, offset, end);
            return finish(out, Field.SECOND, year, month, day, hour, minute, second, 0, 0, NO_OFFSET, length);
        }
        return handleTimeResolution(chars, offset, end, parseConfig, out, year, month, day, hour, minute);
    }

    /**
     * Inputs shorter than the fixed-width prefix: date-only granularities, minute resolution, and every truncation
     * error. Checks the window length before each field, exactly as the String path does.
     */
    private static int parseShort(final char[] chars, final int offset, final int end, final ParseConfig parseConfig, final MutableDateTimeBuffer out)
    {
        final int length = end - offset;

        // YEAR
        final int year = parse4(chars, offset, offset, end);
        if (4 == length)
        {
            return finish(out, Field.YEAR, year, 0, 0, 0, 0, 0, 0, 0, NO_OFFSET, length);
        }

        // MONTH
        assertPositionContains(Field.MONTH, chars, offset, end, offset + 4, DATE_SEPARATOR);
        final int month = parse2(chars, offset + 5, offset, end);
        if (7 == length)
        {
            return finish(out, Field.MONTH, year, month, 0, 0, 0, 0, 0, 0, NO_OFFSET, length);
        }

        // DAY
        assertPositionContains(Field.DAY, chars, offset, end, offset + 7, DATE_SEPARATOR);
        final int day = parse2(chars, offset + 8, offset, end);
        if (10 == length)
        {
            return finish(out, Field.DAY, year, month, day, 0, 0, 0, 0, 0, NO_OFFSET, length);
        }

        // HOURS
        assertAllowedDateTimeSeparator(chars, offset, end, parseConfig);
        final int hour = parse2(chars, offset + 11, offset, end);

        // MINUTES
        assertPositionContains(Field.MINUTE, chars, offset, end, offset + 13, TIME_SEPARATOR);
        final int minute = parse2(chars, offset + 14, offset, end);
        if (16 == length)
        {
            // Have only minutes
            return finish(out, Field.MINUTE, year, month, day, hour, minute, 0, 0, 0, NO_OFFSET, 16);
        }

        // SECONDS or TIMEZONE
        return handleTime(chars, offset, end, parseConfig, out, year, month, day, hour, minute);
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

    /**
     * The validation the {@link com.ethlo.time.DateTime} constructor performs, in the same order, then the single
     * block of stores. Everything above this point has only touched locals.
     */
    private static int finish(final MutableDateTimeBuffer out, final Field field, final int year, final int month, final int day, final int hour, final int minute, final int second, final int nano, final int fractionDigits, final int offsetTotalSeconds, final int parseLength)
    {
        DateTimeValidator.leapSecondCheck(year, month, day, hour, minute, second, nano, offsetTotalSeconds != NO_OFFSET, offsetTotalSeconds);
        DateTimeValidator.validate(field, year, month, day, hour, minute, second, nano);
        out.set(field, year, month, day, hour, minute, second, nano, fractionDigits, offsetTotalSeconds, parseLength);
        return parseLength;
    }

    private static int handleTime(final char[] chars, final int offset, final int end, final ParseConfig parseConfig, final MutableDateTimeBuffer out, final int year, final int month, final int day, final int hour, final int minute)
    {
        switch (chars[offset + 16])
        {
            case TIME_SEPARATOR:
                // We have seconds
                return handleTimeResolution(chars, offset, end, parseConfig, out, year, month, day, hour, minute);

            // We look for time-zone information
            case PLUS:
            case MINUS:
            case ZULU_UPPER:
            case ZULU_LOWER:
                final int zoneOffset = parseTimezone(chars, offset, end, parseConfig, offset + 16);
                final int charLength = 16 + timezoneLength(chars, offset + 16, end);
                return finish(out, Field.MINUTE, year, month, day, hour, minute, 0, 0, 0, zoneOffset, charLength);

            default:
                throw raiseUnexpectedCharacter(text(chars, offset, end), 16, TIME_SEPARATOR, ZULU_UPPER, ZULU_LOWER, PLUS, MINUS);
        }
    }

    private static int handleTimeResolution(final char[] chars, final int offset, final int end, final ParseConfig parseConfig, final MutableDateTimeBuffer out, final int year, final int month, final int day, final int hour, final int minute)
    {
        final int length = end - offset;
        if (length > 19)
        {
            final char c = chars[offset + 19];
            if (parseConfig.isFractionSeparator(c))
            {
                return handleFractionalSeconds(chars, offset, end, parseConfig, out, year, month, day, hour, minute);
            }
            else if (c == ZULU_UPPER || c == ZULU_LOWER)
            {
                assertNoMoreChars(chars, offset, end, parseConfig, offset + 19);
                return handleSecondResolution(chars, offset, end, out, year, month, day, hour, minute, 0, ZULU_LENGTH);
            }
            else if (c == PLUS || c == MINUS)
            {
                final int timezoneOffset = parseTimezone(chars, offset, end, parseConfig, offset + 19);
                return handleSecondResolution(chars, offset, end, out, year, month, day, hour, minute, timezoneOffset, OFFSET_LENGTH);
            }
            else
            {
                throw raiseUnexpectedCharacter(text(chars, offset, end), 19, ArrayUtils.merge(parseConfig.getFractionSeparators(), new char[]{ZULU_UPPER, ZULU_LOWER, PLUS, MINUS}));
            }
        }
        else if (length == 19)
        {
            final int second = parse2(chars, offset + 17, offset, end);
            return finish(out, Field.SECOND, year, month, day, hour, minute, second, 0, 0, NO_OFFSET, length);
        }

        throw raiseUnexpectedEndOfText(text(chars, offset, end), 16);
    }

    private static int handleSecondResolution(final char[] chars, final int offset, final int end, final MutableDateTimeBuffer out, final int year, final int month, final int day, final int hour, final int minute, final int timezoneOffset, final int timezoneLength)
    {
        final int second = parse2(chars, offset + 17, offset, end);
        return finish(out, Field.SECOND, year, month, day, hour, minute, second, 0, 0, timezoneOffset, 19 + timezoneLength);
    }

    private static int handleFractionalSeconds(final char[] chars, final int offset, final int end, final ParseConfig parseConfig, final MutableDateTimeBuffer out, final int year, final int month, final int day, final int hour, final int minute)
    {
        int idx = offset + 20;
        int fractionDigits = 0;
        int nanos = 0;

        // Fast path for the common 3/6/9 digit cases: consume digits three at a time in straight-line code
        while (fractionDigits < MAX_FRACTION_DIGITS && idx + 3 <= end)
        {
            final int d0 = chars[idx] - ZERO;
            final int d1 = chars[idx + 1] - ZERO;
            final int d2 = chars[idx + 2] - ZERO;
            if ((d0 | d1 | d2) < 0 || d0 > 9 || d1 > 9 || d2 > 9)
            {
                break;
            }
            nanos = nanos * 1000 + d0 * 100 + d1 * 10 + d2;
            fractionDigits += 3;
            idx += 3;
        }

        // Remainder: one digit at a time
        while (idx < end)
        {
            final int d = chars[idx] - ZERO;
            if (d < 0 || d > 9)
            {
                break;
            }
            fractionDigits++;
            if (fractionDigits <= MAX_FRACTION_DIGITS)
            {
                // Beyond the maximum the value is rejected below, so avoid overflowing the accumulator
                nanos = nanos * RADIX + d;
            }
            idx++;
        }
        if (fractionDigits == 0 || fractionDigits > MAX_FRACTION_DIGITS)
        {
            assertFractionDigits(text(chars, offset, end), fractionDigits, idx - 1 - offset);
        }

        // Scale to nanoseconds
        nanos *= NANO_SCALE[fractionDigits];

        final int timezoneOffset = parseTimezone(chars, offset, end, parseConfig, idx);
        final int charLength = (idx + timezoneLength(chars, idx, end)) - offset;
        final int second = parse2(chars, offset + 17, offset, end);
        return finish(out, Field.NANO, year, month, day, hour, minute, second, nanos, fractionDigits, timezoneOffset, charLength);
    }

    /**
     * @return The offset in total seconds, or {@link MutableDateTimeBuffer#NO_OFFSET} if the window ends at {@code idx}
     */
    private static int parseTimezone(final char[] chars, final int offset, final int end, final ParseConfig parseConfig, final int idx)
    {
        if (idx >= end)
        {
            return NO_OFFSET;
        }
        final char c = chars[idx];
        if (c == ZULU_UPPER || c == ZULU_LOWER)
        {
            assertNoMoreChars(chars, offset, end, parseConfig, idx);
            return 0;
        }

        if (c != PLUS && c != MINUS)
        {
            throw raiseUnexpectedCharacter(text(chars, offset, end), idx - offset, ZULU_UPPER, ZULU_LOWER, PLUS, MINUS);
        }

        final int left = end - idx;
        if (left < OFFSET_LENGTH)
        {
            final String text = text(chars, offset, end);
            throw new DateTimeParseException(String.format("Invalid timezone offset: %s", text), text, idx - offset);
        }

        assertPositionContains(Field.ZONE_OFFSET, chars, offset, end, idx + 3, TIME_SEPARATOR);

        int hours = parse2(chars, idx + 1, offset, end);
        int minutes = parse2(chars, idx + 4, offset, end);
        if (c == MINUS)
        {
            hours = -hours;
            minutes = -minutes;

            if (hours == 0 && minutes == 0)
            {
                final String text = text(chars, offset, end);
                throw new DateTimeParseException("Unknown 'Local Offset Convention' date-time not allowed", text, idx - offset);
            }
        }

        assertNoMoreChars(chars, offset, end, parseConfig, idx + 5);

        // NOTE: Arithmetic fast path; the ranges are those of TimezoneOffset.ofHoursMinutes, which is only called
        // to produce its exception (identical to the String path) when a value is out of range
        final int totalSeconds = hours * 3600 + minutes * 60;
        if (hours < -MAX_OFFSET_HOURS || hours > MAX_OFFSET_HOURS || minutes < -59 || minutes > 59 || totalSeconds < -MAX_OFFSET_SECONDS || totalSeconds > MAX_OFFSET_SECONDS)
        {
            TimezoneOffset.ofHoursMinutes(hours, minutes);
        }
        return totalSeconds;
    }

    private static int timezoneLength(final char[] chars, final int idx, final int end)
    {
        if (idx >= end)
        {
            return 0;
        }
        final char c = chars[idx];
        return c == ZULU_UPPER || c == ZULU_LOWER ? ZULU_LENGTH : OFFSET_LENGTH;
    }

    private static void assertNoMoreChars(final char[] chars, final int offset, final int end, final ParseConfig parseConfig, final int lastUsed)
    {
        // NOTE: Unlike the String path this applies to any window, not only when offset == 0: the window is the text
        if (parseConfig.isFailOnTrailingJunk() && end > lastUsed + 1)
        {
            final String text = text(chars, offset, end);
            final int relative = lastUsed - offset;
            throw new DateTimeParseException(String.format("Trailing junk data after position %d: %s", relative + 2, text), text, relative + 1);
        }
    }

    private static void assertAllowedDateTimeSeparator(final char[] chars, final int offset, final int end, final ParseConfig config)
    {
        final char needle = chars[offset + 10];
        if (!config.isDateTimeSeparator(needle))
        {
            final String text = text(chars, offset, end);
            final String allowedCharStr = config.getDateTimeSeparators().length > 1 ? Arrays.toString(config.getDateTimeSeparators()) : Character.toString(config.getDateTimeSeparators()[0]);
            throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", allowedCharStr, 11, needle, text), text, 10);
        }
    }

    /**
     * {@link #assertPositionContains} for an index the caller knows is inside the window
     */
    private static void assertCharAt(final char[] chars, final int offset, final int end, final int index, final char expected)
    {
        if (chars[index] != expected)
        {
            final String text = text(chars, offset, end);
            final int relative = index - offset;
            throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", expected, relative + 1, chars[index], text), text, relative);
        }
    }

    private static void assertPositionContains(final Field field, final char[] chars, final int offset, final int end, final int index, final char expected)
    {
        if (index >= end)
        {
            throw raiseMissingGranularity(field, text(chars, offset, end), index - offset);
        }

        if (chars[index] != expected)
        {
            final String text = text(chars, offset, end);
            final int relative = index - offset;
            throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", expected, relative + 1, chars[index], text), text, relative);
        }
    }

    /**
     * The window as a String. Only ever called on an error path.
     */
    private static String text(final char[] chars, final int offset, final int end)
    {
        return new String(chars, offset, end - offset);
    }
}
