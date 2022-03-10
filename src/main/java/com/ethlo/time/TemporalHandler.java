package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 Morten Haraldsen (ethlo)
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;

/**
 * Handler for flexibly dealing with different granularity date/date-times
 * @param <T> The return type of the functions
 */
public interface TemporalHandler<T>
{
    default T handle(LocalDateTime localDateTime)
    {
        return fallback(localDateTime);
    }

    default T handle(LocalDate localDate)
    {
        return fallback(localDate);
    }

    default T handle(YearMonth yearMonth)
    {
        return fallback(yearMonth);
    }

    default T handle(Year year)
    {
        return fallback(year);
    }

    default T handle(OffsetDateTime offsetDateTime)
    {
        return fallback(offsetDateTime);
    }

    default T fallback(final Temporal temporal)
    {
        throw new UnsupportedOperationException("Unhandled type " + temporal.getClass());
    }
}
