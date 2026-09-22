package com.ethlo.time.internal.fixed;

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

import static com.ethlo.time.internal.fixed.ITUParser.DATE_SEPARATOR;
import static com.ethlo.time.internal.fixed.ITUParser.FRACTION_SEPARATOR;
import static com.ethlo.time.internal.fixed.ITUParser.MAX_FRACTION_DIGITS;
import static com.ethlo.time.internal.fixed.ITUParser.MINUS;
import static com.ethlo.time.internal.fixed.ITUParser.PLUS;
import static com.ethlo.time.internal.fixed.ITUParser.SEPARATOR_UPPER;
import static com.ethlo.time.internal.fixed.ITUParser.TIME_SEPARATOR;
import static com.ethlo.time.internal.fixed.ITUParser.ZULU_UPPER;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.ethlo.time.internal.DateTimeFormatException;
import com.ethlo.time.Field;
import com.ethlo.time.TimezoneOffset;
import com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil;

/**
 * RFC-3339 formatting of an {@link OffsetDateTime}. One writer into a {@code char[]} and its mirror into a
 * {@code byte[]} (Latin-1; the output is ASCII); the {@code String} methods allocate a scratch {@code char[]},
 * run the writer, and wrap it. Every field comes from {@code java.time} and is in range by construction, so the
 * digits are written unchecked from a pair table (perf-log S10.1).
 * <p>
 * The two writers are kept line for line in step; {@code FormatterTest} runs them differentially.
 */
public class ITUFormatter
{
    public static final int MIN_YEAR = 0;
    public static final int MAX_YEAR = 9999;

    /**
     * The longest output: 19 for the date-time, 1 + 9 for the fraction, 6 for a {@code ±HH:MM} offset
     */
    public static final int MAX_LENGTH = 35;

    private ITUFormatter()
    {
    }

    public static String finish(final char[] buf, final int length, final TimezoneOffset tz)
    {
        int tzLen = 0;
        if (tz != null)
        {
            tzLen = writeTz(buf, length, tz.getTotalSeconds());
        }
        // char[] on purpose: String(char[], int, int) compresses to Latin-1 with a SIMD intrinsic, which for 20-35
        // chars is cheaper than the Charset constructor over a byte[] (perf-log S10.2, dead end)
        return new String(buf, 0, length + tzLen);
    }

    public static String formatUtc(final OffsetDateTime date, final int fractionDigits)
    {
        return toString(date, ZoneOffset.UTC, Field.SECOND, fractionDigits);
    }

    public static String formatUtc(final OffsetDateTime date, final Field lastIncluded)
    {
        return toString(date, ZoneOffset.UTC, lastIncluded, 0);
    }

    public static String format(final OffsetDateTime date, final ZoneOffset adjustTo, final int fractionDigits)
    {
        return toString(date, adjustTo, Field.NANO, fractionDigits);
    }

    private static String toString(final OffsetDateTime date, final ZoneOffset adjustTo, final Field lastIncluded, final int fractionDigits)
    {
        final char[] buffer = new char[MAX_LENGTH];
        final int length;
        switch (lastIncluded)
        {
            case YEAR:
            case MONTH:
            case DAY:
            case MINUTE:
                length = writeUpTo(date, adjustTo, lastIncluded, buffer);
                break;
            default:
                // HOUR has always rendered the full time, as SECOND does
                length = write(date, adjustTo, fractionDigits, buffer, 0);
        }
        return new String(buffer, 0, length);
    }

    private static OffsetDateTime prepare(final OffsetDateTime date, final ZoneOffset adjustTo, final int fractionDigits, final int capacity, final int offset)
    {
        assertFractionDigits(fractionDigits);
        if (offset < 0 || offset + MAX_LENGTH > capacity)
        {
            throw new IndexOutOfBoundsException("The buffer must have room for " + MAX_LENGTH + " characters from offset " + offset + ", has " + (capacity - offset));
        }
        final int totalSeconds = adjustTo.getTotalSeconds();
        if (totalSeconds != 0 && totalSeconds % 60 != 0)
        {
            // RFC-3339 has ±HH:MM only; truncating would shift the instant by up to 59 seconds (CorrectnessRegressionTest)
            throw new DateTimeException("Zone offset must be a whole number of minutes to be representable: " + totalSeconds + " seconds");
        }
        final OffsetDateTime adjusted = date.getOffset().equals(adjustTo) ? date : date.atZoneSameInstant(adjustTo).toOffsetDateTime();
        assertYearRange(adjusted.getYear());
        return adjusted;
    }

    /**
     * Writes the date-time in the given offset into {@code dst} from {@code offset}, with the time down to the
     * second, a fraction of {@code fractionDigits} digits when that is above zero, and the offset as {@code Z} or
     * {@code ±HH:MM}. {@code [offset, offset + MAX_LENGTH)} is the writer's window; nothing outside it is touched.
     * <p>
     * This is the hot path, kept small on purpose: no granularity branches, so that it stays under C2's
     * inlining limits and the caller sees its constants folded (perf-log S10.5).
     *
     * @return the number of characters written, at most {@link #MAX_LENGTH}
     * @throws IndexOutOfBoundsException if the buffer cannot hold the longest possible output from {@code offset}
     */
    public static int write(final OffsetDateTime date, final ZoneOffset adjustTo, final int fractionDigits, final char[] dst, final int offset)
    {
        final OffsetDateTime adjusted = prepare(date, adjustTo, fractionDigits, dst.length, offset);
        LimitedCharArrayIntegerUtil.write4(dst, offset, adjusted.getYear());
        dst[offset + 4] = DATE_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 5, adjusted.getMonthValue());
        dst[offset + 7] = DATE_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 8, adjusted.getDayOfMonth());
        dst[offset + 10] = SEPARATOR_UPPER;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 11, adjusted.getHour());
        dst[offset + 13] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 14, adjusted.getMinute());
        dst[offset + 16] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 17, adjusted.getSecond());
        int end = offset + 19;
        if (fractionDigits > 0)
        {
            dst[end] = FRACTION_SEPARATOR;
            LimitedCharArrayIntegerUtil.writeFraction(dst, end + 1, adjusted.getNano(), fractionDigits);
            end += 1 + fractionDigits;
        }
        return end + writeTz(dst, end, adjustTo.getTotalSeconds()) - offset;
    }

    /**
     * {@link #write(OffsetDateTime, ZoneOffset, int, char[], int)} into a {@code byte[]}. Mirror of the char[]
     * writer; keep the two in step.
     */
    public static int write(final OffsetDateTime date, final ZoneOffset adjustTo, final int fractionDigits, final byte[] dst, final int offset)
    {
        final OffsetDateTime adjusted = prepare(date, adjustTo, fractionDigits, dst.length, offset);
        LimitedCharArrayIntegerUtil.write4(dst, offset, adjusted.getYear());
        dst[offset + 4] = DATE_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 5, adjusted.getMonthValue());
        dst[offset + 7] = DATE_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 8, adjusted.getDayOfMonth());
        dst[offset + 10] = SEPARATOR_UPPER;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 11, adjusted.getHour());
        dst[offset + 13] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 14, adjusted.getMinute());
        dst[offset + 16] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, offset + 17, adjusted.getSecond());
        int end = offset + 19;
        if (fractionDigits > 0)
        {
            dst[end] = FRACTION_SEPARATOR;
            LimitedCharArrayIntegerUtil.writeFraction(dst, end + 1, adjusted.getNano(), fractionDigits);
            end += 1 + fractionDigits;
        }
        return end + writeTz(dst, end, adjustTo.getTotalSeconds()) - offset;
    }

    /**
     * The granularity-limited form for {@code YEAR}, {@code MONTH}, {@code DAY} and {@code MINUTE}: the cold path
     * behind {@link #formatUtc(OffsetDateTime, Field)}, kept out of {@link #write} so that the hot writer has no
     * granularity branches. The other fields go through {@link #write}.
     */
    private static int writeUpTo(final OffsetDateTime date, final ZoneOffset adjustTo, final Field lastIncluded, final char[] dst)
    {
        final OffsetDateTime adjusted = prepare(date, adjustTo, 0, dst.length, 0);
        LimitedCharArrayIntegerUtil.write4(dst, 0, adjusted.getYear());
        if (lastIncluded == Field.YEAR)
        {
            return Field.YEAR.getRequiredLength();
        }
        dst[4] = DATE_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, 5, adjusted.getMonthValue());
        if (lastIncluded == Field.MONTH)
        {
            return Field.MONTH.getRequiredLength();
        }
        dst[7] = DATE_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, 8, adjusted.getDayOfMonth());
        if (lastIncluded == Field.DAY)
        {
            return Field.DAY.getRequiredLength();
        }
        dst[10] = SEPARATOR_UPPER;
        LimitedCharArrayIntegerUtil.write2(dst, 11, adjusted.getHour());
        dst[13] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(dst, 14, adjusted.getMinute());
        return 16 + writeTz(dst, 16, adjustTo.getTotalSeconds());
    }

    /**
     * @return the number of characters written: 1 for {@code Z}, 6 for {@code ±HH:MM}
     */
    private static int writeTz(final char[] buf, final int start, final int totalSeconds)
    {
        if (totalSeconds == 0)
        {
            buf[start] = ZULU_UPPER;
            return 1;
        }
        final int abs = Math.abs(totalSeconds);
        buf[start] = totalSeconds < 0 ? MINUS : PLUS;
        LimitedCharArrayIntegerUtil.write2(buf, start + 1, abs / 3600);
        buf[start + 3] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(buf, start + 4, abs / 60 % 60);
        return 6;
    }

    private static int writeTz(final byte[] buf, final int start, final int totalSeconds)
    {
        if (totalSeconds == 0)
        {
            buf[start] = ZULU_UPPER;
            return 1;
        }
        final int abs = Math.abs(totalSeconds);
        buf[start] = (byte) (totalSeconds < 0 ? MINUS : PLUS);
        LimitedCharArrayIntegerUtil.write2(buf, start + 1, abs / 3600);
        buf[start + 3] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.write2(buf, start + 4, abs / 60 % 60);
        return 6;
    }

    /**
     * RFC-3339 has a fixed four-digit year, so anything outside 0000-9999 cannot be represented.
     *
     * @param year The year to check
     * @throws DateTimeFormatException if the year cannot be represented
     */
    public static void assertYearRange(final int year)
    {
        if (year < MIN_YEAR || year > MAX_YEAR)
        {
            throw new DateTimeFormatException("The year must be in the range " + MIN_YEAR + " to " + MAX_YEAR + " to be representable, got " + year);
        }
    }

    public static void assertFractionDigits(final int fractionDigits)
    {
        if (fractionDigits < 0 || fractionDigits > MAX_FRACTION_DIGITS)
        {
            throw new DateTimeFormatException("Maximum supported number of fraction digits in second is 9, got " + fractionDigits);
        }
    }
}
