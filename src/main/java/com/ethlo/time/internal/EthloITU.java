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
import static com.ethlo.time.internal.ErrorUtil.raiseMissingTimeZone;
import static com.ethlo.time.internal.ErrorUtil.raiseUnexpectedCharacter;
import static com.ethlo.time.internal.ErrorUtil.raiseUnexpectedEndOfText;
import static com.ethlo.time.internal.LeapSecondHandler.LEAP_SECOND_SECONDS;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.DIGIT_9;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.ZERO;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.parsePositiveInt;

import java.time.Month;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.ethlo.time.DateTime;
import com.ethlo.time.Field;
import com.ethlo.time.LeapSecondException;
import com.ethlo.time.ParseConfig;
import com.ethlo.time.TimezoneOffset;

public class EthloITU extends AbstractRfc3339 implements W3cDateTimeUtil
{
    private static final EthloITU instance = new EthloITU();

    public static final char DATE_SEPARATOR = '-';
    public static final char TIME_SEPARATOR = ':';
    public static final char SEPARATOR_UPPER = 'T';
    private static final char PLUS = '+';
    private static final char MINUS = '-';
    private static final char FRACTION_SEPARATOR = '.';
    private static final char ZULU_UPPER = 'Z';
    private static final char ZULU_LOWER = 'z';
    private static final int[] widths = new int[]{100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10, 1};
    private static final LeapSecondHandler leapSecondHandler = new DefaultLeapSecondHandler();

    private EthloITU()
    {

    }

    public static EthloITU getInstance()
    {
        return instance;
    }

    public static String finish(char[] buf, int length, final TimezoneOffset tz)
    {
        int tzLen = 0;
        if (tz != null)
        {
            tzLen = writeTz(buf, length, tz);
        }
        return new String(buf, 0, length + tzLen);
    }

    private static int writeTz(char[] buf, int start, TimezoneOffset tz)
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

    private static int scale(int fractions, int len, String parsedData, final int index)
    {
        switch (len)
        {
            case 0:
                throw new DateTimeParseException("Must have at least 1 fraction digit: " + parsedData, parsedData, index);
            case 1:
                return fractions * 100_000_000;
            case 2:
                return fractions * 10_000_000;
            case 3:
                return fractions * 1_000_000;
            case 4:
                return fractions * 100_000;
            case 5:
                return fractions * 10_000;
            case 6:
                return fractions * 1_000;
            case 7:
                return fractions * 100;
            case 8:
                return fractions * 10;
            default:
                return fractions;
        }
    }

    private static Object handleTime(ParseConfig parseConfig, String chars, int year, int month, int day, int hour, int minute, final boolean raw)
    {
        switch (chars.charAt(16))
        {
            // We have more granularity, keep going
            case TIME_SEPARATOR:
                return handleTimeResolution(parseConfig, year, month, day, hour, minute, chars, raw);

            case PLUS:
            case MINUS:
            case ZULU_UPPER:
            case ZULU_LOWER:
                final TimezoneOffset zoneOffset = parseTimezone(chars, 16);
                if (!raw)
                {
                    raiseMissingGranularity(Field.SECOND, chars, 16);
                }
                return DateTime.of(year, month, day, hour, minute, zoneOffset);
        }
        throw raiseUnexpectedCharacter(chars, 16);
    }

    private static void assertPositionContains(String chars, int offset, char expected)
    {
        if (offset >= chars.length())
        {
            raiseMissingGranularity(Field.MINUTE, chars, offset);
        }

        if (chars.charAt(offset) != expected)
        {
            throw new DateTimeParseException("Expected character " + expected
                    + " at position " + (offset + 1) + ": " + chars, chars, offset);
        }
    }

    private static void assertAllowedDateTimeSeparator(String chars, final ParseConfig config)
    {
        final char needle = chars.charAt(10);
        if (!config.isDateTimeSeparator(needle))
        {
            throw new DateTimeParseException("Expected character " + Arrays.toString(config.getDateTimeSeparators())
                    + " at position " + (10 + 1) + ": " + chars, chars, 10);
        }
    }

    private static TimezoneOffset parseTimezone(String chars, int offset)
    {
        if (offset >= chars.length())
        {
            raiseMissingTimeZone(chars, offset);
        }
        final int len = chars.length();
        final int left = len - offset;
        final char c = chars.charAt(offset);
        if (c == ZULU_UPPER || c == ZULU_LOWER)
        {
            assertNoMoreChars(chars, offset);
            return TimezoneOffset.UTC;
        }

        final char sign = chars.charAt(offset);
        if (sign != PLUS && sign != MINUS)
        {
            raiseUnexpectedCharacter(chars, offset);
        }

        if (left != 6)
        {
            throw new DateTimeParseException("Invalid timezone offset: " + chars, chars, offset);
        }

        int hours = parsePositiveInt(chars, offset + 1, offset + 3);
        int minutes = parsePositiveInt(chars, offset + 4, offset + 4 + 2);
        if (sign == MINUS)
        {
            hours = -hours;
            minutes = -minutes;
        }

        if (sign == MINUS && hours == 0 && minutes == 0)
        {
            throw new DateTimeParseException("Unknown 'Local Offset Convention' date-time not allowed", chars, offset);
        }

        return TimezoneOffset.ofHoursMinutes(hours, minutes);
    }

    private static void assertNoMoreChars(String chars, int lastUsed)
    {
        if (chars.length() > lastUsed + 1)
        {
            throw new DateTimeParseException("Trailing junk data after position " + (lastUsed + 2) + ": " + chars, chars, lastUsed + 1);
        }
    }

    private static Object parse(String chars, ParseConfig parseConfig, boolean raw)
    {
        if (chars == null)
        {
            throw new NullPointerException("text cannot be null");
        }

        final int len = chars.length();

        // Date portion

        // YEAR
        final int years = parsePositiveInt(chars, 0, 4);
        if (4 == len)
        {
            if (!raw)
            {
                throw raiseMissingGranularity(Field.MONTH, chars, 4);
            }
            return DateTime.ofYear(years);
        }

        // MONTH
        assertPositionContains(chars, 4, DATE_SEPARATOR);
        final int months = parsePositiveInt(chars, 5, 7);
        if (7 == len)
        {
            if (!raw)
            {
                throw raiseMissingGranularity(Field.DAY, chars, 7);
            }
            return DateTime.ofYearMonth(years, months);
        }

        // DAY
        assertPositionContains(chars, 7, DATE_SEPARATOR);
        final int days = parsePositiveInt(chars, 8, 10);
        if (10 == len)
        {
            if (!raw)
            {
                throw raiseMissingGranularity(Field.HOUR, chars, 11);
            }
            return DateTime.ofDate(years, months, days);
        }

        // HOURS
        assertAllowedDateTimeSeparator(chars, parseConfig);
        final int hours = parsePositiveInt(chars, 11, 13);

        // MINUTES
        assertPositionContains(chars, 13, TIME_SEPARATOR);
        final int minutes = parsePositiveInt(chars, 14, 16);
        if (len > 16)
        {
            // SECONDS or TIMEZONE
            return handleTime(parseConfig, chars, years, months, days, hours, minutes, raw);
        }

        // Have only minutes
        if (raw)
        {
            return DateTime.of(years, months, days, hours, minutes, null);
        }
        throw raiseMissingGranularity(Field.SECOND, chars, 16);
    }

    private static Object handleTimeResolution(ParseConfig config, int year, int month, int day, int hour, int minute, String chars, boolean raw)
    {
        // From here the specification is more lenient
        final int length = chars.length();
        if (length > 19)
        {
            TimezoneOffset offset = null;
            int fractions = 0;
            int fractionDigits = 0;
            char c = chars.charAt(19);
            if (config.isFractionSeparator(c))
            {
                final int firstFraction = 20;
                if (chars.length() < 21)
                {
                    raiseUnexpectedEndOfText(chars, firstFraction);
                }

                // We have fractional seconds
                int result = 0;
                int idx = firstFraction;
                boolean nonDigitFound = false;
                do
                {
                    c = chars.charAt(idx);
                    if (c < ZERO || c > DIGIT_9)
                    {
                        nonDigitFound = true;
                        fractionDigits = idx - firstFraction;
                        assertFractionDigits(chars, fractionDigits, idx);
                        fractions = scale(-result, fractionDigits, chars, idx);
                        offset = parseTimezone(chars, idx);
                    }
                    else
                    {
                        fractionDigits = idx - 19;
                        assertFractionDigits(chars, fractionDigits, idx);
                        result = (result << 1) + (result << 3);
                        result -= c - ZERO;
                    }
                    idx++;
                } while (idx < length && !nonDigitFound);

                if (!nonDigitFound)
                {
                    fractionDigits = idx - 20;
                    fractions = scale(-result, fractionDigits, chars, idx);
                    if (!raw)
                    {
                        offset = parseTimezone(chars, idx);
                    }
                }
            }
            else if (c == ZULU_UPPER || c == ZULU_LOWER)
            {
                // Do nothing we are done
                offset = TimezoneOffset.UTC;
            }
            else if (c == PLUS || c == MINUS)
            {
                // No fractional seconds
                offset = parseTimezone(chars, 19);
            }
            else
            {
                throw raiseUnexpectedCharacter(chars, 19);
            }

            final int second = parsePositiveInt(chars, 17, 19);

            if (!raw)
            {
                leapSecondCheck(year, month, day, hour, minute, second, fractions, offset);
                return OffsetDateTime.of(year, month, day, hour, minute, second, fractions, offset.toZoneOffset());
            }
            return fractionDigits > 0 ? DateTime.of(year, month, day, hour, minute, second, fractions, offset, fractionDigits) : DateTime.of(year, month, day, hour, minute, second, offset);
        }
        else if (length == 19)
        {
            final int seconds = parsePositiveInt(chars, 17, 19);
            if (raw)
            {
                return new DateTime(Field.SECOND, year, month, day, hour, minute, seconds, 0, null, 0);
            }
            raiseMissingTimeZone(chars, 19);
        }

        throw raiseUnexpectedEndOfText(chars, 16);
    }

    private static void assertFractionDigits(String chars, int fractionDigits, int idx)
    {
        if (fractionDigits > MAX_FRACTION_DIGITS)
        {
            throw new DateTimeParseException("Too many fraction digits: " + chars, chars, idx);
        }
    }

    private static void leapSecondCheck(int year, int month, int day, int hour, int minute, int second, int nanos, TimezoneOffset offset)
    {
        if (second == LEAP_SECOND_SECONDS)
        {
            // Do not fall over trying to parse leap seconds
            final YearMonth needle = YearMonth.of(year, month);
            final boolean isValidLeapYearMonth = leapSecondHandler.isValidLeapSecondDate(needle);
            if (isValidLeapYearMonth || needle.isAfter(leapSecondHandler.getLastKnownLeapSecond()))
            {
                final int utcHour = hour - offset.getTotalSeconds() / 3_600;
                final int utcMinute = minute - (offset.getTotalSeconds() % 3_600) / 60;
                if (((month == Month.DECEMBER.getValue() && day == 31) || (month == Month.JUNE.getValue() && day == 30))
                        && utcHour == 23
                        && utcMinute == 59)
                {
                    // Consider it a leap second
                    final OffsetDateTime nearest = OffsetDateTime.of(year, month, day, hour, minute, 59, nanos, offset.toZoneOffset()).plusSeconds(1);
                    throw new LeapSecondException(nearest, second, isValidLeapYearMonth);
                }
            }
        }
    }

    @Override
    public String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        return doFormat(date, ZoneOffset.UTC, Field.SECOND, fractionDigits);
    }

    @Override
    public String formatUtc(OffsetDateTime date, Field lastIncluded)
    {
        return doFormat(date, ZoneOffset.UTC, lastIncluded, 0);
    }

    @Override
    public String format(OffsetDateTime date, ZoneOffset adjustTo, final int fractionDigits)
    {
        return doFormat(date, adjustTo, Field.NANO, fractionDigits);
    }

    private String doFormat(OffsetDateTime date, ZoneOffset adjustTo, Field lastIncluded, int fractionDigits)
    {
        assertMaxFractionDigits(fractionDigits);

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

    private boolean handleDatePart(final Field lastIncluded, final char[] buffer, final int value, final int offset, final int length, final Field field)
    {
        LimitedCharArrayIntegerUtil.toString(value, buffer, offset, length);
        return lastIncluded == field;
    }

    private void addFractions(char[] buf, int fractionDigits, int nano)
    {
        final double d = widths[fractionDigits - 1];
        LimitedCharArrayIntegerUtil.toString((int) (nano / d), buf, 20, fractionDigits);
    }

    @Override
    public OffsetDateTime parseDateTime(final String dateTime)
    {
        return (OffsetDateTime) parse(dateTime, ParseConfig.DEFAULT, false);
    }

    @Override
    public String formatUtcMilli(OffsetDateTime date)
    {
        return formatUtc(date, 3);
    }

    @Override
    public String formatUtcMicro(OffsetDateTime date)
    {
        return formatUtc(date, 6);
    }

    @Override
    public String formatUtcNano(OffsetDateTime date)
    {
        return formatUtc(date, 9);
    }

    @Override
    public String formatUtc(OffsetDateTime date)
    {
        return formatUtc(date, 0);
    }

    @Override
    public DateTime parse(String chars)
    {
        return (DateTime) parse(chars, ParseConfig.DEFAULT, true);
    }

    @Override
    public DateTime parse(String chars, ParseConfig config)
    {
        return (DateTime) parse(chars, config, true);
    }
}
