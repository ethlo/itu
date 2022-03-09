package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2022 Morten Haraldsen (ethlo)
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
import java.util.Optional;

import com.ethlo.time.internal.LimitedCharArrayIntegerUtil;

/**
 * Container class for parsed date/date-time data. The {@link #getField()} contains the highest granularity field found, like MONTH, MINUTE, SECOND.
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

    public DateTime(final Field field, final int year, final int month, final int day, final int hour, final int minute, final int second, final int nano, final TimezoneOffset offset)
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
    }

    public static DateTime of(int year, int month, int day, int hour, int minute, int second, TimezoneOffset offset)
    {
        return new DateTime(Field.SECOND, year, month, day, hour, minute, second, 0, offset);
    }

    public static DateTime of(int year, int month, int day, int hour, int minute, int second, int nanos, TimezoneOffset offset)
    {
        return new DateTime(Field.NANO, year, month, day, hour, minute, second, nanos, offset);
    }

    public static DateTime ofYear(int year)
    {
        return new DateTime(Field.YEAR, year, 0, 0, 0, 0, 0, 0, null);
    }

    public static DateTime ofYearMonth(int years, int months)
    {
        return new DateTime(Field.MONTH, years, months, 0, 0, 0, 0, 0, null);
    }

    public static DateTime ofDate(int years, int months, int days)
    {
        return new DateTime(Field.DAY, years, months, days, 0, 0, 0, 0, null);
    }

    public static DateTime of(int years, int months, int days, int hours, int minute, TimezoneOffset offset)
    {
        return new DateTime(Field.MINUTE, years, months, days, hours, minute, 0, 0, offset);
    }

    public static DateTime of(OffsetDateTime dateTime)
    {
        return DateTime.of(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getNano(), TimezoneOffset.of(dateTime.getOffset()));
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
     * Creates a {@link Year} discarding any higher resolution fields
     *
     * @return the {@link Year}
     */
    public Year toYear()
    {
        return Year.of(year);
    }

    /**
     * Creates a {@link YearMonth} discarding any higher resolution fields
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
        if (offset != null)
        {
            return OffsetDateTime.of(year, month, day, hour, minute, second, nano, offset.asJavaTimeOffset());
        }
        throw new DateTimeException("No zone offset information found");
    }

    /**
     * Creates a {@link LocalDate}, discarding any higher resolution fields
     *
     * @return the {@link LocalDate}
     */
    public LocalDate toLocalDate()
    {
        assertMinGranularity(Field.DAY);
        return LocalDate.of(year, month, day);
    }

    /**
     * Returns the minimum field found during parsing
     *
     * @return The minimum field found
     */
    public Field getField()
    {
        return field;
    }

    public DateTime assertMinGranularity(Field field)
    {
        if (!includesGranularity(field))
        {
            throw new DateTimeException("No " + field.name() + " field found");
        }
        return this;
    }

    public String toString(final Field lastIncluded)
    {
        return toString(this, lastIncluded, 0);
    }

    public String toString(final int fractionDigits)
    {
        return toString(this, Field.NANO, fractionDigits);
    }

    private String toString(final DateTime date, final Field lastIncluded, final int fractionDigits)
    {
        if (lastIncluded.ordinal() > date.getField().ordinal())
        {
            throw new DateTimeException("Requested granularity was " + lastIncluded.name() + ", but contains only granularity " + date.getField().name());
        }
        final boolean hasTz = date.getOffset().isPresent();
        final char[] buffer = new char[32];

        // YEAR
        LimitedCharArrayIntegerUtil.toString(date.getYear(), buffer, 0, 4);
        if (lastIncluded == Field.YEAR)
        {
            return finish(buffer, Field.YEAR.getRequiredLength(), false);
        }

        // MONTH
        if (lastIncluded.ordinal() >= Field.MONTH.ordinal())
        {
            buffer[4] = DATE_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getMonth(), buffer, 5, 2);
        }
        if (lastIncluded == Field.MONTH)
        {
            return finish(buffer, Field.MONTH.getRequiredLength(), false);
        }

        // DAY
        if (lastIncluded.ordinal() >= Field.DAY.ordinal())
        {
            buffer[7] = DATE_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getDayOfMonth(), buffer, 8, 2);
        }
        if (lastIncluded == Field.DAY)
        {
            return finish(buffer, Field.DAY.getRequiredLength(), false);
        }

        // HOUR
        if (lastIncluded.ordinal() >= Field.HOUR.ordinal())
        {
            buffer[10] = SEPARATOR_UPPER;
            LimitedCharArrayIntegerUtil.toString(date.getHour(), buffer, 11, 2);
        }
        if (lastIncluded == Field.HOUR)
        {
            return finish(buffer, Field.HOUR.getRequiredLength(), hasTz);
        }

        // MINUTE
        if (lastIncluded.ordinal() >= Field.MINUTE.ordinal())
        {
            buffer[13] = TIME_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getMinute(), buffer, 14, 2);
        }
        if (lastIncluded == Field.MINUTE)
        {
            return finish(buffer, Field.MINUTE.getRequiredLength(), hasTz);
        }

        // SECOND
        if (lastIncluded.ordinal() >= Field.SECOND.ordinal())
        {
            buffer[16] = TIME_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getSecond(), buffer, 17, 2);
        }
        if (lastIncluded == Field.SECOND)
        {
            return finish(buffer, Field.SECOND.getRequiredLength(), hasTz);
        }

        // Fractions
        if (lastIncluded.ordinal() >= Field.NANO.ordinal())
        {
            buffer[19] = '.';
            LimitedCharArrayIntegerUtil.toString(date.getNano(), buffer, 20, fractionDigits);
        }
        return finish(buffer, 20 + fractionDigits, hasTz);
    }

}
