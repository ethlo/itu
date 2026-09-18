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
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Container class for timezone offset, denoted by hours and minutes
 */
public class TimezoneOffset
{
    public static final TimezoneOffset UTC = new TimezoneOffset(0, 0);
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MAX_OFFSET_HOURS = 18;
    private static final int MAX_OFFSET_SECONDS = MAX_OFFSET_HOURS * SECONDS_PER_HOUR;
    private final int hours;
    private final int minutes;

    private TimezoneOffset(final int hours, final int minutes)
    {
        this.hours = hours;
        this.minutes = minutes;
    }

    /**
     * Creates an offset from hours and minutes. The two must carry the same sign, and the resulting offset
     * must be within the range -18:00 to +18:00, matching the constraints of {@link ZoneOffset}.
     *
     * @param hours   The hour part of the offset
     * @param minutes The minute part of the offset, carrying the same sign as the hour part
     * @return The offset
     * @throws DateTimeException if the offset is not in the valid range
     */
    public static TimezoneOffset ofHoursMinutes(int hours, int minutes)
    {
        validate(hours, minutes);
        return new TimezoneOffset(hours, minutes);
    }

    private static void validate(final int hours, final int minutes)
    {
        // NOTE: The messages below intentionally mirror those of java.time.ZoneOffset
        if (hours < -MAX_OFFSET_HOURS || hours > MAX_OFFSET_HOURS)
        {
            throw new DateTimeException("Zone offset hours not in valid range: value " + hours + " is not in the range -" + MAX_OFFSET_HOURS + " to " + MAX_OFFSET_HOURS);
        }
        if (hours > 0 && minutes < 0)
        {
            throw new DateTimeException("Zone offset minutes and seconds must be positive because hours is positive");
        }
        if (hours < 0 && minutes > 0)
        {
            throw new DateTimeException("Zone offset minutes and seconds must be negative because hours is negative");
        }
        if (minutes < -59 || minutes > 59)
        {
            throw new DateTimeException("Zone offset minutes not in valid range: value " + minutes + " is not in the range -59 to 59");
        }
        if (Math.abs(hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE) > MAX_OFFSET_SECONDS)
        {
            throw new DateTimeException("Zone offset not in valid range: -18:00 to +18:00");
        }
    }

    /**
     * Creates an offset from a total number of seconds. The offset must be a whole number of minutes,
     * as sub-minute offsets cannot be represented in RFC-3339.
     *
     * @param seconds The total number of seconds of the offset
     * @return The offset
     * @throws DateTimeException if the offset is not a whole number of minutes, or is out of range
     */
    public static TimezoneOffset ofTotalSeconds(int seconds)
    {
        if (seconds == 0)
        {
            return UTC;
        }
        if (seconds % SECONDS_PER_MINUTE != 0)
        {
            throw new DateTimeException("Zone offset must be a whole number of minutes to be representable: " + seconds + " seconds");
        }
        final int absHours = seconds / SECONDS_PER_HOUR;
        final int absMinutes = (seconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;
        return ofHoursMinutes(absHours, absMinutes);
    }

    public static TimezoneOffset of(ZoneOffset offset)
    {
        return ofTotalSeconds(offset.getTotalSeconds());
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

    public ZoneOffset toZoneOffset()
    {
        if (this.equals(UTC))
        {
            return ZoneOffset.UTC;
        }
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
        return "TimezoneOffset{" + "hours=" + hours + ", minutes=" + minutes + '}';
    }

    /**
     * The number of characters this offset occupies in its textual form.
     * <p>
     * NOTE: The identity comparison against {@link #UTC} is deliberate. The parser hands back this exact
     * constant only for the single-character <code>Z</code>/<code>z</code> form, while an explicit
     * <code>+00:00</code> yields an equal-but-distinct instance that occupies six characters. Using
     * {@link #equals(Object)} here would under-report the consumed length for <code>+00:00</code> and
     * corrupt {@link DateTime#getParseLength()}.
     *
     * @return 1 for the <code>Z</code> form, otherwise 6
     */
    public int getRequiredLength()
    {
        return this == UTC ? 1 : 6;
    }
}
