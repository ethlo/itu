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

/**
 * ISO-8601 rendering of a {@link Duration}: one writer into a {@code char[]} and its mirror into a
 * {@code byte[]}; {@code normalizeDuration} allocates a scratch {@code char[]}, runs the writer and wraps it.
 * The two writers are kept line for line in step; {@code DurationTest} runs them differentially.
 */
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
    public static final int MAX_LENGTH = 40;

    private DurationFormatter()
    {
    }

    public static String normalizeDuration(final Duration duration)
    {
        return normalizeDuration(duration, DurationUnit.WEEKS);
    }

    public static String normalizeDuration(final Duration duration, final DurationUnit maxUnit)
    {
        if (duration.getSeconds() == 0 && duration.getNanos() == 0)
        {
            return "PT0S";
        }
        // NOTE: Rendered into a fixed-size buffer rather than a StringBuilder, as the upper bound on the
        // output length is known up front. That avoids the builder allocation and its capacity growth.
        final char[] buf = new char[MAX_LENGTH];
        final int length = write(duration, maxUnit, buf, 0);
        return new String(buf, 0, length);
    }

    private static void check(final DurationUnit maxUnit, final int capacity, final int offset)
    {
        Objects.requireNonNull(maxUnit, "maxUnit cannot be null");
        if (offset < 0 || offset + MAX_LENGTH > capacity)
        {
            throw new IndexOutOfBoundsException("The buffer must have room for " + MAX_LENGTH + " characters from offset " + offset + ", has " + (capacity - offset));
        }
    }

    /**
     * Writes the normalized form into {@code dst} from {@code offset}. {@code [offset, offset + MAX_LENGTH)} is
     * the writer's window (the fraction is written in full and then cut, so positions past the returned length
     * may hold scratch); nothing outside it is touched.
     *
     * @return the number of characters written, at most {@link #MAX_LENGTH}
     * @throws IndexOutOfBoundsException if the buffer cannot hold the longest possible output from {@code offset}
     */
    public static int write(final Duration duration, final DurationUnit maxUnit, final char[] dst, final int offset)
    {
        check(maxUnit, dst.length, offset);
        long seconds = duration.getSeconds();
        final int nanos = duration.getNanos();
        if (seconds == 0 && nanos == 0)
        {
            dst[offset] = 'P';
            dst[offset + 1] = 'T';
            dst[offset + 2] = '0';
            dst[offset + 3] = 'S';
            return 4;
        }
        int pos = offset;
        final boolean negative = seconds < 0;
        if (negative)
        {
            dst[pos++] = '-';
            // NOTE: With a fraction the magnitude is |seconds| - 1, which is -(seconds + 1). Computed in that
            // order so that seconds == Long.MIN_VALUE does not overflow before the subtraction
            seconds = nanos > 0 ? -(seconds + 1) : Math.negateExact(seconds);
        }
        dst[pos++] = 'P';

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
        return writeComponents(dst, pos, weeks, days, hours, minutes, seconds, nanos, negative) - offset;
    }

    /**
     * The output half of {@link #write}, split off so that both halves stay under C2's hot-inlining size
     * (perf-log S10.5: at 447 bytecodes the whole writer was "hot method too big" and became a call)
     */
    private static int writeComponents(final char[] dst, int pos, final long weeks, final long days, final long hours, final long minutes, final long seconds, final int nanos, final boolean negative)
    {
        if (weeks > 0)
        {
            pos = appendValue(dst, pos, weeks);
            dst[pos++] = 'W';
        }
        if (days > 0)
        {
            pos = appendValue(dst, pos, days);
            dst[pos++] = 'D';
        }
        if (hours > 0 || minutes > 0 || seconds > 0 || nanos > 0)
        {
            dst[pos++] = 'T';
        }
        if (hours > 0)
        {
            pos = appendValue(dst, pos, hours);
            dst[pos++] = 'H';
        }
        if (minutes > 0)
        {
            pos = appendValue(dst, pos, minutes);
            dst[pos++] = 'M';
        }
        if (seconds > 0 || nanos > 0)
        {
            pos = appendValue(dst, pos, seconds);
            if (nanos > 0)
            {
                pos = appendFraction(dst, pos, negative ? NANOS_IN_SECOND - nanos : nanos);
            }
            dst[pos++] = 'S';
        }
        return pos;
    }

    // Mirror of the char[] writer above; keep the two in step
    public static int write(final Duration duration, final DurationUnit maxUnit, final byte[] dst, final int offset)
    {
        check(maxUnit, dst.length, offset);
        long seconds = duration.getSeconds();
        final int nanos = duration.getNanos();
        if (seconds == 0 && nanos == 0)
        {
            dst[offset] = 'P';
            dst[offset + 1] = 'T';
            dst[offset + 2] = '0';
            dst[offset + 3] = 'S';
            return 4;
        }
        int pos = offset;
        final boolean negative = seconds < 0;
        if (negative)
        {
            dst[pos++] = '-';
            seconds = nanos > 0 ? -(seconds + 1) : Math.negateExact(seconds);
        }
        dst[pos++] = 'P';

        long weeks = 0;
        long days = 0;
        long hours = 0;
        long minutes = 0;
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
        return writeComponents(dst, pos, weeks, days, hours, minutes, seconds, nanos, negative) - offset;
    }

    /**
     * The output half of {@link #write}, split off so that both halves stay under C2's hot-inlining size
     * (perf-log S10.5: at 447 bytecodes the whole writer was "hot method too big" and became a call)
     */
    private static int writeComponents(final byte[] dst, int pos, final long weeks, final long days, final long hours, final long minutes, final long seconds, final int nanos, final boolean negative)
    {
        if (weeks > 0)
        {
            pos = appendValue(dst, pos, weeks);
            dst[pos++] = 'W';
        }
        if (days > 0)
        {
            pos = appendValue(dst, pos, days);
            dst[pos++] = 'D';
        }
        if (hours > 0 || minutes > 0 || seconds > 0 || nanos > 0)
        {
            dst[pos++] = 'T';
        }
        if (hours > 0)
        {
            pos = appendValue(dst, pos, hours);
            dst[pos++] = 'H';
        }
        if (minutes > 0)
        {
            pos = appendValue(dst, pos, minutes);
            dst[pos++] = 'M';
        }
        if (seconds > 0 || nanos > 0)
        {
            pos = appendValue(dst, pos, seconds);
            if (nanos > 0)
            {
                pos = appendFraction(dst, pos, negative ? NANOS_IN_SECOND - nanos : nanos);
            }
            dst[pos++] = 'S';
        }
        return pos;
    }

    /**
     * Appends the nanosecond value as a fractional part, without trailing zeros: all nine digits in three groups
     * of three, one division per group, then the trailing zeros are cut (perf-log S10.4). Written by hand rather
     * than via {@code String.format("%09d", ..)}, which formats using the default locale; under a locale with a
     * non-Latin default numbering system that produced digits this library's own parser cannot read back.
     *
     * @param nano The nanosecond value, 1 - 999,999,999
     * @return The index just past the last character written
     */
    private static int appendFraction(final char[] buf, final int pos, final int nano)
    {
        buf[pos] = '.';
        LimitedCharArrayIntegerUtil.writeFraction(buf, pos + 1, nano, NANO_DIGITS);
        int end = pos + 1 + NANO_DIGITS;
        while (buf[end - 1] == '0')
        {
            end--;
        }
        return end;
    }

    private static int appendFraction(final byte[] buf, final int pos, final int nano)
    {
        buf[pos] = '.';
        LimitedCharArrayIntegerUtil.writeFraction(buf, pos + 1, nano, NANO_DIGITS);
        int end = pos + 1 + NANO_DIGITS;
        while (buf[end - 1] == '0')
        {
            end--;
        }
        return end;
    }

    /**
     * Appends a positive component value. Every component except the one the cap was applied at is below 100,
     * so that case is split out rather than running the general loop with its 64-bit divisions and reversal.
     */
    private static int appendValue(final char[] buf, final int pos, final long value)
    {
        if (value < 100)
        {
            return appendUpToTwoDigits(buf, pos, (int) value);
        }
        return appendLong(buf, pos, value);
    }

    private static int appendValue(final byte[] buf, final int pos, final long value)
    {
        if (value < 100)
        {
            return appendUpToTwoDigits(buf, pos, (int) value);
        }
        return appendLong(buf, pos, value);
    }

    /**
     * Appends a value known to be in the range 0 - 99, without leading zeros.
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

    private static int appendUpToTwoDigits(final byte[] buf, int pos, final int value)
    {
        if (value >= 10)
        {
            buf[pos++] = (byte) ('0' + value / 10);
        }
        buf[pos++] = (byte) ('0' + value % 10);
        return pos;
    }

    /**
     * Appends a positive value of arbitrary magnitude: the digits land least-significant first, then the range
     * is reversed.
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
        for (int i = pos, j = end - 1; i < j; i++, j--)
        {
            final char tmp = buf[i];
            buf[i] = buf[j];
            buf[j] = tmp;
        }
        return end;
    }

    private static int appendLong(final byte[] buf, final int pos, long value)
    {
        int end = pos;
        do
        {
            buf[end++] = (byte) ('0' + (int) (value % 10));
            value /= 10;
        }
        while (value != 0);
        for (int i = pos, j = end - 1; i < j; i++, j--)
        {
            final byte tmp = buf[i];
            buf[i] = buf[j];
            buf[j] = tmp;
        }
        return end;
    }
}
