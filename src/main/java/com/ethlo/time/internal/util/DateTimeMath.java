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
 * Calendar arithmetic. Both directions follow Ben Joffe's algorithms, see CREDITS.md (the ports are ITU's own code,
 * https://github.com/ethlo/itu/issues/63); the credit for the shape of the problem goes to
 * <a href="https://howardhinnant.github.io/date_algorithms.html">Howard Hinnant's public domain date algorithms</a>,
 * which this class used until perf-log S8/S9.
 */
public class DateTimeMath
{
    /*
     * Civil date → days since 1970-01-01, the inverse of civilFromDaysSince0000, after the to_rata_die of
     * benjoffe_fast32_v2.hpp (https://www.benjoffe.com/fast-date#inverse, perf-log S9.1). The year is counted from
     * March so that the leap day is the last day of the year; the days of the whole years are
     * 365 * y + y / 4 - y / 100 + y / 400, written as yrs / 4 - cen + cen / 4 with cen = yrs / 100 so that there is
     * one division by 100 and the rest are shifts; and the days of the whole months, the 0 31 61 92 122 153 184
     * 214 245 275 306 337 sequence from March, are (979 * month + shift) / 32, a linear fit that rounds down to the
     * right value for every month, with shift selecting January/February (12 months on) from the rest. Hinnant's
     * form did the same with four long magic divisions (400, 5, 4, 100).
     *
     * Everything is int and non-negative: the year is rebased by one 400-year era so that year 0 in
     * January/February (yrs = -1) does not go below zero, and the era's 146 097 days are taken back out in
     * EPOCH_SHIFT. Valid for years [0, 5 000 000]; DateTimeMathTest checks every day of 0000-9999 against
     * LocalDate.
     */
    private static final int MONTH_SHIFT_MAR = -2_919;
    private static final int MONTH_SHIFT_JAN = 8_829;
    /**
     * The days the formula yields for 1970-01-01 (with the extra era), so that this date is day 0
     */
    private static final int EPOCH_SHIFT = 865_566;

    public static long daysFromCivil(final int year, final int month, final int day)
    {
        final boolean bump = month <= 2;
        final int yrs = year + 400 - (bump ? 1 : 0);
        final int cen = yrs / 100;
        final int yearDays = yrs * 365 + (yrs >>> 2) - cen + (cen >>> 2);
        final int monthDays = (979 * month + (bump ? MONTH_SHIFT_JAN : MONTH_SHIFT_MAR)) >>> 5;
        return (long) yearDays + monthDays + day - EPOCH_SHIFT;
    }

    /**
     * Days from 0000-01-01 to 1970-01-01, the epoch of {@link #daysFromCivil(int, int, int)}
     */
    public static final long DAYS_0000_TO_1970 = -daysFromCivil(0, 1, 1);

    /*
     * Days from 0000-01-01 → year, month, day after Ben Joffe's "very fast date algorithm"
     * (https://www.benjoffe.com/fast-date-64), ported from benjoffe_fast32_v2.hpp in
     * https://github.com/benjoffe/fast-date-benchmarks (see CREDITS.md). It is the 32-bit form whose products all
     * fit 64 bits: four multiplications and no division, against the seven divisions of the era / year-of-era /
     * day-of-year chain it replaced (perf-log S8.1). The ideas, in the order the code uses them:
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

    /*
     * Second of day → hour, minute, second after Ben Joffe's "fast time-of-day" V2
     * (https://www.benjoffe.com/fast-time-of-day, perf-log S8.2; see CREDITS.md). The three fields come from two independent
     * products of the same input, so the chain is two multiplies deep instead of three divisions in series: the
     * hour is the high word of secondOfDay * H_MUL, and the low word is the fraction of the hour elapsed, which
     * times 60 gives the minute in its high word; the second is the same from the low word of secondOfDay * M_MUL.
     * The "+ 1" in each constant rounds the product up so that the truncated high word is exact for inputs up to
     * 2 255 818 seconds; ours is under 86 400. The three accessors share their products: C2 value-numbers the
     * identical expressions once they are inlined into the same caller.
     */
    private static final long H_MUL = (1L << 32) / 3_600 + 1;  // 1193047
    private static final long M_MUL = (1L << 32) / 60 + 1;     // 71582789

    /**
     * @param secondOfDay seconds since midnight, in [0, 86399]
     */
    public static int hourOfDay(final int secondOfDay)
    {
        return (int) ((secondOfDay * H_MUL) >>> 32);
    }

    /**
     * @param secondOfDay seconds since midnight, in [0, 86399]
     */
    public static int minuteOfHour(final int secondOfDay)
    {
        return (int) (((secondOfDay * H_MUL & 0xFFFF_FFFFL) * 60) >>> 32);
    }

    /**
     * @param secondOfDay seconds since midnight, in [0, 86399]
     */
    public static int secondOfMinute(final int secondOfDay)
    {
        return (int) (((secondOfDay * M_MUL & 0xFFFF_FFFFL) * 60) >>> 32);
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
