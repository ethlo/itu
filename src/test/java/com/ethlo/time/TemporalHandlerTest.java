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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TemporalHandlerTest
{
    @Test
    void handle()
    {
        final TemporalHandler<TemporalType> handler = new TemporalHandler<TemporalType>()
        {
        };

        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(LocalDate.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(LocalDateTime.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(OffsetDateTime.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(Year.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(YearMonth.now()));
    }

    @Test
    void handleConsumer()
    {
        final TemporalConsumer consumer = new TemporalConsumer()
        {
        };

        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(LocalDate.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(LocalDateTime.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(OffsetDateTime.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(Year.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(YearMonth.now()));
    }
}
