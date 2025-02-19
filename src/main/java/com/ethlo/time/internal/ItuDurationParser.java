package com.ethlo.time.internal;

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
import static com.ethlo.time.internal.fixed.ITUParser.sanityCheckInputParams;

import java.time.format.DateTimeParseException;

import com.ethlo.time.Duration;

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
    public static final int NANOS_IN_SECOND = 1_000_000_000;
    public static final char SEP_T = 'T';
    public static final char UNIT_WEEK = 'W';
    public static final char UNIT_DAY = 'D';
    public static final char UNIT_HOUR = 'H';
    public static final char UNIT_MINUTE = 'M';
    public static final char UNIT_SECOND = 'S';
    public static final char DOT = '.';
    public static final char DIGIT_ZERO = '0';
    public static final char DIGIT_NINE = '9';
    private static final int[] POW10_TABLE = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};
    private static final int MAX_DIGITS = 19;

    public static Duration parse(final String chars)
    {
        return parse(chars, 0);
    }

    public static Duration parse(final String text, final int offset)
    {
        final int availableLength = sanityCheckInputParams(text, offset);
        if (availableLength == 0)
        {
            error("Duration cannot be empty", text, text.length() - 1);
        }

        boolean negative = false;
        int index = offset;

        // Check for a leading negative sign
        if (text.charAt(offset) == '-')
        {
            negative = true;
            index++;
        }

        final DurationPartsConsumer handler = new DurationPartsConsumer(index, negative);
        final int length = text.length();
        while (index < length)
        {
            index = readUntilNonDigit(text, index, handler);
        }

        handler.validate(text, index);

        return handler.getResult();
    }

    private static int readUntilNonDigit(final String text, final int offset, final DurationPartsConsumer consumer)
    {
        int unit = 0;
        int index = offset;
        long value = 0;
        int startIndex = index;
        while (index < text.length())
        {
            final char c = text.charAt(index);
            int isDigit = (c - DIGIT_ZERO) >>> 31 | (DIGIT_NINE - c) >>> 31;
            if (isDigit != 0)
            {
                unit = c;
                break;
            }
            else
            {
                int digit = c - DIGIT_ZERO;
                value = (value << 3) + (value << 1) + digit;
                index++;
            }
        }

        if (index - startIndex > MAX_DIGITS || value > Integer.MAX_VALUE)
        {
            error("Value too large for unit '" + (char) unit + "'", text, index);
        }

        if (unit == 0)
        {
            error("No unit defined for value " + value, text, index);
        }

        final int length = index - offset;
        if (length == 0 && (unit == UNIT_WEEK || unit == UNIT_DAY || unit == UNIT_HOUR || unit == UNIT_MINUTE || unit == UNIT_SECOND || unit == DOT))
        {
            error("Zero-length value prior to unit '" + ((char) unit) + "'", text, index);
        }

        consumer.accept(text, index, length, (char) unit, (int) value);

        return index + 1;
    }

    private static void error(final String errorMessage, final String text, int index)
    {
        throw new DateTimeParseException(errorMessage + ": " + text, text, index);
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
        private int wFound;
        private int dFound;
        private int hFound;
        private int mFound;
        private int sFound;
        private boolean dotFound;
        private boolean fractionsFound;

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
                    error("Duration must start with 'P'", chars, index);
                }
                pFound = true;
                return;
            }

            if (!pFound)
            {
                error("Duration must start with 'P'", chars, index);
            }

            /*
             * An int can never overflow a long when used for seconds, minutes, hours, days, and weeks,
             * even at their maximum values, because the total number of seconds remains within the
             * 64-bit long's limits.
             */

            switch (unit)
            {
                case SEP_T:

                    if (length != 0)
                    {
                        // We have a number in front of the T
                        error("There is no unit for the number prior to the 'T'", chars, index);
                    }

                    assertNonFractional('T', chars, index);
                    if (afterT)
                    {
                        error("Only one 'T' is allowed and must precede time units", chars, index);
                    }
                    afterT = true;
                    break;

                case UNIT_WEEK:
                    assertNonFractional(UNIT_WEEK, chars, index);
                    if (wFound > 0)
                    {
                        error("'W' (week) can only appear once", chars, index);
                    }

                    if (afterT)
                    {
                        error("'W' (week) must appear before 'T' in the duration", chars, index);
                    }
                    seconds += value * 604800L; // 7 * 86400
                    wFound = index;
                    break;

                case UNIT_DAY:
                    assertNonFractional('D', chars, index);
                    if (dFound > 0)
                    {
                        error("'D' (days) can only appear once", chars, index);
                    }
                    if (afterT)
                    {
                        error("'D' (days) must appear before 'T' in the duration", chars, index);
                    }
                    seconds += value * 86400L;
                    dFound = index;
                    break;

                case UNIT_HOUR:
                    assertNonFractional('H', chars, index);
                    if (hFound > 0)
                    {
                        error("'H' (hours) can only appear once", chars, index);
                    }
                    if (!afterT)
                    {
                        error("'H' (hours) must appear after 'T' in the duration", chars, index);
                    }
                    seconds += value * 3600L;
                    hFound = index;
                    break;

                case UNIT_MINUTE:
                    assertNonFractional(UNIT_MINUTE, chars, index);
                    if (mFound > 0)
                    {
                        error("'M' (minutes) can only appear once", chars, index);
                    }
                    if (!afterT)
                    {
                        error("'M' (minutes) must appear after 'T' in the duration", chars, index);
                    }
                    seconds += value * 60L;
                    mFound = index;
                    break;

                case UNIT_SECOND:
                    if (sFound > 0)
                    {
                        error("'S' (seconds) can only appear once", chars, index);
                    }
                    if (!afterT)
                    {
                        error("'S' (seconds) must appear after 'T' in the duration", chars, index);
                    }
                    sFound = index;

                    if (readingFractionalPart)
                    {
                        if (length > 9)
                        {
                            error("Maximum allowed is 9 fraction digits", chars, index);
                        }

                        if (length == 0)
                        {
                            error("Must have at least one fraction digit after the '.''", chars, index);
                        }

                        nano = value;
                        int remainingDigits = DIGITS_IN_NANO - length;
                        if (remainingDigits > 0)
                        {
                            nano *= POW10_TABLE[remainingDigits];
                        }

                        if (negative && nano > 0)
                        {
                            seconds += 1;
                            nano = NANOS_IN_SECOND - nano;
                        }
                        fractionsFound = true;
                        readingFractionalPart = false;
                    }
                    else
                    {
                        seconds += value;
                    }
                    break;

                case DOT:
                    if (dotFound)
                    {
                        error("'.' can only appear once", chars, index);
                    }
                    if (!afterT)
                    {
                        error("Fractional seconds (.) must come after 'T'", chars, index);
                    }
                    readingFractionalPart = true;
                    seconds += value; // Assume integer part of seconds before fraction
                    dotFound = true;
                    break;

                default:
                    error("Invalid unit: " + unit, chars, index);
            }
        }

        private void assertNonFractional(final char unit, final String chars, final int index)
        {
            if (readingFractionalPart)
            {
                error("Cannot have fractional values for unit " + unit, chars, index);
            }
        }

        public void validate(String chars, int index)
        {
            if (afterT && hFound + mFound + sFound == 0)
            {
                error("Expected at least value and unit after the 'T'", chars, index);
            }

            if (dotFound && !fractionsFound)
            {
                error("Expected at least one fractional digit after the dot", chars, index);
            }

            if (fractionsFound && sFound == 0)
            {
                error("Expected 'S' after fractional number", chars, index);
            }

            if (wFound + dFound + hFound + mFound + sFound == 0)
            {
                error("Expected at least one value and unit", chars, index);
            }

            validateUnitOrder(chars);
        }

        private void validateUnitOrder(String chars)
        {
            int lastIndex = -1;
            lastIndex = verifyUnitIndex(wFound, lastIndex, chars);
            lastIndex = verifyUnitIndex(dFound, lastIndex, chars);
            lastIndex = verifyUnitIndex(hFound, lastIndex, chars);
            lastIndex = verifyUnitIndex(mFound, lastIndex, chars);
            lastIndex = verifyUnitIndex(sFound, lastIndex, chars);
        }

        private int verifyUnitIndex(final int unitIndex, final int lastIndex, final String chars)
        {
            if (unitIndex > 0)
            {
                if (unitIndex < lastIndex)
                {
                    error("Units must be in order from largest to smallest", chars, unitIndex);
                }
                return unitIndex;
            }
            return lastIndex;
        }

        public Duration getResult()
        {
            // IMPORTANT: ISO 8601 does not allow negative fractional values separately from the seconds.
            // Instead, nanoseconds should always be positive, and seconds should absorb the negative sign.
            return Duration.of(negative ? -seconds : seconds, nano);
        }
    }
}
