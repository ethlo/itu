package com.ethlo.time;

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

import static com.ethlo.time.LeapSecondHandler.LEAP_SECOND_SECONDS;
import static com.ethlo.time.LimitedCharArrayIntegerUtil.indexOfNonDigit;
import static com.ethlo.time.LimitedCharArrayIntegerUtil.parsePositiveInt;

import java.time.DateTimeException;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;

public class EthloITU extends AbstractRfc3339 implements W3cDateTimeUtil
{
    private static final char PLUS = '+';
    private static final char MINUS = '-';
    private static final char DATE_SEPARATOR = '-';
    private static final char TIME_SEPARATOR = ':';
    private static final char SEPARATOR_UPPER = 'T';
    private static final char SEPARATOR_LOWER = 't';
    private static final char SEPARATOR_SPACE = ' ';
    private static final char FRACTION_SEPARATOR = '.';
    private static final char ZULU_UPPER = 'Z';
    private static final char ZULU_LOWER = 'z';
    private static final int[] widths = new int[]{100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10, 1};
    private final JdkRfc3339 delegate = new JdkRfc3339();
    private final LeapSecondHandler leapSecondHandler = new DefaultLeapSecondHandler();

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
                return seconds(year, month, day, hour, minute, chars);

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
        final int left = chars.length - offset;
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
            throw new DateTimeException("Unparsed data from offset " + lastUsed + 1);
        }
    }

    @Override
    public String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        return doFormatUtc(date, Field.SECOND, fractionDigits);
    }

    @Override
    public String formatUtc(OffsetDateTime date, Field lastIncluded)
    {
        return doFormatUtc(date, lastIncluded, 0);
    }

    private String doFormatUtc(OffsetDateTime date, Field lastIncluded, int fractionDigits)
    {
        assertMaxFractionDigits(fractionDigits);

        OffsetDateTime utc = date;
        if (date.getOffset() != ZoneOffset.UTC)
        {
            utc = date.atZoneSameInstant(FastUTCZoneId.get()).toOffsetDateTime();
        }

        final char[] buffer = new char[31];

        if (handleDatePart(lastIncluded, buffer, utc.getYear(), 0, 4, Field.YEAR))
        {
            return finish(buffer, Field.YEAR.getRequiredLength());
        }

        buffer[4] = DATE_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, utc.getMonthValue(), 5, 2, Field.MONTH))
        {
            return finish(buffer, Field.MONTH.getRequiredLength());
        }

        buffer[7] = DATE_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, utc.getDayOfMonth(), 8, 2, Field.DAY))
        {
            return finish(buffer, Field.DAY.getRequiredLength());
        }

        // T separator
        buffer[10] = SEPARATOR_UPPER;

        // Time
        LimitedCharArrayIntegerUtil.toString(utc.getHour(), buffer, 11, 2);
        buffer[13] = TIME_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, utc.getMinute(), 14, 2, Field.MINUTE))
        {
            return finish(buffer, Field.MINUTE.getRequiredLength());
        }
        buffer[16] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.toString(utc.getSecond(), buffer, 17, 2);

        // Second fractions
        final boolean hasFractionDigits = fractionDigits > 0;
        if (hasFractionDigits)
        {
            buffer[19] = FRACTION_SEPARATOR;
            addFractions(buffer, fractionDigits, utc.getNano());
        }

        final int length = addUtcTimezone(fractionDigits, buffer, hasFractionDigits);

        return finish(buffer, length);
    }

    private int addUtcTimezone(final int fractionDigits, final char[] buf, final boolean hasFractionDigits)
    {
        buf[(hasFractionDigits ? 20 + fractionDigits : 19)] = ZULU_UPPER;
        return hasFractionDigits ? 21 + fractionDigits : 20;
    }

    private boolean handleDatePart(final Field lastIncluded, final char[] buffer, final int value, final int offset, final int length, final Field field)
    {
        LimitedCharArrayIntegerUtil.toString(value, buffer, offset, length);
        return lastIncluded == field;
    }

    private String finish(char[] buf, int length)
    {
        return new String(buf, 0, length);
    }

    private void addFractions(char[] buf, int fractionDigits, int nano)
    {
        final double d = widths[fractionDigits - 1];
        LimitedCharArrayIntegerUtil.toString((int) (nano / d), buf, 20, fractionDigits);
    }

    @Override
    public String formatUtc(Date date)
    {
        return formatUtc(OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC), 3);
    }

    @Override
    public String format(Date date, String timezone)
    {
        return delegate.format(date, timezone);
    }

    @Override
    public OffsetDateTime parseDateTime(final String dateTime)
    {
        return parse(dateTime).assertMinGranularity(Field.SECOND).toOffsetDatetime();
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
    public String formatUtcMilli(Date date)
    {
        return formatUtcMilli(OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC));
    }

    @Override
    public String format(Date date, String timezone, int fractionDigits)
    {
        return delegate.format(date, timezone, fractionDigits);
    }

    @Override
    public DateTime parse(String text)
    {
        if (text == null)
        {
            throw new NullPointerException("text cannot be null");
        }

        final char[] chars = text.toCharArray();

        // Date portion

        // YEAR
        final int years = getYear(chars);
        if (chars.length == 4)
        {
            return DateTime.ofYear(years);
        }

        // MONTH
        assertPositionContains(chars, 4, DATE_SEPARATOR);
        final int months = getMonth(chars);
        if (chars.length == 7)
        {
            return DateTime.ofYearMonth(years, months);
        }

        // DAY
        assertPositionContains(chars, 7, DATE_SEPARATOR);
        final int days = getDay(chars);
        if (chars.length == 10)
        {
            return DateTime.ofDate(years, months, days);
        }

        // HOURS
        assertPositionContains(chars, 10, SEPARATOR_UPPER, SEPARATOR_LOWER, SEPARATOR_SPACE);
        final int hours = getHour(chars);

        // MINUTES
        assertPositionContains(chars, 13, TIME_SEPARATOR);
        final int minutes = getMinute(chars);
        if (chars.length == 16)
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

    private DateTime seconds(int year, int month, int day, int hour, int minute, char[] chars)
    {
        final int second = getSecond(chars);

        // From here the specification is more lenient
        final int remaining = chars.length - 19;
        if (remaining == 0)
        {
            return DateTime.of(year, month, day, hour, minute, second, 0, null);
        }

        TimezoneOffset offset = null;
        int fractions = 0;

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
                    throw new LeapSecondException(OffsetDateTime.of(year, month, day, hour, minute, 59, fractions, offset.asJavaTimeOffset()).plusSeconds(1), second, isValidLeapYearMonth);
                }
            }
        }
        return DateTime.of(year, month, day, hour, minute, second, fractions, offset);
    }

    private void raiseDateTimeException(char[] chars, String message)
    {
        throw new DateTimeException(message + ": " + new String(chars));
    }

    private int getSecond(final char[] chars)
    {
        return parsePositiveInt(chars, 17, 19);
    }

    private int getFractions(final char[] chars, final int idx, final int len)
    {
        final int fractions;
        fractions = parsePositiveInt(chars, 20, idx);
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
