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

import java.time.DateTimeException;
import java.time.OffsetDateTime;

/**
 * Holder class for parsed data. The {@link #getField()} contains the last found field, like MONTH, MINUTE, SECOND.
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
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.nano = nano;
        this.offset = offset;
    }

    public static DateTime of(int year, int month, int day, int hour, int minute, int second, int nanos, TimezoneOffset offset)
    {
        return new DateTime(Field.SECOND, year, month, day, hour, minute, second, nanos, offset);
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

    public int getYear()
    {
        return year;
    }

    public int getMonth()
    {
        return month;
    }

    public int getDay()
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

    public long getNano()
    {
        return nano;
    }

    /**
     * Returns the time offset, if available
     *
     * @return the time offset, if available
     */
    public TimezoneOffset getOffset()
    {
        return offset;
    }

    public OffsetDateTime toOffsetDatetime()
    {
        if (field == Field.SECOND && offset != null)
        {
            return OffsetDateTime.of(year, month, day, hour, minute, second, nano, offset.asJavaTimeOffset());
        }
        throw new DateTimeException("Missing resolution for date-time, found only " + field.name().toLowerCase());
    }

    public Field getField()
    {
        return field;
    }
}
