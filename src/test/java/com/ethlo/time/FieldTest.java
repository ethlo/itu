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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("CorrectnessTest")
public class FieldTest
{
    @Test
    public void testGetKnownFields()
    {
        assertThat(Field.valueOf(Year.class)).isEqualTo(Field.YEAR);
        assertThat(Field.valueOf(YearMonth.class)).isEqualTo(Field.MONTH);
        assertThat(Field.valueOf(LocalDate.class)).isEqualTo(Field.DAY);
        assertThat(Field.valueOf(OffsetDateTime.class)).isEqualTo(Field.SECOND);
    }

    @Test
    public void testGetUnknown()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Field.valueOf(Temporal.class));
    }
}
