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

import static com.ethlo.time.internal.fixed.ITUParser.DIGITS_IN_NANO;
import static com.ethlo.time.internal.fixed.ITUParser.RADIX;
import static com.ethlo.time.internal.fixed.ITUParser.sanityCheckInputParams;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.DIGIT_9;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.ZERO;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.ethlo.time.internal.util.DurationNormalizer;

/**
 * <b>Rationale Against Supporting Years and Months</b>
 * <p>
 * Supporting years (Y) and months (M) in duration calculations is problematic due to:
 * <p>
 * Variable Lengths: Months have different lengths (28-31 days), and years may be leap or non-leap (365 vs. 366 days). This makes durations ambiguous unless referenced to a specific start date.
 * <p>
 * Context-Dependent Interpretation: P1M could mean 28, 29, 30, or 31 days depending on the month in which it is applied.
 * <p>
 * Difficult Arithmetic: Operations like addition and comparison require anchoring to a specific date, making them non-trivial in purely arithmetic computations.
 * <p>
 * Consistency Issues: Excluding years and months ensures durations are always exact and unambiguous, aligning with precise time-based measurements.
 */
public class ItuDurationParser
{
    private static final int[] POW10_TABLE = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    public static DurationData parse(final String chars)
    {
        return parse(chars, 0);
    }

    public static DurationData parse(final String chars, final int offset)
    {
        sanityCheckInputParams(chars, offset);

        boolean negative = false;
        int index = offset;

        // Check for a leading negative sign
        if (chars.charAt(offset) == '-')
        {
            negative = true;
            index++;
        }

        final DurationPartsConsumer handler = new DurationPartsConsumer(index, negative);
        final int length = chars.length();
        while (index < length)
        {
            index = readUntilNonDigit(chars, index, handler);
        }

        return handler.getResult();
    }

    private static int readUntilNonDigit(final String chars, final int offset, final DurationPartsConsumer consumer)
    {
        int unit = 0;
        int idx = offset;
        int value = 0;
        final int len = chars.length();
        while (idx < len)
        {
            final char c = chars.charAt(idx);
            if (c < ZERO || c > DIGIT_9)
            {
                unit = c;
                break;
            }
            else
            {
                value = value * RADIX + (c - ZERO);
                idx++;
            }
        }

        if (unit == 0)
        {
            throw new DateTimeParseException("No unit defined for value " + value, chars, idx);
        }

        consumer.accept(chars, idx, idx - offset, (char) unit, value);

        return idx + 1;
    }

    public static class DurationData
    {
        private final long seconds;
        private final int nano;

        DurationData(long seconds, int nano)
        {
            this.seconds = seconds;
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

        public String toNormalized()
        {
            return DurationNormalizer.normalizeDuration(seconds, nano);
        }

        public Duration toDuration()
        {
            return Duration.ofSeconds(seconds, nano);
        }

        @Override
        public boolean equals(final Object object)
        {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            DurationData that = (DurationData) object;
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
            return toNormalized();
        }
    }

    private static class DurationPartsConsumer
    {
        private final int startOffset;
        private final boolean negative;
        private long seconds;
        private int nano;
        private boolean readingFractionalPart;
        private boolean afterT;
        private boolean pFound;

        private DurationPartsConsumer(final int startOffset, boolean negative)
        {
            this.startOffset = startOffset;
            this.negative = negative;
        }

        public final void accept(final String chars, final int index, final int length, final char unit, final int value)
        {
            final int relIndex = index - startOffset;

            // Check for initial 'P' (must be at the start)
            if (relIndex == 0)
            {
                if (unit != 'P')
                {
                    throw new DateTimeParseException("Duration must start with 'P'", chars, 0);
                }
                pFound = true;
                return;
            }

            if (!pFound)
            {
                throw new DateTimeParseException("Duration must start with 'P'", chars, 0);
            }

            switch (unit)
            {
                case 'T':
                    if (afterT)
                    {
                        throw new DateTimeParseException("Only one 'T' is allowed and must precede time units", chars, index);
                    }
                    afterT = true;
                    break;

                case 'W':
                    seconds += value * 604800L; // 7 * 86400
                    break;

                case 'D':
                    if (afterT)
                    {
                        throw new DateTimeParseException("'D' (days) must appear before 'T' in the duration", chars, index);
                    }
                    seconds += value * 86400L;
                    break;

                case 'H':
                    if (!afterT)
                    {
                        throw new DateTimeParseException("'H' (hours) must appear after 'T' in the duration", chars, index);
                    }
                    seconds += value * 3600L;
                    break;

                case 'M':
                    if (!afterT)
                    {
                        throw new DateTimeParseException("'M' (minutes) must appear after 'T' in the duration", chars, index);
                    }
                    seconds += value * 60L;
                    break;

                case 'S':
                    if (!afterT)
                    {
                        throw new DateTimeParseException("'S' (seconds) must appear after 'T' in the duration", chars, index);
                    }

                    if (readingFractionalPart)
                    {
                        nano = value;
                        int remainingDigits = DIGITS_IN_NANO - length;
                        if (remainingDigits > 0)
                        {
                            nano *= POW10_TABLE[remainingDigits]; // Use lookup instead of loop
                        }
                        readingFractionalPart = false;
                    }
                    else
                    {
                        seconds += value;
                    }
                    break;

                case '.':
                    if (!afterT)
                    {
                        throw new DateTimeParseException("Fractional seconds (.) must come after 'T'", chars, index);
                    }
                    readingFractionalPart = true;
                    seconds += value; // Assume integer part of seconds before fraction
                    break;

                default:
                    throw new DateTimeParseException("Invalid unit: " + unit, chars, index);
            }
        }

        public DurationData getResult()
        {
            // IMPORTANT: ISO 8601 does not allow negative fractional values separately from the seconds.
            // Instead, nanoseconds should always be positive, and seconds should absorb the negative sign.
            return new DurationData(negative ? (-seconds - 1) : seconds, nano);
        }
    }
}
