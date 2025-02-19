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
import java.util.Comparator;
import java.util.Objects;

import com.ethlo.time.internal.util.DurationFormatter;

/**
 * Represents a precise duration in seconds and nanoseconds.
 * The nanoseconds field is always positive, with the sign absorbed by the seconds field.
 */
public class Duration implements Comparable<Duration>
{
    public static final Duration ZERO = new Duration(0, 0);

    public static final int NANOS_PER_SECOND = 1_000_000_000;
    public static final long SECONDS_PER_MINUTE = 60;
    public static final long SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
    public static final long SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;
    public static final long SECONDS_PER_WEEK = 7 * SECONDS_PER_DAY;
    public static final long NANOS_PER_MICROSECOND = 1_000;
    public static final long NANOS_PER_MILLISECOND = 1_000 * NANOS_PER_MICROSECOND;

    private final long seconds;
    private final int nanos;

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
        this.nanos = nanos;
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

    public static Duration of(long seconds, int nanos)
    {
        return new Duration(seconds, nanos);
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
    public int getNanos()
    {
        return nanos;
    }

    /**
     * Returns a normalized string representation of this duration.
     *
     * @return The normalized duration string.
     */
    public String normalized()
    {
        return DurationFormatter.normalizeDuration(this);
    }

    /**
     * Computes an {@link Instant} that represents this duration on the timeline from now
     *
     * @return The computed instant representing the point on the timeline
     */
    public Instant timeline()
    {
        return timeline(Instant.now());
    }

    /**
     * Computes an {@link Instant} that represents this duration on the timeline from a given instant.
     *
     * @param instant The reference instant.
     * @return The computed instant representing the point on the timeline
     */
    public Instant timeline(Instant instant)
    {
        return instant.plusSeconds(this.seconds).plusNanos(this.nanos);
    }

    /**
     * Converts this duration to a {@link java.time.Duration}.
     *
     * @return The Java Time API equivalent duration.
     */
    java.time.Duration toDuration()
    {
        return java.time.Duration.ofSeconds(seconds, nanos);
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
        final long totalNanos = (long) this.nanos + (long) other.nanos;
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
        int negatedNano = -this.nanos;
        if (negatedNano < 0)
        {
            negatedSeconds -= 1;
            negatedNano += NANOS_PER_SECOND;
        }
        return new Duration(negatedSeconds, negatedNano);
    }

    /**
     * Returns the absolute duration
     *
     * @return the absolute duration
     */
    public Duration abs()
    {
        if (isNegative())
        {
            return negate();
        }
        return this;
    }

    private boolean isNegative()
    {
        return seconds < 0;
    }

    @Override
    public boolean equals(final Object object)
    {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Duration that = (Duration) object;
        return seconds == that.seconds && nanos == that.nanos;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(seconds, nanos);
    }

    @Override
    public String toString()
    {
        return normalized();
    }

    @Override
    public int compareTo(final Duration o)
    {
        return Comparator.comparingLong(Duration::getSeconds)
                .thenComparingInt(Duration::getNanos)
                .compare(this, o);
    }

    public Duration plusHours(final long hours)
    {
        return plusSeconds(Math.multiplyExact(hours, SECONDS_PER_HOUR));
    }

    public Duration plusMinutes(final long minutes)
    {
        return plusSeconds(Math.multiplyExact(minutes, SECONDS_PER_MINUTE));
    }

    public Duration plusSeconds(long seconds)
    {
        return new Duration(Math.addExact(this.seconds, seconds), nanos);
    }

    public Duration plusNanos(long nanos)
    {
        final long nanosTotal = Math.addExact(this.nanos, nanos);
        final int nanosRemainder = Math.toIntExact(nanosTotal % NANOS_PER_SECOND);
        final long extraSecs = nanosTotal / NANOS_PER_SECOND;
        return new Duration(Math.addExact(seconds, extraSecs), nanosRemainder);
    }
}
