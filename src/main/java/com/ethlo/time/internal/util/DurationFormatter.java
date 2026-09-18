package com.ethlo.time.internal.util;

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

import static com.ethlo.time.internal.ItuDurationParser.NANOS_IN_SECOND;

import java.util.Objects;

import com.ethlo.time.Duration;
import com.ethlo.time.DurationUnit;

public class DurationFormatter
{
    private static final int NANO_DIGITS = 9;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int SECONDS_IN_HOUR = 3600;
    private static final int SECONDS_IN_DAY = 86400;
    private static final long SECONDS_IN_WEEK = 604800;

    /**
     * The longest output this formatter can produce is 39 characters, reached by
     * <code>-P15250284452471W3DT15H29M59.999999999S</code> (seconds = -9223372036854775800, nanos = 1):
     * sign, 'P', 14 week digits + 'W', 1 day digit + 'D', 'T', 2 + 'H', 2 + 'M', 2, '.' + 9 fraction
     * digits, 'S'.
     * <p>
     * NOTE: This is also the bound across every {@link DurationUnit} cap. A lower cap gives the leading unit
     * more digits, but always removes more designators and component digits than it adds. Asserted by
     * <code>DurationUnitTest.noCapExceedsTheWeeksFormLength</code>.
     */
    private static final int MAX_LENGTH = 40;

    public static String normalizeDuration(final Duration duration)
    {
        return normalizeDuration(duration, DurationUnit.WEEKS);
    }

    public static String normalizeDuration(final Duration duration, final DurationUnit maxUnit)
    {
        Objects.requireNonNull(maxUnit, "maxUnit cannot be null");

        long seconds = duration.getSeconds();
        final int nanos = duration.getNanos();

        if (seconds == 0 && nanos == 0)
        {
            return "PT0S";
        }

        // NOTE: Rendered into a fixed-size buffer rather than a StringBuilder, as the upper bound on the
        // output length is known up front. That avoids the builder allocation and its capacity growth.
        final char[] buf = new char[MAX_LENGTH];
        int pos = 0;

        final boolean negative = seconds < 0;

        if (negative)
        {
            buf[pos++] = '-';
            // NOTE: With a fraction the magnitude is |seconds| - 1, which is -(seconds + 1). Computed in that
            // order so that seconds == Long.MIN_VALUE does not overflow before the subtraction
            seconds = nanos > 0 ? -(seconds + 1) : Math.negateExact(seconds);
        }

        buf[pos++] = 'P';

        long weeks = 0;
        long days = 0;
        long hours = 0;
        long minutes = 0;

        // NOTE: Enters at the requested cap and falls through the smaller units, so everything above the cap
        // stays folded into the largest unit that is allowed to carry it. Only the unit entered at can exceed
        // its usual range, as each later step is fed a remainder already below the unit above it.
        switch (maxUnit)
        {
            case WEEKS:
                weeks = seconds / SECONDS_IN_WEEK;
                seconds -= weeks * SECONDS_IN_WEEK;
                // fall through
            case DAYS:
                days = seconds / SECONDS_IN_DAY;
                seconds -= days * SECONDS_IN_DAY;
                // fall through
            case HOURS:
                hours = seconds / SECONDS_IN_HOUR;
                seconds -= hours * SECONDS_IN_HOUR;
                // fall through
            case MINUTES:
                minutes = seconds / SECONDS_IN_MINUTE;
                seconds -= minutes * SECONDS_IN_MINUTE;
                // fall through
            default:
                break;
        }

        // Date section
        if (weeks > 0)
        {
            pos = appendValue(buf, pos, weeks);
            buf[pos++] = 'W';
        }

        if (days > 0)
        {
            pos = appendValue(buf, pos, days);
            buf[pos++] = 'D';
        }

        // Time section starts after 'T'
        if (hours > 0 || minutes > 0 || seconds > 0 || nanos > 0)
        {
            buf[pos++] = 'T';
        }

        if (hours > 0)
        {
            pos = appendValue(buf, pos, hours);
            buf[pos++] = 'H';
        }

        if (minutes > 0)
        {
            pos = appendValue(buf, pos, minutes);
            buf[pos++] = 'M';
        }

        // Seconds and fractional seconds
        if (seconds > 0 || nanos > 0)
        {
            pos = appendValue(buf, pos, seconds);

            if (nanos > 0)
            {
                pos = appendFraction(buf, pos, negative ? NANOS_IN_SECOND - nanos : nanos);
            }

            buf[pos++] = 'S';
        }

        return new String(buf, 0, pos);
    }

    /**
     * Appends the nanosecond value as a fractional part, without trailing zeros.
     * <p>
     * NOTE: Written out by hand rather than via <code>String.format("%09d", ..)</code>, which formats using
     * the default locale. Under a locale with a non-Latin default numbering system that produced digits this
     * library's own parser cannot read back.
     *
     * @param buf  The buffer to render into
     * @param pos  The index to place the decimal separator at
     * @param nano The nanosecond value, 1 - 999,999,999
     * @return The index just past the last character written
     */
    private static int appendFraction(final char[] buf, final int pos, final int nano)
    {
        // NOTE: Milli- and microsecond precision are by far the most common, so peel off those whole
        // groups of trailing zeros up front instead of dividing all nine digits out one at a time
        int value = nano;
        int digits = NANO_DIGITS;
        if (value % 1_000_000 == 0)
        {
            value /= 1_000_000;
            digits = 3;
        }
        else if (value % 1_000 == 0)
        {
            value /= 1_000;
            digits = 6;
        }

        buf[pos] = '.';

        // Right-aligned, so that a value narrower than the group is zero-padded on the left
        int end = pos + 1 + digits;
        for (int i = end - 1; i > pos; i--)
        {
            buf[i] = (char) ('0' + (value % 10));
            value /= 10;
        }

        while (buf[end - 1] == '0')
        {
            end--;
        }

        return end;
    }

    /**
     * Appends a positive component value.
     *
     * @param buf   The buffer to render into
     * @param pos   The index to write from
     * @param value The value to append, must be positive
     * @return The index just past the last character written
     */
    private static int appendValue(final char[] buf, final int pos, final long value)
    {
        // NOTE: Every component except the one the cap was applied at is below 100, so the common case is
        // worth splitting out rather than running the general loop with its 64-bit divisions and reversal
        if (value < 100)
        {
            return appendUpToTwoDigits(buf, pos, (int) value);
        }
        return appendLong(buf, pos, value);
    }

    /**
     * Appends a value known to be in the range 0 - 99, without leading zeros.
     *
     * @param buf   The buffer to render into
     * @param pos   The index to write from
     * @param value The value to append
     * @return The index just past the last character written
     */
    private static int appendUpToTwoDigits(final char[] buf, int pos, final int value)
    {
        if (value >= 10)
        {
            buf[pos++] = (char) ('0' + value / 10);
        }
        buf[pos++] = (char) ('0' + value % 10);
        return pos;
    }

    /**
     * Appends a positive value of arbitrary magnitude.
     *
     * @param buf   The buffer to render into
     * @param pos   The index to write from
     * @param value The value to append, must be positive
     * @return The index just past the last character written
     */
    private static int appendLong(final char[] buf, final int pos, long value)
    {
        int end = pos;
        do
        {
            buf[end++] = (char) ('0' + (int) (value % 10));
            value /= 10;
        }
        while (value != 0);

        // The digits land least-significant first, so flip the range we just wrote
        for (int i = pos, j = end - 1; i < j; i++, j--)
        {
            final char tmp = buf[i];
            buf[i] = buf[j];
            buf[j] = tmp;
        }

        return end;
    }
}
