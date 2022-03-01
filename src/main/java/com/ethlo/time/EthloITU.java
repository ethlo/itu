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
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

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
    public static final int MAXIMUM_POSSIBLE_LENGTH = 31;
    private final Java8Rfc3339 delegate = new Java8Rfc3339();
    private final LeapSecondHandler leapSecondHandler = new DefaultLeapSecondHandler();

    @Override
    public OffsetDateTime parseDateTime(String text)
    {
        Objects.requireNonNull(text, "text");

        final int year = getYear(text);

        assertPositionContains(text, 4, DATE_SEPARATOR);
        final int month = getMonth(text);

        assertPositionContains(text, 7, DATE_SEPARATOR);
        final int day = getDay(text);

        // *** Time starts ***//

        // HOURS
        assertPositionContains(text, 10, SEPARATOR_UPPER, SEPARATOR_LOWER, SEPARATOR_SPACE);
        final int hour = getHour(text);

        // MINUTES
        assertPositionContains(text, 13, TIME_SEPARATOR);
        final int minute = getMinute(text);

        // SECONDS or TIMEZONE
        return handleTime(text, year, month, day, hour, minute);
    }

    private int getHour(final String chars)
    {
        return parsePositiveInt(chars, 11, 13);
    }

    private int getMinute(final String chars)
    {
        return parsePositiveInt(chars, 14, 16);
    }

    private int getDay(final String chars)
    {
        return parsePositiveInt(chars, 8, 10);
    }

    private OffsetDateTime handleTime(final String text, final int year, final int month, final int day, int hour, int minute)
    {
        switch (text.charAt(16))
        {
            // We have more granularity, keep going
            case TIME_SEPARATOR:
                return seconds(year, month, day, hour, minute, text);

            case PLUS:
            case MINUS:
            case ZULU_UPPER:
            case ZULU_LOWER:
                final ZoneOffset zoneOffset = parseTz(text, 16);
                return OffsetDateTime.of(year, month, day, hour, minute, 0, 0, zoneOffset);

            default:
                assertPositionContains(text, 16, TIME_SEPARATOR, PLUS, MINUS, ZULU_UPPER);
        }
        throw new DateTimeException(text);
    }

    private void assertPositionContains(final String text, final int offset, final char expected)
    {
        if (offset >= text.length())
        {
            throw new DateTimeException("Abrupt end of input: " + text);
        }

        if (text.charAt(offset) != expected)
        {
            throw new DateTimeException("Expected character " + expected
                    + " at position " + (offset + 1) + " '" + text + "'");
        }
    }

    private void assertPositionContains(final String text, final int offset, final char... expected)
    {
        if (offset >= text.length())
        {
            throw new DateTimeException("Abrupt end of input: " + text);
        }

        boolean found = false;
        final char c = text.charAt(offset);
        for (char e : expected)
        {
            if (c == e)
            {
                found = true;
                break;
            }
        }
        if (!found)
        {
            throw new DateTimeException("Expected character " + Arrays.toString(expected)
                    + " at position " + (offset + 1) + " '" + text + "'");
        }
    }

    private ZoneOffset parseTz(final String text, final int offset)
    {
        final int left = text.length() - offset;
        final char c = text.charAt(offset);
        if (c == ZULU_UPPER || c == ZULU_LOWER)
        {
            assertNoMoreChars(text, offset);
            return ZoneOffset.UTC;
        }

        if (left != 6)
        {
            throw new DateTimeException("Invalid timezone offset: " + text.substring(offset));
        }

        final char sign = text.charAt(offset);
        int hours = parsePositiveInt(text, offset + 1, offset + 3);
        int minutes = parsePositiveInt(text, offset + 4, offset + 4 + 2);
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

    private void assertNoMoreChars(final String text, int lastUsed)
    {
        if (text.length() > lastUsed + 1)
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
        final ZonedDateTime utc = date.atZoneSameInstant(FastUTCZoneId.get());

        final char[] buffer = new char[MAXIMUM_POSSIBLE_LENGTH];

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

    public <T extends Temporal> Temporal doParseLenient(String text, Class<T> type)
    {
        if (text == null || text.isEmpty())
        {
            return null;
        }

        final Field maxRequired = type == null ? null : Field.valueOf(type);

        // Date portion

        // YEAR
        final int year = getYear(text);
        if (maxRequired == Field.YEAR || text.length() == 4)
        {
            return Year.of(year);
        }

        // MONTH
        assertPositionContains(text, 4, DATE_SEPARATOR);
        final int month = getMonth(text);
        if (maxRequired == Field.MONTH || text.length() == 7)
        {
            return YearMonth.of(year, month);
        }

        // DAY
        assertPositionContains(text, 7, DATE_SEPARATOR);
        final int day = getDay(text);
        if (maxRequired == Field.DAY || text.length() == 10)
        {
            return LocalDate.of(year, month, day);
        }

        // HOURS
        assertPositionContains(text, 10, SEPARATOR_UPPER, SEPARATOR_LOWER, SEPARATOR_SPACE);
        final int hour = getHour(text);

        // MINUTES
        assertPositionContains(text, 13, TIME_SEPARATOR);
        final int minute = getMinute(text);
        if (maxRequired == Field.MINUTE || text.length() == 16)
        {
            return LocalDate.of(year, month, day);
        }

        // SECONDS or TIMEZONE
        return handleTime(text, year, month, day, hour, minute);
    }

    private int getMonth(final String chars)
    {
        return parsePositiveInt(chars, 5, 7);
    }

    private int getYear(final String chars)
    {
        return parsePositiveInt(chars, 0, 4);
    }

    private OffsetDateTime seconds(int year, int month, int day, int hour, int minute, final String text)
    {
        final int second = getSecond(text);

        // From here the specification is more lenient
        final int remaining = text.length() - 19;

        ZoneOffset offset;
        int fractions = 0;

        if (remaining == 0)
        {
            throw new DateTimeException("Unexpected end of expression at position 19 '" + text + "'");
        }

        final char c = text.charAt(19);
        if (remaining == 1 && (c == ZULU_UPPER || c == ZULU_LOWER))
        {
            // Do nothing we are done
            offset = ZoneOffset.UTC;
            assertNoMoreChars(text, 19);
        }
        else if (remaining >= 1 && c == FRACTION_SEPARATOR)
        {
            // We have fractional seconds
            final int idx = indexOfNonDigit(text, 20);
            if (idx != -1)
            {
                // We have an end of fractions
                final int len = idx - 20;
                fractions = getFractions(text, idx, len);
                offset = parseTz(text, idx);
            }
            else
            {
                offset = parseTz(text, 20);
            }
        }
        else if (remaining >= 1 && (c == PLUS || c == MINUS))
        {
            // No fractional sections
            offset = parseTz(text, 19);
        }
        else
        {
            throw new DateTimeException("Unexpected character at position 19:" + c);
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
                    throw new LeapSecondException(OffsetDateTime.of(year, month, day, hour, minute, 59, fractions, offset).plusSeconds(1), second, isValidLeapYearMonth);
                }
            }
        }
        return OffsetDateTime.of(year, month, day, hour, minute, second, fractions, offset);
    }

    private int getSecond(final String text)
    {
        return parsePositiveInt(text, 17, 19);
    }

    private int getFractions(final String text, final int idx, final int len)
    {
        final int fractions;
        fractions = parsePositiveInt(text, 20, idx);
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
