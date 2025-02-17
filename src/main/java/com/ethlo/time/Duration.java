package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2025 Morten Haraldsen @ethlo
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

import java.time.Instant;
import java.util.Objects;

import com.ethlo.time.internal.util.DurationNormalizer;

public class Duration
{
    public static final int NANOS_PER_SECOND = 1_000_000_000;
    public static final long SECONDS_PER_MINUTE = 60;
    public static final long SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
    public static final long SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;
    public static final long SECONDS_PER_WEEK = 7 * SECONDS_PER_DAY;
    public static final long NANOS_PER_MICROSECOND = 1_000;
    public static final long NANOS_PER_MILLISECOND = 1_000 * NANOS_PER_MICROSECOND;
    private final long seconds;
    private final int nano;

    Duration(long seconds, int nanos)
    {
        this.seconds = seconds;
        if (nanos < 0)
        {
            throw new IllegalArgumentException("nanos cannot be negative");
        }
        else if (nanos >= 1_000_000_000)
        {
            throw new IllegalArgumentException("nanos cannot be larger than 999,999,999");
        }
        this.nano = nanos;
    }

    public static Duration ofMillis(long millis)
    {
        return ofNanos(Math.multiplyExact(NANOS_PER_MILLISECOND, millis));
    }

    public static Duration ofMicros(long millis)
    {
        return ofNanos(Math.multiplyExact(NANOS_PER_MICROSECOND, millis));
    }


    public static Duration ofNanos(long nanos)
    {
        long seconds = nanos / NANOS_PER_SECOND;
        final int remainderNanos = (int) (nanos % NANOS_PER_SECOND);
        int nano = remainderNanos;
        if (seconds < 0 || nano < 0)
        {
            seconds -= 1;
            nano = NANOS_PER_SECOND + remainderNanos;
        }
        return new Duration(seconds, nano);
    }

    public static Duration ofWeeks(long weeks)
    {
        return new Duration(Math.multiplyExact(SECONDS_PER_WEEK, weeks), 0);
    }

    public static Duration ofDays(long days)
    {
        return new Duration(Math.multiplyExact(SECONDS_PER_DAY, days), 0);
    }

    public static Duration ofHours(long hours)
    {
        return new Duration(Math.multiplyExact(SECONDS_PER_HOUR, hours), 0);
    }

    public static Duration ofMinutes(long minutes)
    {
        return new Duration(Math.multiplyExact(SECONDS_PER_MINUTE, minutes), 0);
    }

    public static Duration ofSeconds(long seconds)
    {
        return new Duration(seconds, 0);
    }

    public long getSeconds()
    {
        return seconds;
    }

    public int getNano()
    {
        return nano;
    }

    public String normalized()
    {
        return DurationNormalizer.normalizeDuration(seconds, nano);
    }

    public Instant past()
    {
        return past(Instant.now());
    }

    public Instant future()
    {
        return future(Instant.now());
    }

    public Instant past(Instant now)
    {
        return now.minusSeconds(this.seconds).minusNanos(this.nano);
    }

    public Instant future(Instant now)
    {
        return now.plusSeconds(this.seconds).plusNanos(this.nano);
    }

    java.time.Duration toDuration()
    {
        return java.time.Duration.ofSeconds(seconds, nano);
    }

    public Duration add(Duration other)
    {
        // Safely sum seconds with overflow check
        final long totalSeconds = Math.addExact(this.seconds, other.seconds);

        // Sum nanoseconds as long (avoids int overflow even though not strictly necessary here)
        final long totalNanos = (long) this.nano + (long) other.nano;

        // Calculate overflow in nanoseconds and adjust
        long overflowSeconds = totalNanos / NANOS_PER_SECOND;
        int remainderNanos = (int) (totalNanos % NANOS_PER_SECOND);

        // Safely add overflow seconds to total seconds
        final long adjustedTotalSeconds = Math.addExact(totalSeconds, overflowSeconds);

        return new Duration(adjustedTotalSeconds, remainderNanos);
    }

    public Duration subtract(Duration other)
    {
        final Duration negated = other.negate();
        return add(negated);
    }

    public Duration negate()
    {
        // Use Math.negateExact to catch overflow for the seconds.
        long negatedSeconds = Math.negateExact(this.seconds);
        int negatedNano = -this.nano;
        if (negatedNano < 0)
        {
            negatedSeconds -= 1;
            negatedNano += 1_000_000_000;
        }
        return new Duration(negatedSeconds, negatedNano);
    }

    @Override
    public boolean equals(final Object object)
    {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Duration that = (Duration) object;
        return seconds == that.seconds && nano == that.nano;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(seconds, nano);
    }

    @Override
    public String toString()
    {
        return normalized();
    }
}
