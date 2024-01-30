package com.ethlo.time.internal;

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

import static com.ethlo.time.internal.ErrorUtil.assertFractionDigits;
import static com.ethlo.time.internal.ErrorUtil.assertPositionContains;
import static com.ethlo.time.internal.ErrorUtil.raiseUnexpectedCharacter;
import static com.ethlo.time.internal.ErrorUtil.raiseUnexpectedEndOfText;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.DIGIT_9;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.ZERO;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.parsePositiveInt;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.ethlo.time.DateTime;
import com.ethlo.time.Field;
import com.ethlo.time.ParseConfig;
import com.ethlo.time.TimezoneOffset;

public class ITUParser
{
    public static final char DATE_SEPARATOR = '-';
    public static final char TIME_SEPARATOR = ':';
    public static final char SEPARATOR_UPPER = 'T';
    public static final char SEPARATOR_LOWER = 't';
    public static final char SEPARATOR_SPACE = ' ';
    static final char PLUS = '+';
    static final char MINUS = '-';
    public static final char FRACTION_SEPARATOR = '.';
    static final char ZULU_UPPER = 'Z';
    private static final char ZULU_LOWER = 'z';
    public static final int MAX_FRACTION_DIGITS = 9;

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
                throw raiseUnexpectedCharacter(chars, offset + 16);
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
            raiseUnexpectedCharacter(chars, idx);
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
        final int years = parsePositiveInt(chars, offset, offset + 4);
        if (4 == availableLength)
        {
            return DateTime.ofYear(years);
        }

        // MONTH
        assertPositionContains(Field.MONTH, chars, offset + 4, DATE_SEPARATOR);
        final int months = parsePositiveInt(chars, offset + 5, offset + 7);
        if (7 == availableLength)
        {
            return DateTime.ofYearMonth(years, months);
        }

        // DAY
        assertPositionContains(Field.DAY, chars, offset + 7, DATE_SEPARATOR);
        final int days = parsePositiveInt(chars, offset + 8, offset + 10);
        if (10 == availableLength)
        {
            return DateTime.ofDate(years, months, days);
        }

        // HOURS
        assertAllowedDateTimeSeparator(offset, chars, parseConfig);
        final int hours = parsePositiveInt(chars, offset + 11, offset + 13);

        // MINUTES
        assertPositionContains(Field.MINUTE, chars, offset + 13, TIME_SEPARATOR);
        final int minutes = parsePositiveInt(chars, offset + 14, offset + 16);
        if (availableLength == 16)
        {
            // Have only minutes
            return DateTime.of(years, months, days, hours, minutes, null);
        }

        // SECONDS or TIMEZONE
        return handleTime(offset, parseConfig, chars, years, months, days, hours, minutes);
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
                throw raiseUnexpectedCharacter(chars, offset + 19);
            }
        }
        else if (length == offset + 19)
        {
            final int seconds = parsePositiveInt(chars, offset + 17, offset + 19);
            return DateTime.of(year, month, day, hour, minute, seconds, null);
        }

        throw raiseUnexpectedEndOfText(chars, offset + 16);
    }

    private static DateTime handleSecondResolution(int offset, int year, int month, int day, int hour, int minute, String chars, TimezoneOffset timezoneOffset)
    {
        final int seconds = parsePositiveInt(chars, offset + 17, offset + 19);
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
                nanos = nanos * 10 + (c - ZERO);
                idx++;
            }
        }

        assertFractionDigits(chars, fractionDigits, offset + (idx - 1));

        // Scale to nanos
        int pos = fractionDigits;
        while (pos < 9)
        {
            nanos *= 10;
            pos++;
        }

        final TimezoneOffset timezoneOffset = parseTimezone(offset, parseConfig, chars, idx);
        final int charLength = (idx + (timezoneOffset != null ? timezoneOffset.getRequiredLength() : 0)) - offset;
        final int second = parsePositiveInt(chars, offset + 17, offset + 19);
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
}