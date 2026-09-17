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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.ethlo.time.Field;
import com.ethlo.time.TimezoneOffset;
import com.ethlo.time.internal.DateTimeFormatException;
import com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil;

public class ITUFormatter
{
    public static final int MIN_YEAR = 0;
    public static final int MAX_YEAR = 9999;

    public static String finish(final char[] buf, final int length, final TimezoneOffset tz)
    {
        int tzLen = 0;
        if (tz != null)
        {
            tzLen = writeTz(buf, length, tz);
        }
        return new String(buf, 0, length + tzLen);
    }

    private static int writeTz(final char[] buf, final int start, final TimezoneOffset tz)
    {
        if (tz.equals(TimezoneOffset.UTC))
        {
            buf[start] = ZULU_UPPER;
            return 1;
        }
        else
        {
            buf[start] = tz.getTotalSeconds() < 0 ? MINUS : PLUS;
            LimitedCharArrayIntegerUtil.toString(Math.abs(tz.getHours()), buf, start + 1, 2);
            buf[start + 3] = TIME_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(Math.abs(tz.getMinutes()), buf, start + 4, 2);
            return 6;
        }
    }

    public static String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        return doFormat(date, ZoneOffset.UTC, Field.SECOND, fractionDigits);
    }

    public static String formatUtc(OffsetDateTime date, Field lastIncluded)
    {
        return doFormat(date, ZoneOffset.UTC, lastIncluded, 0);
    }

    public static String format(OffsetDateTime date, ZoneOffset adjustTo, final int fractionDigits)
    {
        return doFormat(date, adjustTo, Field.NANO, fractionDigits);
    }

    private static String doFormat(OffsetDateTime date, ZoneOffset adjustTo, Field lastIncluded, int fractionDigits)
    {
        assertFractionDigits(fractionDigits);

        OffsetDateTime adjusted = date;
        if (!date.getOffset().equals(adjustTo))
        {
            adjusted = date.atZoneSameInstant(adjustTo).toOffsetDateTime();
        }
        final TimezoneOffset tz = TimezoneOffset.of(adjustTo);

        final char[] buffer = new char[26 + fractionDigits];

        assertYearRange(adjusted.getYear());

        if (handleDatePart(lastIncluded, buffer, adjusted.getYear(), 0, 4, Field.YEAR))
        {
            return finish(buffer, Field.YEAR.getRequiredLength(), null);
        }

        buffer[4] = DATE_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, adjusted.getMonthValue(), 5, 2, Field.MONTH))
        {
            return finish(buffer, Field.MONTH.getRequiredLength(), null);
        }

        buffer[7] = DATE_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, adjusted.getDayOfMonth(), 8, 2, Field.DAY))
        {
            return finish(buffer, Field.DAY.getRequiredLength(), null);
        }

        // T separator
        buffer[10] = SEPARATOR_UPPER;

        // Time
        LimitedCharArrayIntegerUtil.toString(adjusted.getHour(), buffer, 11, 2);
        buffer[13] = TIME_SEPARATOR;
        if (handleDatePart(lastIncluded, buffer, adjusted.getMinute(), 14, 2, Field.MINUTE))
        {
            return finish(buffer, Field.MINUTE.getRequiredLength(), tz);
        }
        buffer[16] = TIME_SEPARATOR;
        LimitedCharArrayIntegerUtil.toString(adjusted.getSecond(), buffer, 17, 2);

        // Second fractions
        final boolean hasFractionDigits = fractionDigits > 0;
        if (hasFractionDigits)
        {
            buffer[19] = FRACTION_SEPARATOR;
            addFractions(buffer, fractionDigits, adjusted.getNano());
            return finish(buffer, 20 + fractionDigits, tz);
        }
        return finish(buffer, 19, tz);
    }

    private static boolean handleDatePart(final Field lastIncluded, final char[] buffer, final int value, final int offset, final int length, final Field field)
    {
        LimitedCharArrayIntegerUtil.toString(value, buffer, offset, length);
        return lastIncluded == field;
    }

    private static void addFractions(char[] buf, int fractionDigits, int nano)
    {
        LimitedCharArrayIntegerUtil.toString(LimitedCharArrayIntegerUtil.scaleNanos(nano, fractionDigits), buf, 20, fractionDigits);
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
