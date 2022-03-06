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

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class TimezoneOffset
{
    public static final TimezoneOffset UTC = new TimezoneOffset(0, 0);
    private final int hours;
    private final int minutes;

    private TimezoneOffset(final int hours, final int minutes)
    {
        this.hours = hours;
        this.minutes = minutes;
    }

    public static TimezoneOffset ofHoursMinutes(int hours, int minutes)
    {
        return new TimezoneOffset(hours, minutes);
    }

    public static TimezoneOffset of(ZoneOffset offset)
    {
        final Duration d = Duration.ofSeconds(offset.getTotalSeconds());
        return TimezoneOffset.ofHoursMinutes((int) d.get(ChronoUnit.HOURS), (int) d.get(ChronoUnit.MINUTES));
    }

    public int getHours()
    {
        return hours;
    }

    public int getMinutes()
    {
        return minutes;
    }

    public int getTotalSeconds()
    {
        return hours * 60 * 60 + minutes * 60;
    }

    public ZoneOffset asJavaTimeOffset()
    {
        return ZoneOffset.ofHoursMinutes(hours, minutes);
    }

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
        TimezoneOffset that = (TimezoneOffset) o;
        return hours == that.hours && minutes == that.minutes;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(hours, minutes);
    }

    @Override
    public String toString()
    {
        return "TimezoneOffset{" +
                "hours=" + hours +
                ", minutes=" + minutes +
                '}';
    }
}
