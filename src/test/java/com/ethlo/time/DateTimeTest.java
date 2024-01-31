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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("CorrectnessTest")
public class DateTimeTest
{
    final DateTime a = ITU.parseLenient("2018-01-06T23:59:12-04:30");
    final DateTime b = ITU.parseLenient("2018-01-06T23:59:12-04:30");
    final DateTime c = ITU.parseLenient("2018-01-06T23:59:12-05:30");
    final DateTime d = DateTime.of(2000, 12, 10, 12, 22, 18, TimezoneOffset.UTC);
    final DateTime e = DateTime.ofDate(2000, 12, 10);

    @Test
    public void testEquals()
    {
        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(b);
        assertThat(b).isEqualTo(a);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo("");
    }

    @Test
    public void testHashcode()
    {
        System.out.println(a);
        assertThat(a.hashCode()).isEqualTo(-309185068);
    }
}
