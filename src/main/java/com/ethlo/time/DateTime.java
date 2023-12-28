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

import static com.ethlo.time.internal.EthloITU.DATE_SEPARATOR;
import static com.ethlo.time.internal.EthloITU.SEPARATOR_UPPER;
import static com.ethlo.time.internal.EthloITU.TIME_SEPARATOR;
import static com.ethlo.time.internal.EthloITU.finish;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

import com.ethlo.time.internal.LimitedCharArrayIntegerUtil;

/**
 * Container class for parsed date/date-time data. The {@link #getMostGranularField()} contains the highest granularity field found, like MONTH, MINUTE, SECOND.
 */
public class DateTime
{
    private final Field field;
    private final int year;
    private final int month;
    private final int day;
    private final int hour;
    private final int minute;
    private final int second;
    private final int nano;
    private final TimezoneOffset offset;
    private final int fractionDigits;

    public DateTime(final Field field, final int year, final int month, final int day, final int hour, final int minute, final int second, final int nano, final TimezoneOffset offset, final int fractionDigits)
    {
        this.field = field;
        this.year = year;
        this.month = assertSize(month, 1, 12, Field.MONTH);
        this.day = assertSize(day, 1, 31, Field.DAY);
        this.hour = assertSize(hour, 0, 23, Field.HOUR);
        this.minute = assertSize(minute, 0, 59, Field.MINUTE);
        this.second = assertSize(second, 0, 60, Field.SECOND);
        this.nano = assertSize(nano, 0, 999_999_999, Field.NANO);
        this.offset = offset;
        this.fractionDigits = fractionDigits;
    }

    /**
     * Create a new instance with second granularity from the input parameters
     *
     * @param year year
     * @param month month
     * @param day day
     * @param hour hour
     * @param minute minute
     * @param second second
     * @param offset timezone offset
     * @return A DateTime with second granularity
     */
    public static DateTime of(int year, int month, int day, int hour, int minute, int second, TimezoneOffset offset)
    {
        return new DateTime(Field.SECOND, year, month, day, hour, minute, second, 0, offset, 0);
    }

    /**
     * Create a new instance with nanosecond granularity from the input parameters
     *
     * @param year year
     * @param month month
     * @param day day
     * @param hour hour
     * @param minute minute
     * @param second second
     * @param nanos nanos
     * @param offset timezone offset
     * @return A DateTime with nanosecond granularity
     */
    public static DateTime of(int year, int month, int day, int hour, int minute, int second, int nanos, TimezoneOffset offset, final int fractionDigits)
    {
        return new DateTime(Field.NANO, year, month, day, hour, minute, second, nanos, offset, fractionDigits);
    }

    /**
     * Create a new instance with year granularity from the input parameters
     * @param year The year
     * @return a new instance with year granularity from the input parameters
     */
    public static DateTime ofYear(int year)
    {
        return new DateTime(Field.YEAR, year, 0, 0, 0, 0, 0, 0, null, 0);
    }

    /**
     * Create a new instance with year-month granularity from the input parameters
     * @param year The year
     * @param month The month
     * @return a new instance with year-month granularity from the input parameters
     */
    public static DateTime ofYearMonth(int year, int month)
    {
        return new DateTime(Field.MONTH, year, month, 0, 0, 0, 0, 0, null, 0);
    }

    /**
     * Create a new instance with day granularity from the input parameters
     * @param year The year
     * @param month The month
     * @param day The day
     * @return a new instance with day granularity from the input parameters
     */
    public static DateTime ofDate(int year, int month, int day)
    {
        return new DateTime(Field.DAY, year, month, day, 0, 0, 0, 0, null, 0);
    }

    /**
     * Create a new instance with minute granularity from the input parameters
     * @param year The year
     * @param month The month
     * @param day The day
     * @param hour The hour
     * @param minute The minute
     * @param offset The timezone offset
     * @return a new instance with minute granularity from the input parameters
     */
    public static DateTime of(int year, int month, int day, int hour, int minute, TimezoneOffset offset)
    {
        return new DateTime(Field.MINUTE, year, month, day, hour, minute, 0, 0, offset, 0);
    }

    /**
     * Create a new instance with data from the specified date-time.
     *
     * @param dateTime The date-time to copy data from
     * @return A new instance
     */
    public static DateTime of(OffsetDateTime dateTime)
    {
        return DateTime.of(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getNano(), TimezoneOffset.of(dateTime.getOffset()), 9);
    }

    private int assertSize(int value, int min, int max, Field field)
    {
        if (value > max)
        {
            throw new DateTimeException("Field " + field.name() + " out of bounds. Expected " + min + "-" + max + ", got " + value);
        }
        return value;
    }

    /**
     * Returns if the specified field is part of this date/date-time
     *
     * @param field The field to check for
     * @return True if included, otherwise false
     */
    public boolean includesGranularity(Field field)
    {
        return field.ordinal() <= this.field.ordinal();
    }

    public int getYear()
    {
        return year;
    }

    public int getMonth()
    {
        return month;
    }

    public int getDayOfMonth()
    {
        return day;
    }

    public int getHour()
    {
        return hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public int getSecond()
    {
        return second;
    }

    public int getNano()
    {
        return nano;
    }

    /**
     * Returns the time offset, if available
     *
     * @return the time offset, if available
     */
    public Optional<TimezoneOffset> getOffset()
    {
        return Optional.ofNullable(offset);
    }

    /**
     * Creates a {@link Year} discarding any higher granularity fields
     *
     * @return the {@link Year}
     */
    public Year toYear()
    {
        return Year.of(year);
    }

    /**
     * Creates a {@link YearMonth} discarding any higher granularity fields
     *
     * @return the {@link YearMonth}
     */
    public YearMonth toYearMonth()
    {
        assertMinGranularity(Field.MONTH);
        return YearMonth.of(year, month);
    }

    /**
     * Creates a {@link LocalDateTime} discarding any timezone information
     *
     * @return the {@link LocalDateTime}
     */
    public LocalDateTime toLocalDatetime()
    {
        assertMinGranularity(Field.MINUTE);
        return LocalDateTime.of(year, month, day, hour, minute, second, nano);
    }

    /**
     * Creates an {@link OffsetDateTime}
     *
     * @return the {@link OffsetDateTime}
     */
    public OffsetDateTime toOffsetDatetime()
    {
        assertMinGranularity(Field.MINUTE);
        return toOffsetDatetimeNoGranularityCheck();
    }

    public OffsetDateTime toOffsetDatetimeNoGranularityCheck()
    {
        if (offset != null)
        {
            return OffsetDateTime.of(year, month, day, hour, minute, second, nano, offset.toZoneOffset());
        }
        throw new DateTimeException("No zone offset information found");
    }

    /**
     * Creates a {@link LocalDate}, discarding any higher granularity fields
     *
     * @return the {@link LocalDate}
     */
    public LocalDate toLocalDate()
    {
        assertMinGranularity(Field.DAY);
        return LocalDate.of(year, month, day);
    }

    /**
     * Returns the most granular field found during parsing
     *
     * @return The field found
     */
    public Field getMostGranularField()
    {
        return field;
    }

    private void assertMinGranularity(Field field)
    {
        if (!includesGranularity(field))
        {
            throw new DateTimeException("No " + field.name() + " field found");
        }
    }

    /**
     * Formats this date-time as an ISO formatted string with the last included field as specified.
     *
     * @param lastIncluded The last specified field to include
     * @return The formatted date/date-time string
     */
    public String toString(final Field lastIncluded)
    {
        return toString(this, lastIncluded, 0);
    }

    /**
     * Formats this date-time as an RFC-3339 compatible string with the specified number of fractions in the second.
     *
     * @param fractionDigits The number of fractions to include
     * @return The formatted date/date-time string
     */
    public String toString(final int fractionDigits)
    {
        return toString(this, Field.NANO, fractionDigits);
    }

    private String toString(final DateTime date, final Field lastIncluded, final int fractionDigits)
    {
        if (lastIncluded.ordinal() > date.getMostGranularField().ordinal())
        {
            throw new DateTimeException("Requested granularity was " + lastIncluded.name() + ", but contains only granularity " + date.getMostGranularField().name());
        }
        final TimezoneOffset tz = date.getOffset().orElse(null);
        final char[] buffer = new char[35];

        // YEAR
        LimitedCharArrayIntegerUtil.toString(date.getYear(), buffer, 0, 4);
        if (lastIncluded == Field.YEAR)
        {
            return finish(buffer, Field.YEAR.getRequiredLength(), null);
        }

        // MONTH
        if (lastIncluded.ordinal() >= Field.MONTH.ordinal())
        {
            buffer[4] = DATE_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getMonth(), buffer, 5, 2);
        }
        if (lastIncluded == Field.MONTH)
        {
            return finish(buffer, Field.MONTH.getRequiredLength(), null);
        }

        // DAY
        if (lastIncluded.ordinal() >= Field.DAY.ordinal())
        {
            buffer[7] = DATE_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getDayOfMonth(), buffer, 8, 2);
        }
        if (lastIncluded == Field.DAY)
        {
            return finish(buffer, Field.DAY.getRequiredLength(), null);
        }

        // HOUR
        if (lastIncluded.ordinal() >= Field.HOUR.ordinal())
        {
            buffer[10] = SEPARATOR_UPPER;
            LimitedCharArrayIntegerUtil.toString(date.getHour(), buffer, 11, 2);
        }
        if (lastIncluded == Field.HOUR)
        {
            return finish(buffer, Field.HOUR.getRequiredLength(), tz);
        }

        // MINUTE
        if (lastIncluded.ordinal() >= Field.MINUTE.ordinal())
        {
            buffer[13] = TIME_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getMinute(), buffer, 14, 2);
        }
        if (lastIncluded == Field.MINUTE)
        {
            return finish(buffer, Field.MINUTE.getRequiredLength(), tz);
        }

        // SECOND
        if (lastIncluded.ordinal() >= Field.SECOND.ordinal())
        {
            buffer[16] = TIME_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getSecond(), buffer, 17, 2);
        }
        if (lastIncluded == Field.SECOND)
        {
            return finish(buffer, Field.SECOND.getRequiredLength(), tz);
        }

        // Fractions
        if (lastIncluded.ordinal() >= Field.NANO.ordinal())
        {
            buffer[19] = '.';
            LimitedCharArrayIntegerUtil.toString(date.getNano(), buffer, 20, fractionDigits);
        }
        return finish(buffer, 20 + fractionDigits, tz);
    }

    /**
     * Return the number of significant fraction digits in the second.
     *
     * @return The number of significant fraction digits
     */
    public int getFractionDigits()
    {
        return fractionDigits;
    }

    /**
     * Formats this date-time as a date/date-time with the same fields as was parsed
     *
     * @return The formatted date/date-time string
     */
    @Override
    public String toString()
    {
        return fractionDigits > 0 ? toString(fractionDigits) : toString(field);
    }

    /**
     * * @hidden
     */
    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        DateTime dateTime = (DateTime) o;
        return year == dateTime.year
                && month == dateTime.month
                && day == dateTime.day
                && hour == dateTime.hour
                && minute == dateTime.minute
                && second == dateTime.second
                && nano == dateTime.nano
                && fractionDigits == dateTime.fractionDigits
                && field == dateTime.field
                && Objects.equals(offset, dateTime.offset);
    }

    /**
     * @hidden
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(field.ordinal(), year, month, day, hour, minute, second, nano, offset, fractionDigits);
    }
}
