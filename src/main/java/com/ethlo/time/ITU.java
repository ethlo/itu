package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2018 Morten Haraldsen (ethlo)
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.Date;

public class ITU
{
    private static final EthloITU delegate = new EthloITU();
    private static final ZoneId GMT_ZONE = ZoneOffset.UTC;

    private ITU()
    {
    }

    public static OffsetDateTime parseDateTime(String s)
    {
        return delegate.parseDateTime(s);
    }

    public static String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        return delegate.formatUtc(date, fractionDigits);
    }

    public static String formatUtc(OffsetDateTime date, Field lastIncluded, int fractionDigits)
    {
        return delegate.formatUtc(date, lastIncluded, fractionDigits);
    }

    public static String formatUtc(Date date)
    {
        return delegate.formatUtc(date);
    }

    public static String format(Date date, String timezone)
    {
        return delegate.format(date, timezone);
    }

    public static boolean isValid(String dateTime)
    {
        return delegate.isValid(dateTime);
    }

    public static String formatUtcMilli(OffsetDateTime date)
    {
        return delegate.formatUtcMilli(date);
    }

    public static String formatUtcMicro(OffsetDateTime date)
    {
        return delegate.formatUtcMicro(date);
    }

    public static String formatUtcNano(OffsetDateTime date)
    {
        return delegate.formatUtcNano(date);
    }

    public static String formatUtc(OffsetDateTime date)
    {
        return delegate.formatUtc(date);
    }

    public static String formatUtcMilli(Date date)
    {
        return delegate.formatUtcMilli(date);
    }

    public static String format(Date date, String timezone, int fractionDigits)
    {
        return delegate.format(date, timezone, fractionDigits);
    }

    public static Temporal parseLenient(String s)
    {
        return delegate.parseLenient(s);
    }

    public static <T extends Temporal> T parseLenient(String s, Class<T> type)
    {
        return delegate.parseLenient(s, type);
    }

    public static long toEpochMillis(Temporal temporal)
    {
        if (temporal instanceof Instant)
        {
            return ((Instant) temporal).toEpochMilli();
        }
        else if (temporal instanceof OffsetDateTime)
        {
            return toEpochMillis(((OffsetDateTime) temporal).toInstant());
        }
        else if (temporal instanceof LocalDate)
        {
            return toEpochMillis(((LocalDate) temporal).atStartOfDay(GMT_ZONE).toInstant());
        }
        else if (temporal instanceof YearMonth)
        {
            return toEpochMillis(((YearMonth) temporal).atDay(1));
        }
        else if (temporal instanceof Year)
        {
            return toEpochMillis(((Year) temporal).atDay(1));
        }
        throw new IllegalArgumentException("Unhandled type " + temporal.getClass());
    }
}
