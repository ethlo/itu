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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Date;

public class FastInternetDateTimeUtil extends AbstractRfc3339 implements W3cDateTimeUtil
{
    public static final int LEAP_SECOND_SECONDS = 60;
    private final StdJdkInternetDateTimeUtil delegate = new StdJdkInternetDateTimeUtil();

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

    @Override
    public OffsetDateTime parseDateTime(String s)
    {
        final Temporal t = doParseLenient(s, OffsetDateTime.class);
        if (t == null)
        {
            return null;
        }
        else if (t instanceof OffsetDateTime)
        {
            return (OffsetDateTime) t;
        }
        throw new DateTimeException("Invalid RFC-3339 date-time: " + s);
    }

    private void assertPositionContains(char[] chars, int offset, char... expected)
    {
        if (offset >= chars.length)
        {
            throw new DateTimeException("Abrupt end of input: " + new String(chars));
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

    private ZoneOffset parseTz(char[] chars, int offset)
    {
        final int left = chars.length - offset;
        if (chars[offset] == ZULU_UPPER || chars[offset] == ZULU_LOWER)
        {
            assertNoMoreChars(chars, offset);
            return ZoneOffset.UTC;
        }

        if (left != 6)
        {
            throw new DateTimeException("Invalid timezone offset: " + new String(chars, offset, left));
        }

        final char sign = chars[offset];
        int hours = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, offset + 1, offset + 3);
        int minutes = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, offset + 4, offset + 4 + 2);
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

        return ZoneOffset.ofHoursMinutes(hours, minutes);
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
        return formatUtc(date, Field.SECOND, fractionDigits);
    }

    @Override
    public String format(OffsetDateTime date, Field lastIncluded)
    {
        return formatUtc(date, lastIncluded, 0);
    }

    @Override
    public String formatUtc(OffsetDateTime date, Field lastIncluded, int fractionDigits)
    {
        assertMaxFractionDigits(fractionDigits);
        final LocalDateTime utc = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);

        final char[] buffer = new char[31];

        if (handleDatePart(lastIncluded, buffer, utc.getYear(), 0, 4, Field.YEAR))
        {
            return finish(buffer, 4);
        }

        buffer[4] = DATE_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, utc.getMonthValue(), 5, 2, Field.MONTH))
        {
            return finish(buffer, 7);
        }

        buffer[7] = DATE_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, utc.getDayOfMonth(), 8, 2, Field.DAY))
        {
            return finish(buffer, 10);
        }

        // T separator
        buffer[10] = SEPARATOR_UPPER;

        // Time
        LimitedCharArrayIntegerUtil.toString(utc.getHour(), buffer, 11, 2);
        buffer[13] = TIME_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, utc.getMinute(), 14, 2, Field.MINUTE))
        {
            return finish(buffer, 16);
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
    public boolean isValid(String dateTime)
    {
        try
        {
            parseDateTime(dateTime);
            return true;
        }
        catch (DateTimeException exc)
        {
            return false;
        }
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
    public Temporal parseLenient(String s)
    {
        return doParseLenient(s, null);
    }

    @Override
    public <T extends Temporal> T parseLenient(String s, Class<T> type)
    {
        return type.cast(doParseLenient(s, type));
    }

    public <T extends Temporal> Temporal doParseLenient(String s, Class<T> type)
    {
        if (s == null || s.isEmpty())
        {
            return null;
        }

        final Field maxRequired = type == null ? null : Field.valueOf(type);
        final char[] chars = s.toCharArray();

        // Date portion

        // YEAR
        final int year = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, 0, 4);
        if (maxRequired == Field.YEAR || chars.length == 4)
        {
            return Year.of(year);
        }

        // MONTH
        assertPositionContains(chars, 4, DATE_SEPARATOR);
        final int month = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, 5, 7);
        if (maxRequired == Field.MONTH || chars.length == 7)
        {
            return YearMonth.of(year, month);
        }

        // DAY
        assertPositionContains(chars, 7, DATE_SEPARATOR);
        final int day = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, 8, 10);
        if (maxRequired == Field.DAY || chars.length == 10)
        {
            return LocalDate.of(year, month, day);
        }

        // *** Time starts ***//

        // HOURS
        assertPositionContains(chars, 10, SEPARATOR_UPPER, SEPARATOR_LOWER, SEPARATOR_SPACE);
        final int hour = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, 11, 13);

        // MINUTES
        assertPositionContains(chars, 13, TIME_SEPARATOR);
        final int minute = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, 14, 16);
        if (maxRequired == Field.MINUTE || chars.length == 16)
        {
            return LocalDate.of(year, month, day);
        }

        // SECONDS or TIMEZONE
        switch (chars[16])
        {
            // We have more granularity, keep going
            case TIME_SEPARATOR:
                return seconds(year, month, day, hour, minute, chars);

            case PLUS:
            case MINUS:
            case ZULU_UPPER:
            case ZULU_LOWER:
                final ZoneOffset zoneOffset = parseTz(chars, 16);
                return OffsetDateTime.of(year, month, day, hour, minute, 0, 0, zoneOffset);

            default:
                assertPositionContains(chars, 16, TIME_SEPARATOR, PLUS, MINUS, ZULU_UPPER);
        }
        throw new DateTimeException(new String(chars));
    }

    private OffsetDateTime seconds(int year, int month, int day, int hour, int minute, char[] chars)
    {
        final int second = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, 17, 19);

        // From here the specification is more lenient
        final int remaining = chars.length - 19;

        ZoneOffset offset;
        int fractions = 0;

        if (remaining == 1 && (chars[19] == ZULU_UPPER || chars[19] == ZULU_LOWER))
        {
            // Do nothing we are done
            offset = ZoneOffset.UTC;
            assertNoMoreChars(chars, 19);
        }
        else if (remaining >= 1 && chars[19] == FRACTION_SEPARATOR)
        {
            // We have fractional seconds
            final int idx = LimitedCharArrayIntegerUtil.indexOfNonDigit(chars, 20);
            if (idx != -1)
            {
                // We have an end of fractions
                final int len = idx - 20;
                fractions = getFractions(chars, idx, len);
                offset = parseTz(chars, idx);
            }
            else
            {
                offset = parseTz(chars, 20);
            }
        }
        else if (remaining >= 1 && (chars[19] == PLUS || chars[19] == MINUS))
        {
            // No fractional sections
            offset = parseTz(chars, 19);
        }
        else if (remaining == 0)
        {
            throw new DateTimeException("Unexpected end of expression at position 19 '" + new String(chars) + "'");
        }
        else
        {
            throw new DateTimeException("Unexpected character at position 19:" + chars[19]);
        }

        if (second == LEAP_SECOND_SECONDS)
        {
            // Do not fall over trying to parse leap seconds
            final int utcHour = hour - (offset.getTotalSeconds() / 3_600);
            final int utcMinute = minute - ((offset.getTotalSeconds() % 3_600) / 60);
            if (((month == Month.DECEMBER.getValue() && day == 31) || (month == Month.JUNE.getValue() && day == 30))
                    && utcHour == 23
                    && utcMinute == 59)
            {
                // Consider it a leap second
                return OffsetDateTime.of(year, month, day, hour, minute, 59, fractions, offset).plusSeconds(1);
            }
        }
        return OffsetDateTime.of(year, month, day, hour, minute, second, fractions, offset);
    }

    private int getFractions(final char[] chars, final int idx, final int len)
    {
        final int fractions;
        fractions = LimitedCharArrayIntegerUtil.parsePositiveInt(chars, 20, idx);
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
