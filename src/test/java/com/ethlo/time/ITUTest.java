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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("CorrectnessTest")
public class ITUTest
{
    private static final OffsetDateTime VALID_DATETIME = OffsetDateTime.parse("2017-05-01T16:23:12Z");

    @Test
    public void parseDateTime()
    {
        assertThat(ITU.parseDateTime(VALID_DATETIME.toString())).isNotNull();
    }

    @Test
    public void testFormatUtc()
    {
        assertThat(ITU.formatUtc(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void isValid()
    {
        assertThat(ITU.isValid("2017-asddsd")).isFalse();
    }

    @Test
    public void isValidEmpty()
    {
        assertThat(ITU.isValid("")).isFalse();
    }

    @Test
    public void isValidNull()
    {
        assertThrows(NullPointerException.class, () -> ITU.isValid(null));
    }

    @Test
    public void formatUtcMicro()
    {
        assertThat(ITU.formatUtcMicro(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void formatUtcNano()
    {
        assertThat(ITU.formatUtcNano(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void testFormatUtcMilli()
    {
        assertThat(ITU.formatUtcMilli(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void parseLenient()
    {
        assertThat(ITU.parseLenient("2017-01-31")).isNotNull();
    }

    @Test
    public void parseLenient2()
    {
        assertThat(ITU.parseLenient("2017-01-31")).isNotNull();
    }
}
