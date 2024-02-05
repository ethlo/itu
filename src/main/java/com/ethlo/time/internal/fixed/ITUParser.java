package com.ethlo.time.internal.fixed;

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

import static com.ethlo.time.internal.util.ErrorUtil.assertFractionDigits;
import static com.ethlo.time.internal.util.ErrorUtil.assertPositionContains;
import static com.ethlo.time.internal.util.ErrorUtil.raiseUnexpectedCharacter;
import static com.ethlo.time.internal.util.ErrorUtil.raiseUnexpectedEndOfText;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.DIGIT_9;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.ZERO;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.parsePositiveInt;

import java.text.ParsePosition;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.ethlo.time.DateTime;
import com.ethlo.time.DateTimeParser;
import com.ethlo.time.Field;
import com.ethlo.time.ParseConfig;
import com.ethlo.time.TimezoneOffset;
import com.ethlo.time.internal.util.ArrayUtils;

public class ITUParser implements DateTimeParser
{
    public static final char DATE_SEPARATOR = '-';
    public static final char TIME_SEPARATOR = ':';
    public static final char SEPARATOR_UPPER = 'T';
    public static final char SEPARATOR_LOWER = 't';
    public static final char SEPARATOR_SPACE = ' ';
    public static final char PLUS = '+';
    public static final char MINUS = '-';
    public static final char FRACTION_SEPARATOR = '.';
    public static final char ZULU_UPPER = 'Z';
    public static final char ZULU_LOWER = 'z';
    public static final int MAX_FRACTION_DIGITS = 9;
    public static final int RADIX = 10;
    public static final int DIGITS_IN_NANO = 9;
    private static final DateTimeParser instance = new ITUParser();

    private ITUParser()
    {

    }

    private static DateTime handleTime(final int offset, final ParseConfig parseConfig, final String chars, final int year, final int month, final int day, final int hour, final int minute)
    {
        switch (chars.charAt(offset + 16))
        {
            case TIME_SEPARATOR:
                // We have seconds
                return handleTimeResolution(offset, parseConfig, year, month, day, hour, minute, chars);

            // We look for time-zone information
            case PLUS:
            case MINUS:
            case ZULU_UPPER:
            case ZULU_LOWER:
                final TimezoneOffset zoneOffset = parseTimezone(offset, parseConfig, chars, offset + 16);
                final int charLength = Field.MINUTE.getRequiredLength() + (zoneOffset != null ? zoneOffset.getRequiredLength() : 0);
                return new DateTime(Field.MINUTE, year, month, day, hour, minute, 0, 0, zoneOffset, 0, charLength);

            default:
                throw raiseUnexpectedCharacter(chars, offset + 16, TIME_SEPARATOR, ZULU_UPPER, ZULU_LOWER, PLUS, MINUS);
        }
    }

    private static void assertAllowedDateTimeSeparator(final int offset, final String chars, final ParseConfig config)
    {
        final int index = offset + 10;
        final char needle = chars.charAt(index);
        if (!config.isDateTimeSeparator(needle))
        {
            final String allowedCharStr = config.getDateTimeSeparators().length > 1 ? Arrays.toString(config.getDateTimeSeparators()) : Character.toString(config.getDateTimeSeparators()[0]);
            throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", allowedCharStr, index + 1, chars.charAt(index), chars), chars, index);
        }
    }

    private static TimezoneOffset parseTimezone(int offset, final ParseConfig parseConfig, final String chars, final int idx)
    {
        if (idx >= chars.length())
        {
            return null;
        }
        final int len = chars.length();
        final int left = len - idx;
        final char c = chars.charAt(idx);
        if (c == ZULU_UPPER || c == ZULU_LOWER)
        {
            assertNoMoreChars(offset, parseConfig, chars, idx);
            return TimezoneOffset.UTC;
        }

        final char sign = chars.charAt(idx);
        if (sign != PLUS && sign != MINUS)
        {
            raiseUnexpectedCharacter(chars, idx, ZULU_UPPER, ZULU_LOWER, PLUS, MINUS);
        }

        if (left < 6)
        {
            throw new DateTimeParseException(String.format("Invalid timezone offset: %s", chars), chars, idx);
        }

        int hours = parsePositiveInt(chars, idx + 1, idx + 3);
        int minutes = parsePositiveInt(chars, idx + 4, idx + 4 + 2);
        if (sign == MINUS)
        {
            hours = -hours;
            minutes = -minutes;

            if (hours == 0 && minutes == 0)
            {
                throw new DateTimeParseException("Unknown 'Local Offset Convention' date-time not allowed", chars, idx);
            }
        }

        assertNoMoreChars(offset, parseConfig, chars, idx + 6);
        return TimezoneOffset.ofHoursMinutes(hours, minutes);
    }

    private static void assertNoMoreChars(final int offset, final ParseConfig parseConfig, final String chars, final int lastUsed)
    {
        if (parseConfig.isFailOnTrailingJunk() && offset == 0)
        {
            if (chars.length() > lastUsed + 1)
            {
                throw new DateTimeParseException(String.format("Trailing junk data after position %d: %s", lastUsed + 2, chars), chars, lastUsed + 1);
            }
        }
    }

    public static DateTime parseLenient(final String chars, final ParseConfig parseConfig, int offset)
    {
        if (chars == null)
        {
            throw new NullPointerException("text cannot be null");
        }

        final int availableLength = chars.length() - offset;

        if (availableLength < 0)
        {
            throw new IndexOutOfBoundsException(String.format("offset is %d which is equal to or larger than the input length of %d", offset, chars.length()));
        }

        if (offset < 0)
        {
            throw new IndexOutOfBoundsException(String.format("offset cannot be negative, was %d", offset));
        }

        // Date portion

        // YEAR
        final int years = parseYears(chars, offset);
        if (4 == availableLength)
        {
            return new DateTime(Field.YEAR, years, 0, 0, 0, 0, 0, 0, null, 0, availableLength);
        }

        // MONTH
        assertPositionContains(Field.MONTH, chars, offset + 4, DATE_SEPARATOR);
        final int month = parseMonth(chars, offset);
        if (7 == availableLength)
        {
            return new DateTime(Field.MONTH, years, month, 0, 0, 0, 0, 0, null, 0, availableLength);
        }

        // DAY
        assertPositionContains(Field.DAY, chars, offset + 7, DATE_SEPARATOR);
        final int days = parseDays(chars, offset);
        if (10 == availableLength)
        {
            return new DateTime(Field.DAY, years, month, days, 0, 0, 0, 0, null, 0, availableLength);
        }

        // HOURS
        assertAllowedDateTimeSeparator(offset, chars, parseConfig);
        final int hours = parseHours(chars, offset);

        // MINUTES
        assertPositionContains(Field.MINUTE, chars, offset + 13, TIME_SEPARATOR);
        final int minutes = parseMinutes(chars, offset);
        if (availableLength == 16)
        {
            // Have only minutes
            return new DateTime(Field.MINUTE, years, month, days, hours, minutes, 0, 0, null, 0, 16);
        }

        // SECONDS or TIMEZONE
        return handleTime(offset, parseConfig, chars, years, month, days, hours, minutes);
    }

    private static int parseSeconds(int offset, String chars)
    {
        return parsePositiveInt(chars, offset + 17, offset + 19);
    }

    private static int parseMinutes(String chars, int offset)
    {
        return parsePositiveInt(chars, offset + 14, offset + 16);
    }

    private static int parseHours(String chars, int offset)
    {
        return parsePositiveInt(chars, offset + 11, offset + 13);
    }

    private static int parseDays(String chars, int offset)
    {
        return parsePositiveInt(chars, offset + 8, offset + 10);
    }

    private static int parseMonth(String chars, int offset)
    {
        return parsePositiveInt(chars, offset + 5, offset + 7);
    }

    private static int parseYears(String chars, int offset)
    {
        return parsePositiveInt(chars, offset, offset + 4);
    }

    private static DateTime handleTimeResolution(final int offset, ParseConfig parseConfig, int year, int month, int day, int hour, int minute, String chars)
    {
        final int length = chars.length() - offset;
        if (length > 19)
        {
            final char c = chars.charAt(offset + 19);
            if (parseConfig.isFractionSeparator(c))
            {
                return handleFractionalSeconds(offset, parseConfig, year, month, day, hour, minute, chars);
            }
            else if (c == ZULU_UPPER || c == ZULU_LOWER)
            {
                final TimezoneOffset timezoneOffset = TimezoneOffset.UTC;
                return handleSecondResolution(offset, year, month, day, hour, minute, chars, timezoneOffset);
            }
            else if (c == PLUS || c == MINUS)
            {
                final TimezoneOffset timezoneOffset = parseTimezone(offset, parseConfig, chars, offset + 19);
                return handleSecondResolution(offset, year, month, day, hour, minute, chars, timezoneOffset);
            }
            else
            {
                throw raiseUnexpectedCharacter(chars, offset + 19, ArrayUtils.merge(parseConfig.getFractionSeparators(), new char[]{ZULU_UPPER, ZULU_LOWER, PLUS, MINUS}));
            }
        }
        else if (length == 19)
        {
            final int seconds = parseSeconds(offset, chars);
            return new DateTime(Field.SECOND, year, month, day, hour, minute, seconds, 0, null, 0, length);
        }

        throw raiseUnexpectedEndOfText(chars, offset + 16);
    }

    private static DateTime handleSecondResolution(int offset, int year, int month, int day, int hour, int minute, String chars, TimezoneOffset timezoneOffset)
    {
        final int seconds = parseSeconds(offset, chars);
        final int charLength = Field.SECOND.getRequiredLength() + (timezoneOffset != null ? timezoneOffset.getRequiredLength() : 0);
        return new DateTime(Field.SECOND, year, month, day, hour, minute, seconds, 0, timezoneOffset, 0, charLength);
    }

    private static DateTime handleFractionalSeconds(int offset, ParseConfig parseConfig, int year, int month, int day, int hour, int minute, String chars)
    {
        int idx = offset + 20;
        int fractionDigits = 0;
        int nanos = 0;
        while (idx < chars.length())
        {
            final char c = chars.charAt(idx);
            if (c < ZERO || c > DIGIT_9)
            {
                break;
            }
            else
            {
                fractionDigits++;
                nanos = nanos * RADIX + (c - ZERO);
                idx++;
            }
        }

        assertFractionDigits(chars, fractionDigits, offset + (idx - 1));

        // Scale to nanoseconds
        int pos = fractionDigits;
        while (pos < DIGITS_IN_NANO)
        {
            nanos *= RADIX;
            pos++;
        }

        final TimezoneOffset timezoneOffset = parseTimezone(offset, parseConfig, chars, idx);
        final int charLength = (idx + (timezoneOffset != null ? timezoneOffset.getRequiredLength() : 0)) - offset;
        final int second = parseSeconds(offset, chars);
        return new DateTime(Field.NANO, year, month, day, hour, minute, second, nanos, timezoneOffset, fractionDigits, charLength);
    }

    public static OffsetDateTime parseDateTime(final String chars, int offset)
    {
        final DateTime dateTime = parseLenient(chars, ParseConfig.DEFAULT, offset);
        if (dateTime.includesGranularity(Field.SECOND))
        {
            return dateTime.toOffsetDatetime();
        }
        final Field field = dateTime.getMostGranularField();
        final Field nextGranularity = Field.values()[field.ordinal() + 1];
        throw new DateTimeParseException(String.format("Unexpected end of input, missing field %s: %s", nextGranularity, chars), chars, field.getRequiredLength());
    }

    public static DateTime parseLenient(String text, ParseConfig parseConfig, ParsePosition position)
    {
        try
        {
            int offset = position.getIndex();
            final DateTime result = ITUParser.parseLenient(text, parseConfig, position.getIndex());
            position.setIndex(offset + result.getParseLength());
            return result;
        }
        catch (DateTimeParseException exc)
        {
            position.setErrorIndex(exc.getErrorIndex());
            position.setIndex(position.getErrorIndex());
            throw exc;
        }
    }

    public static DateTimeParser getInstance()
    {
        return instance;
    }

    @Override
    public DateTime parse(final String text, final ParsePosition parsePosition)
    {
        return parseLenient(text, ParseConfig.DEFAULT, parsePosition);
    }

    @Override
    public DateTime parse(final String text)
    {
        return parseLenient(text, ParseConfig.DEFAULT, 0);
    }
}