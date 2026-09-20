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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

import org.junit.jupiter.api.Test;

/**
 * Which ChronoFields a parsed value supports follows its granularity, with two exceptions: INSTANT_SECONDS and
 * NANO_OF_SECOND are always supported (absent fields read as the start of their range) so that Instant.from(..)
 * works on every granularity.
 */
public class TemporalAccessorTest
{
    @Test
    void testTemporalAccessorYear()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isFalse();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isFalse();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isFalse();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isFalse();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isTrue();
        assertThat(parsed.getLong(ChronoField.NANO_OF_SECOND)).isZero();
        assertThat(parsed.getLong(ChronoField.YEAR)).isEqualTo(2017);
    }

    @Test
    void testTemporalAccessorMonth()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isFalse();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isFalse();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isFalse();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isTrue();
        assertThat(parsed.getLong(ChronoField.NANO_OF_SECOND)).isZero();

        assertThat(parsed.getLong(ChronoField.MONTH_OF_YEAR)).isEqualTo(1);
    }

    @Test
    void testTemporalAccessorDay()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isFalse();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isFalse();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isTrue();
        assertThat(parsed.getLong(ChronoField.NANO_OF_SECOND)).isZero();

        assertThat(parsed.getLong(ChronoField.DAY_OF_MONTH)).isEqualTo(27);
    }

    @Test
    void testTemporalAccessorHour()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27T13:22");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isTrue();
        assertThat(parsed.getLong(ChronoField.NANO_OF_SECOND)).isZero();

        assertThat(parsed.getLong(ChronoField.HOUR_OF_DAY)).isEqualTo(13);
    }

    @Test
    void testTemporalAccessorMinute()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27T15:34");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isTrue();
        assertThat(parsed.getLong(ChronoField.NANO_OF_SECOND)).isZero();

        assertThat(parsed.getLong(ChronoField.MINUTE_OF_HOUR)).isEqualTo(34);
    }

    @Test
    void testTemporalAccessorSecond()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27T15:34:49");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isTrue();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isTrue();
        assertThat(parsed.getLong(ChronoField.NANO_OF_SECOND)).isZero();

        assertThat(parsed.getLong(ChronoField.SECOND_OF_MINUTE)).isEqualTo(49);
    }

    @Test
    void testTemporalAccessorNanos()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27T15:34:49.987654321");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isTrue();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isTrue();

        assertThat(parsed.getLong(ChronoField.NANO_OF_SECOND)).isEqualTo(987654321);
    }

    /**
     * Instant.from(..) is how generic java.time code reads a DateTime: it must agree with java.time's own parse of
     * the same text, and with toInstant()
     */
    @Test
    void testInstantFrom()
    {
        final String text = "2018-11-01T14:45:59.123456789+04:00";
        final DateTime parsed = ITU.parseLenient(text);
        assertThat(Instant.from(parsed)).isEqualTo(OffsetDateTime.parse(text).toInstant());
        assertThat(Instant.from(parsed)).isEqualTo(parsed.toInstant());
    }

    @Test
    void testInstantFromWithoutOffsetIsUtc()
    {
        final DateTime parsed = ITU.parseLenient("2017-01-27T15:34:49.987654321");
        assertThat(Instant.from(parsed)).isEqualTo(LocalDateTime.parse("2017-01-27T15:34:49.987654321").toInstant(ZoneOffset.UTC));
    }
}
