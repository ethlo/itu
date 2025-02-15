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

public class DurationNormalizer
{
    private static final long SECONDS_IN_MINUTE = 60;
    private static final long SECONDS_IN_HOUR = 3600;
    private static final long SECONDS_IN_DAY = 86400;
    private static final long SECONDS_IN_WEEK = 604800;

    public static String normalizeDuration(long seconds, int nano)
    {
        // Normalize nanoseconds into seconds if necessary
        if (nano >= 1_000_000_000)
        {
            throw new IllegalArgumentException("nano value has to be less than 1000,000,000");
        }

        final StringBuilder duration = new StringBuilder();

        if (seconds < 0)
        {
            duration.append('-');
            seconds = (seconds * -1) - 1;
        }

        duration.append('P');

        // Weeks calculation
        long weeks = seconds / SECONDS_IN_WEEK;
        if (weeks > 0)
        {
            duration.append(weeks).append("W");
            seconds %= SECONDS_IN_WEEK;
        }

        // Days calculation
        long days = seconds / SECONDS_IN_DAY;
        if (days > 0)
        {
            duration.append(days).append("D");
            seconds %= SECONDS_IN_DAY;
        }

        // Time section starts after 'T'
        if (seconds > 0)
        {
            duration.append("T");
        }

        long hours = seconds / SECONDS_IN_HOUR;
        if (hours > 0)
        {
            duration.append(hours).append("H");
            seconds %= SECONDS_IN_HOUR;
        }

        // Minutes calculation
        long minutes = seconds / SECONDS_IN_MINUTE;
        if (minutes > 0)
        {
            duration.append(minutes).append("M");
            seconds %= SECONDS_IN_MINUTE;
        }

        // Seconds and fractional seconds
        if (seconds > 0 || nano > 0)
        {
            duration.append(seconds);

            if (nano > 0)
            {
                // Efficiently append fractional part without trailing zeros
                String fractionalPart = String.format("%09d", nano);
                int endIndex = fractionalPart.length();
                while (endIndex > 0 && fractionalPart.charAt(endIndex - 1) == '0')
                {
                    endIndex--;
                }

                if (endIndex > 0)
                {
                    duration.append(".").append(fractionalPart, 0, endIndex);
                }
            }

            duration.append("S");
        }

        return duration.toString();
    }
}
