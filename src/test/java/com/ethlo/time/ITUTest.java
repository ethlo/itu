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

import static com.ethlo.time.TemporalType.LOCAL_DATE;
import static com.ethlo.time.TemporalType.LOCAL_DATE_TIME;
import static com.ethlo.time.TemporalType.OFFSET_DATE_TIME;
import static com.ethlo.time.TemporalType.YEAR;
import static com.ethlo.time.TemporalType.YEAR_MONTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;

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
    public void parseDateTimeWithoutSeconds()
    {
        assertThrows(DateTimeException.class, () -> ITU.parseDateTime("2017-12-09T11:23Z"));
    }

    @Test
    void formatUtcWithFractionDigits()
    {
        assertThat(ITU.formatUtc(VALID_DATETIME, 6)).isEqualTo("2017-05-01T16:23:12.000000Z");
    }

    @Test
    void formatOffsetDateTimeWithLimitedGranularity()
    {
        assertThat(ITU.formatUtc(VALID_DATETIME, Field.MINUTE)).isEqualTo("2017-05-01T16:23Z");
    }

    @Test
    void formatDateTimeWithLimitedGranularity()
    {
        assertThat(ITU.formatUtc(DateTime.of(2012, 11, 31, 22, 50, TimezoneOffset.UTC), Field.MINUTE)).isEqualTo("2012-11-31T22:50Z");
    }

    @Test
    void formatDateTimeWithFullGranularity()
    {
        assertThat(ITU.formatUtc(DateTime.of(2012, 11, 31, 22, 50, 46, 123456789, TimezoneOffset.UTC), 9)).isEqualTo("2012-11-31T22:50:46.123456789Z");
    }

    @Test
    public void testFormatUtc()
    {
        assertThat(ITU.formatUtc(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void isValidFalse()
    {
        assertThat(ITU.isValid("2017-asddsd")).isFalse();
    }

    @Test
    public void isValidTrue()
    {
        assertThat(ITU.isValid("2017-12-09T11:23:39Z")).isTrue();
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

    @Test
    public void parseLenientConsumer()
    {
        ITU.parse("2017-01-31", new TemporalConsumer()
        {
            @Override
            public void handle(final LocalDate localDate)
            {
                assertThat(localDate).isEqualTo(LocalDate.of(2017, 1, 31));
            }

            @Override
            public void fallback(final Temporal temporal)
            {
                fail("Should not fall back");
            }
        });
    }

    @Test
    public void parseLenientConsumerLocalDate()
    {
        final String input = "2017-01-31";

        assertThat(ITU.isValid(input, YEAR)).isFalse();
        assertThat(ITU.isValid(input, YEAR_MONTH)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE)).isTrue();
        assertThat(ITU.isValid(input, LOCAL_DATE_TIME)).isFalse();
        assertThat(ITU.isValid(input, OFFSET_DATE_TIME)).isFalse();

        ITU.parse(input, new TemporalConsumer()
        {
            @Override
            public void handle(final LocalDate localDate)
            {
                assertThat(localDate).isEqualTo(LocalDate.of(2017, 1, 31));
            }
        });
    }

    @Test
    public void parseLenientConsumerLocalDateTime()
    {
        final String input = "2017-01-31T14:00";

        assertThat(ITU.isValid(input, YEAR)).isFalse();
        assertThat(ITU.isValid(input, YEAR_MONTH)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE_TIME)).isTrue();
        assertThat(ITU.isValid(input, OFFSET_DATE_TIME)).isFalse();

        ITU.parse(input, new TemporalConsumer()
        {
            @Override
            public void handle(final LocalDateTime localDateTime)
            {
                assertThat(localDateTime).isEqualTo(LocalDateTime.parse(input));
            }
        });
    }

    @Test
    public void parseLenientConsumerOffsetDateTime()
    {
        final String input = "2017-01-31T15:04:32+04:00";

        assertThat(ITU.isValid(input, YEAR)).isFalse();
        assertThat(ITU.isValid(input, YEAR_MONTH)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE_TIME)).isFalse();
        assertThat(ITU.isValid(input, OFFSET_DATE_TIME)).isTrue();

        ITU.parse(input, new TemporalConsumer()
        {
            @Override
            public void handle(final OffsetDateTime offsetDateTime)
            {
                assertThat(offsetDateTime).isEqualTo(OffsetDateTime.parse(input));
            }
        });
    }

    @Test
    public void parseLenientConsumerYearMonth()
    {
        final String input = "2017-01";

        assertThat(ITU.isValid(input, YEAR)).isFalse();
        assertThat(ITU.isValid(input, YEAR_MONTH)).isTrue();
        assertThat(ITU.isValid(input, LOCAL_DATE)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE_TIME)).isFalse();
        assertThat(ITU.isValid(input, OFFSET_DATE_TIME)).isFalse();

        ITU.parse(input, new TemporalConsumer()
        {
            @Override
            public void handle(final YearMonth yearMonth)
            {
                assertThat(yearMonth).isEqualTo(YearMonth.parse(input));
            }
        });
    }

    @Test
    public void testWorksAsAny()
    {
        final String input = "2018-01";
        assertThat(ITU.isValid(input, YEAR, YEAR_MONTH)).isTrue();
    }

    @Test
    public void parseLenientConsumerYear()
    {
        final String input = "2017";

        assertThat(ITU.isValid(input, YEAR)).isTrue();
        assertThat(ITU.isValid(input, YEAR_MONTH)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE_TIME)).isFalse();
        assertThat(ITU.isValid(input, OFFSET_DATE_TIME)).isFalse();

        ITU.parse(input, new TemporalConsumer()
        {
            @Override
            public void handle(final Year year)
            {
                assertThat(year).isEqualTo(Year.parse(input));
            }
        });
    }

    @Test
    public void parseLenientUnparseable()
    {
        final String input = "2017-03-05G";

        assertThat(ITU.isValid(input, YEAR)).isFalse();
        assertThat(ITU.isValid(input, YEAR_MONTH)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE)).isFalse();
        assertThat(ITU.isValid(input, LOCAL_DATE_TIME)).isFalse();
        assertThat(ITU.isValid(input, OFFSET_DATE_TIME)).isFalse();

        final DateTimeException exc = assertThrows(DateTimeException.class, () -> ITU.parse(input, new TemporalConsumer()
        {
            @Override
            public void handle(final Year year)
            {
                assertThat(year).isEqualTo(Year.parse(input));
            }
        }));
        assertThat(exc).hasMessage("Expected character [T, t,  ] at position 11 '2017-03-05G'");
    }
}
