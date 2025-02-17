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

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a precise duration in seconds and nanoseconds.
 * The nanoseconds field is always positive, with the sign absorbed by the seconds field.
 */
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

    /**
     * Constructs a duration with the specified seconds and nanoseconds.
     *
     * @param seconds The number of seconds.
     * @param nanos   The number of nanoseconds, must be between 0 and 999,999,999.
     * @throws IllegalArgumentException if nanos is out of range.
     */
    Duration(long seconds, int nanos)
    {
        this.seconds = seconds;
        if (nanos < 0)
        {
            throw new IllegalArgumentException("nanos cannot be negative");
        }
        else if (nanos >= NANOS_PER_SECOND)
        {
            throw new IllegalArgumentException("nanos cannot be larger than 999,999,999");
        }
        this.nano = nanos;
    }

    /**
     * Creates a duration from milliseconds.
     *
     * @param millis The number of milliseconds.
     * @return A corresponding Duration instance.
     */
    public static Duration ofMillis(long millis)
    {
        return ofNanos(Math.multiplyExact(NANOS_PER_MILLISECOND, millis));
    }

    /**
     * Creates a duration from microseconds.
     *
     * @param micros The number of microseconds.
     * @return A corresponding Duration instance.
     */
    public static Duration ofMicros(long micros)
    {
        return ofNanos(Math.multiplyExact(NANOS_PER_MICROSECOND, micros));
    }

    /**
     * Creates a duration from nanoseconds.
     *
     * @param nanos The number of nanoseconds.
     * @return A corresponding Duration instance.
     */
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

    /**
     * Creates a duration from weeks.
     *
     * @param weeks The number of weeks.
     * @return A corresponding Duration instance.
     */
    public static Duration ofWeeks(long weeks)
    {
        return new Duration(Math.multiplyExact(SECONDS_PER_WEEK, weeks), 0);
    }

    /**
     * Creates a duration from days.
     *
     * @param days The number of days.
     * @return A corresponding Duration instance.
     */
    public static Duration ofDays(long days)
    {
        return new Duration(Math.multiplyExact(SECONDS_PER_DAY, days), 0);
    }

    /**
     * Creates a duration from hours.
     *
     * @param hours The number of hours.
     * @return A corresponding Duration instance.
     */
    public static Duration ofHours(long hours)
    {
        return new Duration(Math.multiplyExact(SECONDS_PER_HOUR, hours), 0);
    }

    /**
     * Creates a duration from minutes.
     *
     * @param minutes The number of minutes.
     * @return A corresponding Duration instance.
     */
    public static Duration ofMinutes(long minutes)
    {
        return new Duration(Math.multiplyExact(SECONDS_PER_MINUTE, minutes), 0);
    }

    /**
     * Creates a duration from seconds.
     *
     * @param seconds The number of seconds.
     * @return A corresponding Duration instance.
     */
    public static Duration ofSeconds(long seconds)
    {
        return new Duration(seconds, 0);
    }

    /**
     * Returns the number of seconds in this duration.
     *
     * @return The seconds value.
     */
    public long getSeconds()
    {
        return seconds;
    }

    /**
     * Returns the nanosecond component of this duration.
     *
     * @return The nanosecond value.
     */
    public int getNano()
    {
        return nano;
    }

    /**
     * Returns a normalized string representation of this duration.
     *
     * @return The normalized duration string.
     */
    public String normalized()
    {
        return DurationNormalizer.normalizeDuration(seconds, nano);
    }

    /**
     * Returns an {@link Instant} representing the past moment from now.
     *
     * @return The past instant.
     */
    public Instant past()
    {
        return past(Instant.now());
    }

    /**
     * Returns an {@link Instant} representing the future moment from now.
     *
     * @return The future instant.
     */
    public Instant future()
    {
        return future(Instant.now());
    }

    /**
     * Computes an {@link Instant} that represents this duration in the past from a given instant.
     *
     * @param now The reference instant.
     * @return The computed past instant.
     */
    public Instant past(Instant now)
    {
        return now.minusSeconds(this.seconds).minusNanos(this.nano);
    }

    /**
     * Computes an {@link Instant} that represents this duration in the future from a given instant.
     *
     * @param now The reference instant.
     * @return The computed future instant.
     */
    public Instant future(Instant now)
    {
        return now.plusSeconds(this.seconds).plusNanos(this.nano);
    }

    /**
     * Converts this duration to a {@link java.time.Duration}.
     *
     * @return The Java Time API equivalent duration.
     */
    java.time.Duration toDuration()
    {
        return java.time.Duration.ofSeconds(seconds, nano);
    }

    /**
     * Adds another duration to this one.
     *
     * @param other The duration to add.
     * @return A new Duration representing the sum.
     */
    public Duration add(Duration other)
    {
        final long totalSeconds = Math.addExact(this.seconds, other.seconds);
        final long totalNanos = (long) this.nano + (long) other.nano;
        long overflowSeconds = totalNanos / NANOS_PER_SECOND;
        int remainderNanos = (int) (totalNanos % NANOS_PER_SECOND);
        final long adjustedTotalSeconds = Math.addExact(totalSeconds, overflowSeconds);
        return new Duration(adjustedTotalSeconds, remainderNanos);
    }

    /**
     * Subtracts another duration from this one.
     *
     * @param other The duration to subtract.
     * @return A new Duration representing the difference.
     */
    public Duration subtract(Duration other)
    {
        return add(other.negate());
    }

    /**
     * Negates this duration.
     *
     * @return A new Duration with the opposite sign.
     */
    public Duration negate()
    {
        long negatedSeconds = Math.negateExact(this.seconds);
        int negatedNano = -this.nano;
        if (negatedNano < 0)
        {
            negatedSeconds -= 1;
            negatedNano += NANOS_PER_SECOND;
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
