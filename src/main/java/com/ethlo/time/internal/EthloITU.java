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

import static com.ethlo.time.internal.ErrorUtil.raiseMissingGranularity;
import static com.ethlo.time.internal.ErrorUtil.raiseUnexpectedCharacter;
import static com.ethlo.time.internal.ErrorUtil.raiseUnexpectedEndOfText;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.DIGIT_9;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.ZERO;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.parsePositiveInt;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.ethlo.time.DateTime;
import com.ethlo.time.DateTimeFormatException;
import com.ethlo.time.Field;
import com.ethlo.time.ParseConfig;
import com.ethlo.time.TimezoneOffset;

public class EthloITU
{
    public static final char DATE_SEPARATOR = '-';
    public static final char TIME_SEPARATOR = ':';
    public static final char SEPARATOR_UPPER = 'T';
    public static final char SEPARATOR_LOWER = 't';
    public static final char SEPARATOR_SPACE = ' ';
    private static final char PLUS = '+';
    private static final char MINUS = '-';
    public static final char FRACTION_SEPARATOR = '.';
    private static final char ZULU_UPPER = 'Z';
    private static final char ZULU_LOWER = 'z';
    public static final int MAX_FRACTION_DIGITS = 9;
    private static final int[] widths = new int[]{100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10, 1};

    private EthloITU()
    {

    }

    public static String finish(final char[] buf, final int length, final TimezoneOffset tz)
    {
        int tzLen = 0;
        if (tz != null)
        {
            tzLen = writeTz(buf, length, tz);
        }
        return new String(buf, 0, length + tzLen);
    }

    private static int writeTz(final char[] buf, final int start, final TimezoneOffset tz)
    {
        if (tz.equals(TimezoneOffset.UTC))
        {
            buf[start] = ZULU_UPPER;
            return 1;
        }
        else
        {
            buf[start] = tz.getTotalSeconds() < 0 ? MINUS : PLUS;
            LimitedCharArrayIntegerUtil.toString(Math.abs(tz.getHours()), buf, start + 1, 2);
            buf[start + 3] = TIME_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(Math.abs(tz.getMinutes()), buf, start + 4, 2);
            return 6;
        }
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

    private static void assertPositionContains(Field field, String chars, int offset, char expected)
    {
        if (offset >= chars.length())
        {
            raiseMissingGranularity(field, chars, offset);
        }

        if (chars.charAt(offset) != expected)
        {
            throw new DateTimeParseException("Expected character " + expected + " at position " + (offset + 1) + ": " + chars, chars, offset);
        }
    }

    private static void assertAllowedDateTimeSeparator(final int offset, final String chars, final ParseConfig config)
    {
        final char needle = chars.charAt(offset + 10);
        if (!config.isDateTimeSeparator(needle))
        {
            throw new DateTimeParseException("Expected character " + Arrays.toString(config.getDateTimeSeparators()) + " at position " + (offset + 10 + 1) + ": " + chars, chars, offset + 10);
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
            throw new DateTimeParseException("Invalid timezone offset: " + chars, chars, idx);
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
                throw new DateTimeParseException("Trailing junk data after position " + (lastUsed + 2) + ": " + chars, chars, lastUsed + 1);
            }
        }
    }

    public static DateTime parseLenient(final String chars, final ParseConfig parseConfig, int offset)
    {
        if (chars == null)
        {
            throw new NullPointerException("text cannot be null");
        }

        final int len = chars.length() - offset;

        // Date portion

        // YEAR
        final int years = parsePositiveInt(chars, offset, offset + 4);
        if (4 == len)
        {
            return DateTime.ofYear(years);
        }

        // MONTH
        assertPositionContains(Field.MONTH, chars, offset + 4, DATE_SEPARATOR);
        final int months = parsePositiveInt(chars, offset + 5, offset + 7);
        if (7 == len)
        {
            return DateTime.ofYearMonth(years, months);
        }

        // DAY
        assertPositionContains(Field.DAY, chars, offset + 7, DATE_SEPARATOR);
        final int days = parsePositiveInt(chars, offset + 8, offset + 10);
        if (10 == len)
        {
            return DateTime.ofDate(years, months, days);
        }

        // HOURS
        assertAllowedDateTimeSeparator(offset, chars, parseConfig);
        final int hours = parsePositiveInt(chars, offset + 11, offset + 13);

        // MINUTES
        assertPositionContains(Field.MINUTE, chars, offset + 13, TIME_SEPARATOR);
        final int minutes = parsePositiveInt(chars, offset + 14, offset + 16);
        if (len == 16)
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
        // We have fractional seconds
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

    private static void assertFractionDigits(String chars, int fractionDigits, int idx)
    {
        if (fractionDigits == 0)
        {
            throw new DateTimeParseException("Must have at least 1 fraction digit: " + chars, chars, idx);
        }

        if (fractionDigits > MAX_FRACTION_DIGITS)
        {
            throw new DateTimeParseException("Maximum supported number of fraction digits in second is 9, got " + fractionDigits + ": " + chars, chars, idx);
        }
    }

    public static String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        return doFormat(date, ZoneOffset.UTC, Field.SECOND, fractionDigits);
    }

    public static String formatUtc(OffsetDateTime date, Field lastIncluded)
    {
        return doFormat(date, ZoneOffset.UTC, lastIncluded, 0);
    }

    public static String format(OffsetDateTime date, ZoneOffset adjustTo, final int fractionDigits)
    {
        return doFormat(date, adjustTo, Field.NANO, fractionDigits);
    }

    private static String doFormat(OffsetDateTime date, ZoneOffset adjustTo, Field lastIncluded, int fractionDigits)
    {
        if (fractionDigits > MAX_FRACTION_DIGITS)
        {
            throw new DateTimeFormatException("Maximum supported number of fraction digits in second is 9, got " + fractionDigits);
        }

        OffsetDateTime adjusted = date;
        if (!date.getOffset().equals(adjustTo))
        {
            adjusted = date.atZoneSameInstant(adjustTo).toOffsetDateTime();
        }
        final TimezoneOffset tz = TimezoneOffset.of(adjustTo);

        final char[] buffer = new char[26 + fractionDigits];

        if (handleDatePart(lastIncluded, buffer, adjusted.getYear(), 0, 4, Field.YEAR))
        {
            return finish(buffer, Field.YEAR.getRequiredLength(), null);
        }

        buffer[4] = DATE_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, adjusted.getMonthValue(), 5, 2, Field.MONTH))
        {
            return finish(buffer, Field.MONTH.getRequiredLength(), null);
        }

        buffer[7] = DATE_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, adjusted.getDayOfMonth(), 8, 2, Field.DAY))
        {
            return finish(buffer, Field.DAY.getRequiredLength(), null);
        }

        // T separator
        buffer[10] = SEPARATOR_UPPER;

        // Time
        LimitedCharArrayIntegerUtil.toString(adjusted.getHour(), buffer, 11, 2);
        buffer[13] = TIME_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, adjusted.getMinute(), 14, 2, Field.MINUTE))
        {
            return finish(buffer, Field.MINUTE.getRequiredLength(), tz);
        }
        buffer[16] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.toString(adjusted.getSecond(), buffer, 17, 2);

        // Second fractions
        final boolean hasFractionDigits = fractionDigits > 0;
        if (hasFractionDigits)
        {
            buffer[19] = FRACTION_SEPARATOR;
            addFractions(buffer, fractionDigits, adjusted.getNano());
            return finish(buffer, 20 + fractionDigits, tz);
        }
        return finish(buffer, 19, tz);
    }

    private static boolean handleDatePart(final Field lastIncluded, final char[] buffer, final int value, final int offset, final int length, final Field field)
    {
        LimitedCharArrayIntegerUtil.toString(value, buffer, offset, length);
        return lastIncluded == field;
    }

    private static void addFractions(char[] buf, int fractionDigits, int nano)
    {
        final double d = widths[fractionDigits - 1];
        LimitedCharArrayIntegerUtil.toString((int) (nano / d), buf, 20, fractionDigits);
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
        throw new DateTimeParseException("Unexpected end of input, missing field " + nextGranularity + ": " + chars, chars, field.getRequiredLength());
    }
}