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

import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

public class LenientParseTests
{
    @Test
    public void testParseTimestampAndUseTemporalAccessor()
    {
        final String s = "2018-11-01T14:45:59.123456789+04:00";
        final DateTime a = ITU.parseLenient(s);
        assertThat(a.getFractionDigits()).isEqualTo(9);
        assertThat(Instant.from(a)).isEqualTo(OffsetDateTime.parse(s).toInstant());
    }

    @Test
    public void testParseTimestamp()
    {
        final String s = "2018-11-01T14:45:59.12356789+04:00";
        final DateTime a = ITU.parseLenient(s);
        assertThat(a.toInstant()).isEqualTo(OffsetDateTime.parse(s).toInstant());
    }

    @Test
    public void testParseYear()
    {
        final String s = "2018";
        final DateTime a = ITU.parseLenient(s);
        assertThat(a.toInstant()).isEqualTo(OffsetDateTime.parse(s + "-01-01T00:00:00Z").toInstant());
    }

    @Test
    public void testParseYearMonth()
    {
        final String s = "2018-11";
        final DateTime a = ITU.parseLenient(s);
        assertThat(a.toInstant()).isEqualTo(OffsetDateTime.parse(s + "-01T00:00:00Z").toInstant());
    }

    @Test
    public void testParseYearDayMonth()
    {
        final String s = "1997-11-30";
        final DateTime a = ITU.parseLenient(s);
        assertThat(a.toInstant()).isEqualTo(OffsetDateTime.parse(s + "T00:00:00Z").toInstant());
    }

    @Test
    public void testParseYearMonthHourMinutes()
    {
        final String s = "2018-11-27T12:30";
        final DateTime a = ITU.parseLenient(s);
        assertThat(a.toInstant()).isEqualTo(OffsetDateTime.parse(s + "Z").toInstant());
    }
}
