package com.ethlo.time.internal.util;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2024 Morten Haraldsen (ethlo)
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

/**
 * CREDIT: <a href="https://howardhinnant.github.io/date_algorithms.html">Public domain math for converting between epoch and date-time</a>
 */
public class DateTimeMath
{
    public static long daysFromCivil(int y, final int m, final int d)
    {
        // Returns number of days since civil 1970-01-01.  Negative values indicate
        //    days prior to 1970-01-01.
        // Preconditions:  y-m-d represents a date in the civil (Gregorian) calendar
        //                 m is in [1, 12]
        //                 d is in [1, last_day_of_month(y, m)]
        //                 y is "approximately" in
        //                   [numeric_limits<Int>::min()/366, numeric_limits<Int>::max()/366]
        //                 Exact range of validity is:
        //                 [civil_from_days(numeric_limits<Int>::min()),
        //                  civil_from_days(numeric_limits<Int>::max()-719468)]
        y -= m <= 2 ? 1 : 0;
        final long era = (y >= 0 ? y : y - 399) / 400;
        final long yoe = y - era * 400;      // [0, 399]
        final long doy = (153L * (m > 2 ? m - 3 : m + 9) + 2) / 5 + d - 1;  // [0, 365]
        final long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;         // [0, 146096]
        return era * 146097 + doe - 719468;
    }

    /**
     * The inverse of {@link #daysFromCivil(int, int, int)}: the civil date of a day count since 1970-01-01, packed
     * into one int so that no object is needed to return three values. Layout: {@code year << 9 | month << 5 | day};
     * unpack with {@link #packedYear(int)}, {@link #packedMonth(int)} and {@link #packedDay(int)}.
     * <p>
     * The caller must keep {@code days} within years [0, 9999]: the packing has no room for a sign.
     */
    public static int civilFromDays(final long days)
    {
        final long z = days + 719468;
        final long era = (z >= 0 ? z : z - 146096) / 146097;
        final long doe = z - era * 146097;                                  // [0, 146096]
        final long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;  // [0, 399]
        final long y = yoe + era * 400;
        final long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);          // [0, 365]
        final long mp = (5 * doy + 2) / 153;                                // [0, 11]
        final int d = (int) (doy - (153 * mp + 2) / 5 + 1);                // [1, 31]
        final int m = (int) (mp < 10 ? mp + 3 : mp - 9);                    // [1, 12]
        final int year = (int) (m <= 2 ? y + 1 : y);
        return year << 9 | m << 5 | d;
    }

    public static int packedYear(final int packed)
    {
        return packed >>> 9;
    }

    public static int packedMonth(final int packed)
    {
        return (packed >>> 5) & 0xF;
    }

    public static int packedDay(final int packed)
    {
        return packed & 0x1F;
    }
}
