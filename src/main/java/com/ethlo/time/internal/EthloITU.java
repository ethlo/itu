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

import static com.ethlo.time.internal.LeapSecondHandler.LEAP_SECOND_SECONDS;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.indexOfNonDigit;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.parsePositiveInt;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.uncheckedParsePositiveInt;

import java.time.DateTimeException;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;

import com.ethlo.time.DateTime;
import com.ethlo.time.Field;
import com.ethlo.time.LeapSecondException;
import com.ethlo.time.TimezoneOffset;

public class EthloITU extends AbstractRfc3339 implements W3cDateTimeUtil
{
    private static final EthloITU instance = new EthloITU();

    private static final char PLUS = '+';
    private static final char MINUS = '-';
    public static final char DATE_SEPARATOR = '-';
    public static final char TIME_SEPARATOR = ':';
    public static final char SEPARATOR_UPPER = 'T';
    private static final char SEPARATOR_LOWER = 't';
    private static final char SEPARATOR_SPACE = ' ';
    private static final char FRACTION_SEPARATOR = '.';
    private static final char ZULU_UPPER = 'Z';
    private static final char ZULU_LOWER = 'z';
    private static final int[] widths = new int[]{100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10, 1};
    private final LeapSecondHandler leapSecondHandler = new DefaultLeapSecondHandler();

    private EthloITU()
    {

    }

    public static EthloITU getInstance()
    {
        return instance;
    }

    private int getHour(final char[] chars)
    {
        return parsePositiveInt(chars, 11, 13);
    }

    private int getMinute(final char[] chars)
    {
        return parsePositiveInt(chars, 14, 16);
    }

    private int getDay(final char[] chars)
    {
        return parsePositiveInt(chars, 8, 10);
    }

    private DateTime handleTime(char[] chars, int year, int month, int day, int hour, int minute)
    {
        switch (chars[16])
        {
            // We have more granularity, keep going
            case TIME_SEPARATOR:
                return handleSeconds(year, month, day, hour, minute, chars);

            case PLUS:
            case MINUS:
            case ZULU_UPPER:
            case ZULU_LOWER:
                final TimezoneOffset zoneOffset = parseTimezone(chars, 16);
                return DateTime.of(year, month, day, hour, minute, zoneOffset);

            default:
                assertPositionContains(chars, 16, TIME_SEPARATOR, PLUS, MINUS, ZULU_UPPER);
        }
        throw new DateTimeException(new String(chars));
    }

    private void assertPositionContains(char[] chars, int offset, char expected)
    {
        if (offset >= chars.length)
        {
            raiseDateTimeException(chars, "Unexpected end of input");
        }

        if (chars[offset] != expected)
        {
            throw new DateTimeException("Expected character " + expected
                    + " at position " + (offset + 1) + " '" + new String(chars) + "'");
        }
    }

    private void assertPositionContains(char[] chars, int offset, char... expected)
    {
        if (offset >= chars.length)
        {
            raiseDateTimeException(chars, "Unexpected end of input");
        }

        boolean found = false;
        for (char e : expected)
        {
            if (chars[offset] == e)
            {
                found = true;
                break;
            }
        }
        if (!found)
        {
            throw new DateTimeException("Expected character " + Arrays.toString(expected)
                    + " at position " + (offset + 1) + " '" + new String(chars) + "'");
        }
    }

    private TimezoneOffset parseTimezone(char[] chars, int offset)
    {
        final int len = chars.length;
        final int left = len - offset;
        final char c = chars[offset];
        if (c == ZULU_UPPER || c == ZULU_LOWER)
        {
            assertNoMoreChars(chars, offset);
            return TimezoneOffset.UTC;
        }

        if (left != 6)
        {
            throw new DateTimeException("Invalid timezone offset: " + new String(chars, offset, left));
        }

        final char sign = chars[offset];
        int hours = parsePositiveInt(chars, offset + 1, offset + 3);
        int minutes = parsePositiveInt(chars, offset + 4, offset + 4 + 2);
        if (sign == MINUS)
        {
            hours = -hours;
            minutes = -minutes;
        }
        else if (sign != PLUS)
        {
            throw new DateTimeException("Invalid character starting at position " + offset + 1);
        }

        if (sign == MINUS && hours == 0 && minutes == 0)
        {
            throw new DateTimeException("Unknown 'Local Offset Convention' date-time not allowed");
        }

        return TimezoneOffset.ofHoursMinutes(hours, minutes);
    }

    private void assertNoMoreChars(char[] chars, int lastUsed)
    {
        if (chars.length > lastUsed + 1)
        {
            throw new DateTimeException("Trailing junk data after position " + (lastUsed + 1) + ": " + new String(chars));
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

        final char[] buffer = new char[31];

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

    private void addFractions(char[] buf, int fractionDigits, int nano)
    {
        final double d = widths[fractionDigits - 1];
        LimitedCharArrayIntegerUtil.toString((int) (nano / d), buf, 20, fractionDigits);
    }

    @Override
    public OffsetDateTime parseDateTime(final String dateTime)
    {
        return assertSecondsGranularity(parse(dateTime)).toOffsetDatetime();
    }

    private DateTime assertSecondsGranularity(DateTime dt)
    {
        if (!dt.includesGranularity(Field.SECOND))
        {
            throw new DateTimeException("No " + Field.SECOND.name() + " field found");
        }
        return dt;
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
    public DateTime parse(String text)
    {
        if (text == null)
        {
            throw new NullPointerException("text cannot be null");
        }

        final int len = text.length();
        final char[] chars = text.toCharArray();

        // Date portion

        // YEAR
        final int years = getYear(chars);
        if (4 == len)
        {
            return DateTime.ofYear(years);
        }

        // MONTH
        assertPositionContains(chars, 4, DATE_SEPARATOR);
        final int months = getMonth(chars);
        if (7 == len)
        {
            return DateTime.ofYearMonth(years, months);
        }

        // DAY
        assertPositionContains(chars, 7, DATE_SEPARATOR);
        final int days = getDay(chars);
        if (10 == len)
        {
            return DateTime.ofDate(years, months, days);
        }

        // HOURS
        assertPositionContains(chars, 10, SEPARATOR_UPPER, SEPARATOR_LOWER, SEPARATOR_SPACE);
        final int hours = getHour(chars);

        // MINUTES
        assertPositionContains(chars, 13, TIME_SEPARATOR);
        final int minutes = getMinute(chars);
        if (16 == len)
        {
            return DateTime.of(years, months, days, hours, minutes, null);
        }

        // SECONDS or TIMEZONE
        return handleTime(chars, years, months, days, hours, minutes);
    }

    private int getMonth(final char[] chars)
    {
        return parsePositiveInt(chars, 5, 7);
    }

    private int getYear(final char[] chars)
    {
        return parsePositiveInt(chars, 0, 4);
    }

    private DateTime handleSeconds(int year, int month, int day, int hour, int minute, char[] chars)
    {
        // From here the specification is more lenient
        final int remaining = chars.length - 19;
        if (remaining == 0)
        {
            final int seconds = getSeconds(chars);
            return leapSecondCheck(year, month, day, hour, minute, seconds, 0, null, false);
        }

        TimezoneOffset offset = null;
        int fractions = 0;
        boolean hasFractions = false;
        final char c = chars[19];
        if (remaining == 1 && (c == ZULU_UPPER || c == ZULU_LOWER))
        {
            // Do nothing we are done
            offset = TimezoneOffset.UTC;
            assertNoMoreChars(chars, 19);
        }
        else if (remaining >= 1 && c == FRACTION_SEPARATOR)
        {
            // We have fractional seconds
            final int idx = indexOfNonDigit(chars, 20);
            if (idx != -1)
            {
                // We have an end of fractions
                final int len = idx - 20;
                fractions = getFractions(chars, idx, len);
                hasFractions = true;
                offset = parseTimezone(chars, idx);
            }
            else
            {
                raiseDateTimeException(chars, "No timezone information");
            }
        }
        else if (remaining >= 1 && (c == PLUS || c == MINUS))
        {
            // No fractional sections
            offset = parseTimezone(chars, 19);
        }
        else
        {
            raiseDateTimeException(chars, "Unexpected character at position 19");
        }

        return leapSecondCheck(year, month, day, hour, minute, getSeconds(chars), fractions, offset, hasFractions);
    }

    private DateTime leapSecondCheck(int year, int month, int day, int hour, int minute, int second, int nanos, TimezoneOffset offset, final boolean hasFractions)
    {
        if (second == LEAP_SECOND_SECONDS)
        {
            // Do not fall over trying to parse leap seconds
            final YearMonth needle = YearMonth.of(year, month);
            final boolean isValidLeapYearMonth = leapSecondHandler.isValidLeapSecondDate(needle);
            if (isValidLeapYearMonth || needle.isAfter(leapSecondHandler.getLastKnownLeapSecond()))
            {
                final int utcHour = hour - (offset.getTotalSeconds() / 3_600);
                final int utcMinute = minute - ((offset.getTotalSeconds() % 3_600) / 60);
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
        return hasFractions ? DateTime.of(year, month, day, hour, minute, second, nanos, offset) : DateTime.of(year, month, day, hour, minute, second, offset);
    }

    private void raiseDateTimeException(char[] chars, String message)
    {
        throw new DateTimeException(message + ": " + new String(chars));
    }

    private int getSeconds(final char[] chars)
    {
        return parsePositiveInt(chars, 17, 19);
    }

    private int getFractions(final char[] chars, final int idx, final int len)
    {
        final int fractions;
        fractions = uncheckedParsePositiveInt(chars, 20, idx);
        switch (len)
        {
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
}
