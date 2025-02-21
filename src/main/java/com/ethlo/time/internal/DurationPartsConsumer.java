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

import static com.ethlo.time.Duration.SECONDS_PER_DAY;
import static com.ethlo.time.Duration.SECONDS_PER_HOUR;
import static com.ethlo.time.Duration.SECONDS_PER_MINUTE;
import static com.ethlo.time.Duration.SECONDS_PER_WEEK;
import static com.ethlo.time.internal.fixed.ITUParser.DIGITS_IN_NANO;

import java.time.format.DateTimeParseException;

import com.ethlo.time.Duration;

class DurationPartsConsumer
{
    private static final int[] POW10_TABLE = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

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

    DurationPartsConsumer(final int startOffset, boolean negative)
    {
        this.startOffset = startOffset;
        this.negative = negative;
    }

    protected static void error(final String errorMessage, final String text, int index)
    {
        throw new DateTimeParseException(errorMessage + ": " + text, text, index);
    }

    public final void accept(final String text, final int index, final int length, final char unit, final long value)
    {
        final int relIndex = index - startOffset;

        // Check for initial 'P' (must be at the start)
        if (relIndex == 0)
        {
            if (unit != 'P')
            {
                error("Duration must start with 'P'", text, index);
            }
            pFound = true;
            return;
        }

        if (!pFound)
        {
            error("Duration must start with 'P'", text, index);
        }

        if (unit == ItuDurationParser.UNIT_UNDEFINED)
        {
            if (!dotFound)
            {
                error("No unit defined for value " + value, text, index);
            }
            error("No unit defined for value " + (seconds % SECONDS_PER_MINUTE) + ItuDurationParser.DOT + value, text, index);
        }

        if (length == 0 && (unit == ItuDurationParser.UNIT_WEEK || unit == ItuDurationParser.UNIT_DAY || unit == ItuDurationParser.UNIT_HOUR || unit == ItuDurationParser.UNIT_MINUTE || unit == ItuDurationParser.UNIT_SECOND || unit == ItuDurationParser.DOT))
        {
            error("Zero-length value prior to unit '" + unit + "'", text, index);
        }

        switch (unit)
        {
            case ItuDurationParser.SEP_T:

                if (length != 0)
                {
                    // We have a number in front of the T
                    error("There is no unit for the number prior to the 'T'", text, index);
                }

                assertNonFractional('T', text, index);
                if (afterT)
                {
                    error("Only one 'T' is allowed and must precede time units", text, index);
                }
                afterT = true;
                break;

            case ItuDurationParser.UNIT_WEEK:
                assertNonFractional(ItuDurationParser.UNIT_WEEK, text, index);
                if (wFound > 0)
                {
                    error("'W' (week) can only appear once", text, index);
                }

                if (afterT)
                {
                    error("'W' (week) must appear before 'T' in the duration", text, index);
                }
                seconds = Math.addExact(seconds, Math.multiplyExact(value, SECONDS_PER_WEEK));
                wFound = index;
                break;

            case ItuDurationParser.UNIT_DAY:
                assertNonFractional(ItuDurationParser.UNIT_DAY, text, index);
                if (dFound > 0)
                {
                    error("'D' (days) can only appear once", text, index);
                }
                if (afterT)
                {
                    error("'D' (days) must appear before 'T' in the duration", text, index);
                }
                seconds = Math.addExact(seconds, Math.multiplyExact(value, SECONDS_PER_DAY));
                dFound = index;
                break;

            case ItuDurationParser.UNIT_HOUR:
                assertNonFractional(ItuDurationParser.UNIT_HOUR, text, index);
                if (hFound > 0)
                {
                    error("'H' (hours) can only appear once", text, index);
                }
                if (!afterT)
                {
                    error("'H' (hours) must appear after 'T' in the duration", text, index);
                }
                seconds = Math.addExact(seconds, Math.multiplyExact(value, SECONDS_PER_HOUR));
                hFound = index;
                break;

            case ItuDurationParser.UNIT_MINUTE:
                assertNonFractional(ItuDurationParser.UNIT_MINUTE, text, index);
                if (mFound > 0)
                {
                    error("'M' (minutes) can only appear once", text, index);
                }
                if (!afterT)
                {
                    error("'M' (minutes) must appear after 'T' in the duration", text, index);
                }
                seconds = Math.addExact(seconds, Math.multiplyExact(value, SECONDS_PER_MINUTE));
                mFound = index;
                break;

            case ItuDurationParser.UNIT_SECOND:
                if (sFound > 0)
                {
                    error("'S' (seconds) can only appear once", text, index);
                }
                if (!afterT)
                {
                    error("'S' (seconds) must appear after 'T' in the duration", text, index);
                }
                sFound = index;

                if (readingFractionalPart)
                {
                    if (length > 9)
                    {
                        error("Maximum allowed is 9 fraction digits", text, index);
                    }

                    nano = Math.toIntExact(value);
                    int remainingDigits = DIGITS_IN_NANO - length;
                    if (remainingDigits > 0)
                    {
                        nano *= POW10_TABLE[remainingDigits];
                    }

                    if (negative && nano > 0)
                    {
                        seconds += 1;
                        nano = ItuDurationParser.NANOS_IN_SECOND - nano;
                    }
                    fractionsFound = true;
                    readingFractionalPart = false;
                }
                else
                {
                    seconds += value;
                }
                break;

            case ItuDurationParser.DOT:
                if (dotFound)
                {
                    error("'.' can only appear once", text, index);
                }
                if (!afterT)
                {
                    error("Fractional seconds (.) must come after 'T'", text, index);
                }
                readingFractionalPart = true;
                seconds += value; // Assume integer part of seconds before fraction
                dotFound = true;
                break;

            default:
                error("Invalid unit: " + unit, text, index);
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
            error("Expected at least one value and unit after the 'T'", chars, index);
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
