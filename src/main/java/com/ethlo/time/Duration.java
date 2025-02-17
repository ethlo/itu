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
    private final long seconds;
    private final int nano;

    public Duration(long seconds, int nano)
    {
        this.seconds = seconds;
        if (nano < 0)
        {
            throw new IllegalArgumentException("nano cannot be negative");
        }
        else if (nano >= 1000_000_000)
        {
            throw new IllegalArgumentException("nano cannot be larger than 999,999,999");
        }
        this.nano = nano;
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

        // Ensure remainder is non-negative
        if (remainderNanos < 0)
        {
            remainderNanos += NANOS_PER_SECOND;
            overflowSeconds -= 1; // Adjust overflow seconds due to remainder correction
        }

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
