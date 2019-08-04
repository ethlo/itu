package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2019 Morten Haraldsen (ethlo)
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
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(CorrectnessTest.class)
public class FieldTest
{
    @Test
    public void testGetKnownFields()
    {
        final Field year = Field.valueOf(Year.class);
        final Field month = Field.valueOf(YearMonth.class);
        final Field day = Field.valueOf(LocalDate.class);
        final Field second = Field.valueOf(OffsetDateTime.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnknown()
    {
        Field.valueOf(Temporal.class);
    }
}
