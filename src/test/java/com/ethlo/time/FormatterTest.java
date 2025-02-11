package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2024 Morten Haraldsen (ethlo)
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

import java.time.DateTimeException;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

public class FormatterTest
{
    @Test
    void testFormatSpecifiedResolution()
    {
        final String s = "2017-02-21T10:00:00.000+12:00";
        final OffsetDateTime date = ITU.parseDateTime(s);
        assertThat(ITU.format(date, 9)).isEqualTo("2017-02-21T10:00:00.000000000+12:00");
    }

    @Test
    void testFormatYear()
    {
        assertThat(DateTime.ofYear(1234).toString()).isEqualTo("1234");
    }

    @Test
    void testFormatYearMonth()
    {
        assertThat(DateTime.ofYearMonth(1234, 12).toString()).isEqualTo("1234-12");
    }

    @Test
    void testFormatHigherGranularityThanAvailable()
    {
        final DateTimeException exc = assertThrows(DateTimeException.class, () -> DateTime.ofYear(1234).toString(Field.DAY));
        assertThat(exc).hasMessage("Requested granularity was DAY, but contains only granularity YEAR");
    }

    @Test
    void testFormatWithFractionDigits()
    {
        final DateTimeException exc = assertThrows(DateTimeException.class, () -> DateTime.ofYear(1234).toString(Field.DAY));
        assertThat(exc).hasMessage("Requested granularity was DAY, but contains only granularity YEAR");
    }

    @Test
    void testFormatMoreThanNanoResolutionFails()
    {
        final OffsetDateTime d = ITU.parseDateTime("2017-02-21T15:00:00.123456789Z");
        final int fractionDigits = 10;
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> ITU.formatUtc(d, fractionDigits));
        assertThat(exception.getMessage()).isEqualTo("Maximum supported number of fraction digits in second is 9, got 10");
    }

    @Test
    void testFormatUtc()
    {
        final String s = "2017-02-21T15:09:03.123456789Z";
        final OffsetDateTime date = ITU.parseDateTime(s);
        final String expected = "2017-02-21T15:09:03Z";
        final String actual = ITU.formatUtc(date);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testFormatHour()
    {
        assertThat(DateTime.of(2000, 12, 31, 22, 30, null).toString(Field.HOUR)).isEqualTo("2000-12-31T22");
    }

    @Test
    void testFormatUtcMilli()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = ITU.parseDateTime(s);
        assertThat(ITU.formatUtcMilli(date)).isEqualTo("2017-02-21T15:00:00.123Z");
    }

    @Test
    void testFormatUtcMicro()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = ITU.parseDateTime(s);
        assertThat(ITU.formatUtcMicro(date)).isEqualTo("2017-02-21T15:00:00.123456Z");
    }

    @Test
    void testFormatUtcNano()
    {
        final String s = "2017-02-21T15:00:00.987654321Z";
        final OffsetDateTime date = ITU.parseDateTime(s);
        assertThat(ITU.formatUtcNano(date)).isEqualTo(s);
    }

    @Test
    void testFormat5()
    {
        final String s = "2017-02-21T15:27:39.123+13:00";
        final OffsetDateTime date = ITU.parseDateTime(s);
        assertThat(ITU.formatUtcMilli(date)).isEqualTo("2017-02-21T02:27:39.123Z");
    }

    @Test
    void testTostring()
    {
        final DateTime dateTime = DateTime.of(2000, 12, 31, 22, 30, 0, 0, null, 0);
        assertThat(dateTime.toString()).isEqualTo("2000-12-31T22:30:00");
    }
}
