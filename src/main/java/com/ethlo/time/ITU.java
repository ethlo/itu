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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;

public class ITU
{
    private static final EthloITU delegate = EthloITU.getInstance();

    private ITU()
    {
    }

    public static OffsetDateTime parseDateTime(String s)
    {
        return delegate.parseDateTime(s);
    }

    public static DateTime parseLenient(String text)
    {
        return delegate.parse(text);
    }

    public static boolean isValid(String dateTime)
    {
        try
        {
            parseDateTime(dateTime);
            return true;
        }
        catch (DateTimeException exc)
        {
            return false;
        }
    }

    public static String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        return delegate.formatUtc(date, fractionDigits);
    }

    public static String formatUtc(OffsetDateTime date, Field lastIncluded)
    {
        return delegate.formatUtc(date, lastIncluded);
    }

    public static String formatUtc(OffsetDateTime date)
    {
        return delegate.formatUtc(date);
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

    public static void parse(String text, TemporalConsumer temporalConsumer)
    {
        final DateTime dateTime = delegate.parse(text);
        if (dateTime.includesGranularity(Field.MINUTE))
        {
            if (dateTime.getOffset().isPresent())
            {
                temporalConsumer.handle(dateTime.toOffsetDatetime());
            }
            else
            {
                temporalConsumer.handle(dateTime.toLocalDatetime());
            }
        }
        else if (dateTime.includesGranularity(Field.DAY))
        {
            temporalConsumer.handle(dateTime.toLocalDate());
        }
        else if (dateTime.includesGranularity(Field.MONTH))
        {
            temporalConsumer.handle(dateTime.toYearMonth());
        }
        else
        {
            temporalConsumer.handle(Year.of(dateTime.getYear()));
        }
    }

    public static <T> T parse(String text, TemporalHandler<T> temporalHandler)
    {
        final DateTime dateTime = delegate.parse(text);
        if (dateTime.includesGranularity(Field.MINUTE))
        {
            if (dateTime.getOffset().isPresent())
            {
                return temporalHandler.handle(dateTime.toOffsetDatetime());
            }
            else
            {
                return temporalHandler.handle(dateTime.toLocalDatetime());
            }
        }
        else if (dateTime.includesGranularity(Field.DAY))
        {
            return temporalHandler.handle(dateTime.toLocalDate());
        }
        else if (dateTime.includesGranularity(Field.MONTH))
        {
            return temporalHandler.handle(dateTime.toYearMonth());
        }
        else
        {
            return temporalHandler.handle(Year.of(dateTime.getYear()));
        }
    }

    public static boolean isValid(final String text, TemporalType... types)
    {
        try
        {
            return ITU.parse(text, new TemporalHandler<Boolean>()
            {
                @Override
                public Boolean handle(final LocalDate localDate)
                {
                    return isAllowed(TemporalType.LOCAL_DATE, types);
                }

                @Override
                public Boolean handle(final OffsetDateTime offsetDateTime)
                {
                    return isAllowed(TemporalType.OFFSET_DATE_TIME, types);
                }

                @Override
                public Boolean handle(final LocalDateTime localDateTime)
                {
                    return isAllowed(TemporalType.LOCAL_DATE_TIME, types);
                }

                @Override
                public Boolean handle(final YearMonth yearMonth)
                {
                    return isAllowed(TemporalType.YEAR_MONTH, types);
                }

                @Override
                public Boolean handle(final Year year)
                {
                    return isAllowed(TemporalType.YEAR, types);
                }
            });
        }
        catch (DateTimeException exc)
        {
            return false;
        }
    }

    private static void assertIsAllowed(TemporalType needle, TemporalType... allowed)
    {
        if (!isAllowed(needle, allowed))
        {
            throw new DateTimeException("Is not a valid representation of " + needle);
        }
    }

    private static boolean isAllowed(TemporalType needle, TemporalType... allowed)
    {
        for (TemporalType t : allowed)
        {
            if (t.equals(needle))
            {
                return true;
            }
        }
        return false;
    }
}
