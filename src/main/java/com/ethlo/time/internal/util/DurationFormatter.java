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

import com.ethlo.time.Duration;

public class DurationFormatter
{
    private static final int NANO_DIGITS = 9;
    private static final long SECONDS_IN_MINUTE = 60;
    private static final long SECONDS_IN_HOUR = 3600;
    private static final long SECONDS_IN_DAY = 86400;
    private static final long SECONDS_IN_WEEK = 604800;

    public static String normalizeDuration(Duration duration)
    {
        long seconds = duration.getSeconds();
        int nanos = duration.getNanos();

        if (seconds == 0 && nanos == 0)
        {
            return "PT0S";
        }

        final StringBuilder s = new StringBuilder();

        final boolean negative = seconds < 0;

        if (negative)
        {
            s.append('-');
            // NOTE: With a fraction the magnitude is |seconds| - 1, which is -(seconds + 1). Computed in that
            // order so that seconds == Long.MIN_VALUE does not overflow before the subtraction
            seconds = nanos > 0 ? -(seconds + 1) : Math.negateExact(seconds);
        }

        s.append('P');

        // Weeks calculation
        long weeks = seconds / SECONDS_IN_WEEK;
        if (weeks > 0)
        {
            s.append(weeks).append("W");
            seconds %= SECONDS_IN_WEEK;
        }

        // Days calculation
        long days = seconds / SECONDS_IN_DAY;
        if (days > 0)
        {
            s.append(days).append("D");
            seconds %= SECONDS_IN_DAY;
        }

        // Time section starts after 'T'
        if (seconds > 0 || nanos > 0)
        {
            s.append("T");
        }

        long hours = seconds / SECONDS_IN_HOUR;
        if (hours > 0)
        {
            s.append(hours).append("H");
            seconds %= SECONDS_IN_HOUR;
        }

        // Minutes calculation
        long minutes = seconds / SECONDS_IN_MINUTE;
        if (minutes > 0)
        {
            s.append(minutes).append("M");
            seconds %= SECONDS_IN_MINUTE;
        }

        // Seconds and fractional seconds
        if (seconds > 0 || nanos > 0)
        {
            s.append(seconds);

            if (nanos > 0)
            {
                appendFraction(s, negative ? NANOS_IN_SECOND - nanos : nanos);
            }

            s.append("S");
        }

        return s.toString();
    }

    /**
     * Appends the nanosecond value as a fractional part, without trailing zeros.
     * <p>
     * NOTE: Written out by hand rather than via <code>String.format("%09d", ..)</code>, which formats using
     * the default locale. Under a locale with a non-Latin default numbering system that produced digits this
     * library's own parser cannot read back.
     *
     * @param s    The builder to append to
     * @param nano The nanosecond value, 1 - 999,999,999
     */
    private static void appendFraction(final StringBuilder s, final int nano)
    {
        final char[] digits = new char[NANO_DIGITS];
        int value = nano;
        for (int i = NANO_DIGITS - 1; i >= 0; i--)
        {
            digits[i] = (char) ('0' + (value % 10));
            value /= 10;
        }

        int endIndex = NANO_DIGITS;
        while (endIndex > 0 && digits[endIndex - 1] == '0')
        {
            endIndex--;
        }

        if (endIndex > 0)
        {
            s.append('.').append(digits, 0, endIndex);
        }
    }
}
