package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2022 Morten Haraldsen (ethlo)
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
 * Consumer for flexibly dealing with different granularity date/date-times
 */
public interface TemporalConsumer
{
    default void handle(LocalDateTime localDateTime)
    {
        fallback(localDateTime);
    }

    default void handle(LocalDate localDate)
    {
        fallback(localDate);
    }

    default void handle(YearMonth yearMonth)
    {
        fallback(yearMonth);
    }

    default void handle(Year year)
    {
        fallback(year);
    }

    default void handle(OffsetDateTime offsetDateTime)
    {
        fallback(offsetDateTime);
    }

    default void fallback(final Temporal temporal)
    {
        throw new UnsupportedOperationException("Unhandled type " + temporal.getClass());
    }
}
