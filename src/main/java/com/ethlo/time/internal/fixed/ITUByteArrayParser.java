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

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.ethlo.time.Field;
import com.ethlo.time.MutableDateTimeBuffer;
import com.ethlo.time.ParseConfig;
import com.ethlo.time.TimezoneOffset;
import com.ethlo.time.internal.util.ArrayUtils;
import com.ethlo.time.internal.util.DateTimeValidator;

/**
 * The {@code byte[]} mirror of {@link ITUCharArrayParser}, line for line: the bytes are read as ISO-8859-1, so
 * every byte is one character and a non-ASCII byte is reported as the character it maps to. Each digit read is
 * masked ({@code & 0xFF}) before the {@code ^ '0'} check, because a byte is signed and a negative value would
 * otherwise pass {@code <= 9}. The corpus differential runs every entry through all three parsers. A widening copy
 * into a scratch {@code char[]} was measured first and cost 4-8 ns of a 9-15 ns parse (perf-log S11.0).
 * <p>
 * The zero-allocation counterpart of {@link ITUParser#parseLenient(String, ParseConfig, int)}: the same algorithm
 * over a window {@code [offset, offset + length)} of a {@code byte[]}, writing into a {@link MutableDateTimeBuffer}.
 * <p>
 * Contract: for any window, the result (or the exception, its message and {@code getErrorIndex()}) is exactly what
 * {@code ITUParser.parseLenient(new String(bytes, offset, length), config, 0)} produces. Indices are therefore
 * relative to the window, and the trailing-junk rule applies to the window regardless of {@code offset} - unlike the
 * String path, which skips that check when parsing from a non-zero offset. Nothing outside the window is read.
 * <p>
 * Values are parsed into locals and stored in one block at the end, which is what makes "the buffer is untouched on
 * failure" free. Any change here must be mirrored in the String parser, or the differential tests will say so.
 */
public final class ITUByteArrayParser
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

    private ITUByteArrayParser()
    {
    }

    public static int parseLenient(final byte[] bytes, final int offset, final int length, final ParseConfig parseConfig, final MutableDateTimeBuffer out)
    {
        sanityCheckInputParams(bytes, offset, length, out);
        final int end = offset + length;
        if (length < FIXED_PREFIX_LENGTH)
        {
            return parseShort(bytes, offset, end, parseConfig, out);
        }

        // NOTE: The whole fixed-width prefix YYYY-MM-DDTHH:MM:SS is present, so nothing below needs to ask whether the
        // window is long enough: only the characters themselves can be wrong. Every error raised here is raised by the
        // same helper the short path uses, so the messages and indices are identical (the differential tests hold this)
        // NOTE: Positions are passed as (base, constant) pairs, never as a precomputed offset + k, see parse2In
        final int year = parse4In(bytes, offset, 0, offset, end);
        assertCharAt(bytes, offset, end, offset, 4, DATE_SEPARATOR);
        final int month = parse2In(bytes, offset, 5, offset, end);
        assertCharAt(bytes, offset, end, offset, 7, DATE_SEPARATOR);
        final int day = parse2In(bytes, offset, 8, offset, end);
        assertAllowedDateTimeSeparator(bytes, offset, end, parseConfig);
        final int hour = parse2In(bytes, offset, 11, offset, end);
        assertCharAt(bytes, offset, end, offset, 13, TIME_SEPARATOR);
        final int minute = parse2In(bytes, offset, 14, offset, end);
        if (bytes[offset + 16] != TIME_SEPARATOR)
        {
            // Minute resolution with a timezone, or an error: handleTime raises the same exception as always
            return handleTime(bytes, offset, end, parseConfig, out, year, month, day, hour, minute);
        }
        if (length == FIXED_PREFIX_LENGTH)
        {
            final int second = parse2In(bytes, offset, 17, offset, end);
            return finish(out, Field.SECOND, year, month, day, hour, minute, second, 0, 0, NO_OFFSET, length);
        }
        return handleTimeResolution(bytes, offset, end, parseConfig, out, year, month, day, hour, minute);
    }

    /**
     * Inputs shorter than the fixed-width prefix: date-only granularities, minute resolution, and every truncation
     * error. Checks the window length before each field, exactly as the String path does.
     */
    private static int parseShort(final byte[] bytes, final int offset, final int end, final ParseConfig parseConfig, final MutableDateTimeBuffer out)
    {
        final int length = end - offset;

        // YEAR
        final int year = parse4(bytes, offset, offset, end);
        if (4 == length)
        {
            return finish(out, Field.YEAR, year, 0, 0, 0, 0, 0, 0, 0, NO_OFFSET, length);
        }

        // MONTH
        assertPositionContains(Field.MONTH, bytes, offset, end, offset + 4, DATE_SEPARATOR);
        final int month = parse2(bytes, offset + 5, offset, end);
        if (7 == length)
        {
            return finish(out, Field.MONTH, year, month, 0, 0, 0, 0, 0, 0, NO_OFFSET, length);
        }

        // DAY
        assertPositionContains(Field.DAY, bytes, offset, end, offset + 7, DATE_SEPARATOR);
        final int day = parse2(bytes, offset + 8, offset, end);
        if (10 == length)
        {
            return finish(out, Field.DAY, year, month, day, 0, 0, 0, 0, 0, NO_OFFSET, length);
        }

        // HOURS
        assertAllowedDateTimeSeparator(bytes, offset, end, parseConfig);
        final int hour = parse2(bytes, offset + 11, offset, end);

        // MINUTES
        assertPositionContains(Field.MINUTE, bytes, offset, end, offset + 13, TIME_SEPARATOR);
        final int minute = parse2(bytes, offset + 14, offset, end);
        if (16 == length)
        {
            // Have only minutes
            return finish(out, Field.MINUTE, year, month, day, hour, minute, 0, 0, 0, NO_OFFSET, 16);
        }

        // SECONDS or TIMEZONE
        return handleTime(bytes, offset, end, parseConfig, out, year, month, day, hour, minute);
    }

    private static void sanityCheckInputParams(final byte[] bytes, final int offset, final int length, final MutableDateTimeBuffer out)
    {
        if (bytes == null)
        {
            throw new NullPointerException("bytes cannot be null");
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
        if (offset + length > bytes.length)
        {
            throw new IndexOutOfBoundsException(String.format("offset %d plus length %d exceeds the array length of %d", offset, length, bytes.length));
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

    private static int handleTime(final byte[] bytes, final int offset, final int end, final ParseConfig parseConfig, final MutableDateTimeBuffer out, final int year, final int month, final int day, final int hour, final int minute)
    {
        switch (bytes[offset + 16])
        {
            case TIME_SEPARATOR:
                // We have seconds
                return handleTimeResolution(bytes, offset, end, parseConfig, out, year, month, day, hour, minute);

            // We look for time-zone information
            case PLUS:
            case MINUS:
            case ZULU_UPPER:
            case ZULU_LOWER:
                final int zoneOffset = parseTimezone(bytes, offset, end, parseConfig, offset + 16);
                final int charLength = 16 + timezoneLength(bytes, offset + 16, end);
                return finish(out, Field.MINUTE, year, month, day, hour, minute, 0, 0, 0, zoneOffset, charLength);

            default:
                throw raiseUnexpectedCharacter(text(bytes, offset, end), 16, TIME_SEPARATOR, ZULU_UPPER, ZULU_LOWER, PLUS, MINUS);
        }
    }

    private static int handleTimeResolution(final byte[] bytes, final int offset, final int end, final ParseConfig parseConfig, final MutableDateTimeBuffer out, final int year, final int month, final int day, final int hour, final int minute)
    {
        final int length = end - offset;
        if (length > 19)
        {
            final char c = (char) (bytes[offset + 19] & 0xFF);
            if (parseConfig.isFractionSeparator(c))
            {
                return handleFractionalSeconds(bytes, offset, end, parseConfig, out, year, month, day, hour, minute);
            }
            else if (c == ZULU_UPPER || c == ZULU_LOWER)
            {
                assertNoMoreChars(bytes, offset, end, parseConfig, offset, 19);
                return handleSecondResolution(bytes, offset, end, out, year, month, day, hour, minute, 0, ZULU_LENGTH);
            }
            else if (c == PLUS || c == MINUS)
            {
                final int timezoneOffset = parseTimezone(bytes, offset, end, parseConfig, offset + 19);
                return handleSecondResolution(bytes, offset, end, out, year, month, day, hour, minute, timezoneOffset, OFFSET_LENGTH);
            }
            else
            {
                throw raiseUnexpectedCharacter(text(bytes, offset, end), 19, ArrayUtils.merge(parseConfig.getFractionSeparators(), new char[]{ZULU_UPPER, ZULU_LOWER, PLUS, MINUS}));
            }
        }

        // NOTE: length == 19 never gets here: parseLenient handles it before calling, and parseShort has length < 19
        throw raiseUnexpectedEndOfText(text(bytes, offset, end), 16);
    }

    private static int handleSecondResolution(final byte[] bytes, final int offset, final int end, final MutableDateTimeBuffer out, final int year, final int month, final int day, final int hour, final int minute, final int timezoneOffset, final int timezoneLength)
    {
        final int second = parse2In(bytes, offset, 17, offset, end);
        return finish(out, Field.SECOND, year, month, day, hour, minute, second, 0, 0, timezoneOffset, 19 + timezoneLength);
    }

    private static int handleFractionalSeconds(final byte[] bytes, final int offset, final int end, final ParseConfig parseConfig, final MutableDateTimeBuffer out, final int year, final int month, final int day, final int hour, final int minute)
    {
        int fractionDigits = 0;
        int nanos = 0;

        // The common 3/6/9 digit cases as nested straight-line blocks rather than a loop: C2 unrolled the loop
        // itself, but with loop predication checks and a stack spill per digit (perf-log S4.5). Each block takes
        // three digits or nothing; whatever is left goes to the one-at-a-time loop below, as before.
        // The blocks index from offset with constants and only fractionDigits (a constant on each path) changes:
        // an idx local advanced per block is a value C2 must keep materialised for the traps (perf-log S6.6)
        if (offset + 23 <= end)
        {
            final int millis = digits3(bytes, offset + 20);
            if (millis >= 0)
            {
                nanos = millis;
                fractionDigits = 3;
                if (offset + 26 <= end)
                {
                    final int micros = digits3(bytes, offset + 23);
                    if (micros >= 0)
                    {
                        nanos = nanos * 1000 + micros;
                        fractionDigits = 6;
                        if (offset + 29 <= end)
                        {
                            final int nanosPart = digits3(bytes, offset + 26);
                            if (nanosPart >= 0)
                            {
                                nanos = nanos * 1000 + nanosPart;
                                fractionDigits = 9;
                            }
                        }
                    }
                }
            }
        }
        int idx = offset + 20 + fractionDigits;

        // Remainder: one digit at a time
        while (idx < end)
        {
            final int d = (bytes[idx] & 0xFF) ^ ZERO;
            if (d > 9)
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
            assertFractionDigits(text(bytes, offset, end), fractionDigits, idx - 1 - offset);
        }

        // Scale to nanoseconds
        nanos *= NANO_SCALE[fractionDigits];

        final int timezoneOffset = parseTimezone(bytes, offset, end, parseConfig, idx);
        final int charLength = (idx + timezoneLength(bytes, idx, end)) - offset;
        final int second = parse2In(bytes, offset, 17, offset, end);
        return finish(out, Field.NANO, year, month, day, hour, minute, second, nanos, fractionDigits, timezoneOffset, charLength);
    }

    /**
     * @return The value of the three digits at {@code idx}, or -1 if any of them is not a digit. The caller guarantees
     * {@code idx + 3 <= end}. See {@code LimitedCharArrayIntegerUtil.parse2} for the {@code c ^ '0'} digit test
     */
    private static int digits3(final byte[] bytes, final int idx)
    {
        final int d0 = (bytes[idx] & 0xFF) ^ ZERO;
        final int d1 = (bytes[idx + 1] & 0xFF) ^ ZERO;
        final int d2 = (bytes[idx + 2] & 0xFF) ^ ZERO;
        if (d0 <= 9 && d1 <= 9 && d2 <= 9)
        {
            return d0 * 100 + d1 * 10 + d2;
        }
        return -1;
    }

    /**
     * @return The offset in total seconds, or {@link MutableDateTimeBuffer#NO_OFFSET} if the window ends at {@code idx}
     */
    private static int parseTimezone(final byte[] bytes, final int offset, final int end, final ParseConfig parseConfig, final int idx)
    {
        if (idx >= end)
        {
            return NO_OFFSET;
        }
        final char c = (char) (bytes[idx] & 0xFF);
        if (c == ZULU_UPPER || c == ZULU_LOWER)
        {
            assertNoMoreChars(bytes, offset, end, parseConfig, idx, 0);
            return 0;
        }

        if (c != PLUS && c != MINUS)
        {
            throw raiseUnexpectedCharacter(text(bytes, offset, end), idx - offset, ZULU_UPPER, ZULU_LOWER, PLUS, MINUS);
        }

        final int left = end - idx;
        if (left < OFFSET_LENGTH)
        {
            final String text = text(bytes, offset, end);
            throw new DateTimeParseException(String.format("Invalid timezone offset: %s", text), text, idx - offset);
        }

        // Six characters are present, so the fields need no window checks
        assertCharAt(bytes, offset, end, idx, 3, TIME_SEPARATOR);

        int hours = parse2In(bytes, idx, 1, offset, end);
        int minutes = parse2In(bytes, idx, 4, offset, end);
        if (c == MINUS)
        {
            hours = -hours;
            minutes = -minutes;

            if (hours == 0 && minutes == 0)
            {
                final String text = text(bytes, offset, end);
                throw new DateTimeParseException("Unknown 'Local Offset Convention' date-time not allowed", text, idx - offset);
            }
        }

        assertNoMoreChars(bytes, offset, end, parseConfig, idx, 5);

        // NOTE: Arithmetic fast path; the ranges are those of TimezoneOffset.ofHoursMinutes, which is only called
        // to produce its exception (identical to the String path) when a value is out of range
        final int totalSeconds = hours * 3600 + minutes * 60;
        if (hours < -MAX_OFFSET_HOURS || hours > MAX_OFFSET_HOURS || minutes < -59 || minutes > 59 || totalSeconds < -MAX_OFFSET_SECONDS || totalSeconds > MAX_OFFSET_SECONDS)
        {
            TimezoneOffset.ofHoursMinutes(hours, minutes);
        }
        return totalSeconds;
    }

    private static int timezoneLength(final byte[] bytes, final int idx, final int end)
    {
        if (idx >= end)
        {
            return 0;
        }
        final char c = (char) (bytes[idx] & 0xFF);
        return c == ZULU_UPPER || c == ZULU_LOWER ? ZULU_LENGTH : OFFSET_LENGTH;
    }

    /**
     * The last used position is {@code base + rel}, two arguments for the reason given at {@code parse2In}
     */
    private static void assertNoMoreChars(final byte[] bytes, final int offset, final int end, final ParseConfig parseConfig, final int base, final int rel)
    {
        // NOTE: Unlike the String path this applies to any window, not only when offset == 0: the window is the text
        if (parseConfig.isFailOnTrailingJunk() && end > base + rel + 1)
        {
            final String text = text(bytes, offset, end);
            final int relative = base + rel - offset;
            throw new DateTimeParseException(String.format("Trailing junk data after position %d: %s", relative + 2, text), text, relative + 1);
        }
    }

    private static void assertAllowedDateTimeSeparator(final byte[] bytes, final int offset, final int end, final ParseConfig config)
    {
        final char needle = (char) (bytes[offset + 10] & 0xFF);
        if (!config.isDateTimeSeparator(needle))
        {
            final String text = text(bytes, offset, end);
            final String allowedCharStr = config.getDateTimeSeparators().length > 1 ? Arrays.toString(config.getDateTimeSeparators()) : Character.toString(config.getDateTimeSeparators()[0]);
            throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", allowedCharStr, 11, needle, text), text, 10);
        }
    }

    /**
     * {@link #assertPositionContains} for a position {@code base + rel} the caller knows is inside the window (two
     * arguments for the reason given at {@code parse2In})
     */
    private static void assertCharAt(final byte[] bytes, final int offset, final int end, final int base, final int rel, final char expected)
    {
        if (bytes[base + rel] != expected)
        {
            final String text = text(bytes, offset, end);
            final int relative = base + rel - offset;
            throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", expected, relative + 1, (char) (bytes[base + rel] & 0xFF), text), text, relative);
        }
    }

    private static void assertPositionContains(final Field field, final byte[] bytes, final int offset, final int end, final int index, final char expected)
    {
        if (index >= end)
        {
            throw raiseMissingGranularity(field, text(bytes, offset, end), index - offset);
        }

        if (bytes[index] != expected)
        {
            final String text = text(bytes, offset, end);
            final int relative = index - offset;
            throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", expected, relative + 1, (char) (bytes[index] & 0xFF), text), text, relative);
        }
    }

    /**
     * The window as a String. Only ever called on an error path.
     */
    private static String text(final byte[] bytes, final int offset, final int end)
    {
        return new String(bytes, offset, end - offset, StandardCharsets.ISO_8859_1);
    }
}
