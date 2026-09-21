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
     * Days from 0000-01-01 to 1970-01-01, the epoch of {@link #daysFromCivil(int, int, int)}
     */
    public static final long DAYS_0000_TO_1970 = -daysFromCivil(0, 1, 1);

    /*
     * Days from 0000-01-01 → year, month, day after Ben Joffe's "very fast date algorithm"
     * (https://www.benjoffe.com/fast-date-64, benjoffe_fast32_v2.hpp, BSL-1.0), in the 32-bit form whose
     * products all fit 64 bits: four multiplications and no division, against the seven divisions of the era /
     * year-of-era / day-of-year chain it replaced (perf-log S8.1). The ideas, in the order the code uses them:
     *
     * - Count the days backwards from a far-future 0000-02-29-aligned point (ERAS whole 400-year eras after it),
     *   so that both the 4-year and the 400-year cycle *start* with their long member and the "(4x + 3) / n"
     *   rounding terms disappear; each division becomes one multiply and a shift.
     * - "Julian map": one mul-shift for the centuries, then rev + cen - cen / 4 turns the Gregorian count into a
     *   Julian one where every fourth year is leap, no exceptions.
     * - One signed multiply gives the year in the high word and a fixed-point year fraction in the low word; the
     *   year fraction is scaled straight to a month/day number without going through day-of-year.
     * - The 1/4-day-per-year drift that skipping day-of-year introduces is cancelled by (yrs & 3) * YRS_CYCLE,
     *   which depends only on yrs and so sits off the critical path.
     *
     * The constants are range option A of the reference (SCALE = 8, ERAS = 4331): exact for ±284 449 years around
     * 1970, checked here over the whole 0000-9999 domain by DateTimeMathTest. Signedness: every product below is
     * of a non-negative value under 2^32 with a constant under 2^32, so it fits a long without wrapping and the
     * shifts are unsigned; num is the one signed product (YRS_MUL < 0) and its shift is arithmetic on purpose.
     */
    private static final int ERAS = 4331;
    private static final int SCALE = 8;
    private static final int MSIZE = 1 << SCALE;          // month size in the fixed-point month/day number
    private static final int YRS_CYCLE = MSIZE / 32 / 4;  // a quarter of a 32-day "month" per year
    /**
     * The reference counts from 1970 ({@code 146097 * ERAS - 719469}); the 719 528 days from 0000-01-01 to
     * 1970-01-01 are folded in so that the caller's day count needs no rebase
     */
    private static final int D_SHIFT = 146_097 * ERAS - 719_469 + (int) DAYS_0000_TO_1970;
    private static final int Y_SHIFT = 400 * ERAS;
    private static final long CEN_MUL = 3_853_261_555L;   // floor(2^47 / 36524.25)
    private static final long YRS_MUL = -376_287_347L;    // -(floor(2^37 / 365.25) + 1)
    private static final long YPT_MUL = 3_056L;           // floor(MSIZE * 365.25 / 30.6) + 1
    private static final long DAY_MUL = 513_382_809L;     // floor(2^32 * 30.6 / MSIZE)
    private static final int SHIFT_1 = -2_307;            // -(590680 * MSIZE / 65536): January and February
    private static final int SHIFT_0 = SHIFT_1 + 12 * MSIZE;

    /**
     * The inverse of {@link #daysFromCivil(int, int, int)} for the years 0000-9999, taking the day count from
     * 0000-01-01 rather than 1970 so that every intermediate value is non-negative. The result is packed into one
     * int so that no object is needed to return three values. Layout: {@code year << 9 | month << 5 | day};
     * unpack with {@link #packedYear(int)}, {@link #packedMonth(int)} and {@link #packedDay(int)}.
     *
     * @param daysSince0000 days since 0000-01-01, in [0, 3652424]; the caller guarantees the range
     */
    public static int civilFromDaysSince0000(final int daysSince0000)
    {
        final int rev = D_SHIFT - daysSince0000;                        // reversed day count, [629093742, 632746166]
        final int cen = (int) ((rev * CEN_MUL) >>> 47);                 // centuries back, ≈ rev / 36524.25
        final int jul = rev - (cen >>> 2) + cen;                        // Julian map: a leap day every 4 years, always
        final long num = (jul * YRS_MUL) >> 5;                          // high word: -years back; low word: year fraction
        final int yrs = (int) (num >> 32);
        final int ypt = (int) (((num & 0xFFFF_FFFFL) * YPT_MUL) >>> 32); // year part in 1/MSIZE months, backwards
        final boolean bump = ypt > 10 * MSIZE;                          // January or February: the next calendar year
        final int shift = bump ? SHIFT_1 : SHIFT_0;
        final int mNum = (yrs & 3) * YRS_CYCLE + shift + ypt;           // forward again: month << SCALE | day fraction
        final int month = mNum >>> SCALE;                               // [1, 12]
        final int day = (int) (((mNum & (MSIZE - 1)) * DAY_MUL) >>> 32) + 1; // [1, 31]
        final int year = Y_SHIFT + yrs + (bump ? 1 : 0);
        return year << 9 | month << 5 | day;
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
