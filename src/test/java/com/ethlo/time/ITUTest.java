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

import static com.ethlo.time.TemporalType.LOCAL_DATE;
import static com.ethlo.time.TemporalType.LOCAL_DATE_TIME;
import static com.ethlo.time.TemporalType.OFFSET_DATE_TIME;
import static com.ethlo.time.TemporalType.YEAR;
import static com.ethlo.time.TemporalType.YEAR_MONTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
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
        assertThat(DateTime.of(2012, 10, 31, 22, 50, TimezoneOffset.UTC).toString(Field.MINUTE)).isEqualTo("2012-10-31T22:50Z");
    }

    @Test
    void formatDateTimeWithFullGranularity()
    {
        assertThat(DateTime.of(2012, 11, 30, 22, 50, 46, 1234567, TimezoneOffset.UTC, 7).toString()).isEqualTo("2012-11-30T22:50:46.1234567Z");
    }

    @Test
    void formatDateTimeWithSecondGranularity()
    {
        final OffsetDateTime input = OffsetDateTime.of(2012, 11, 30, 22, 50, 46, 123456789, ZoneOffset.ofHoursMinutes(-9, -30));
        assertThat(ITU.format(input)).isEqualTo("2012-11-30T22:50:46-09:30");
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
        assertThat(exc).hasMessage("Expected character [T, t,  ] at position 11: 2017-03-05G");
    }

    @Test
    void testParseNull()
    {
        final NullPointerException exception = assertThrows(NullPointerException.class, () -> ITU.parseDateTime(null));
        assertThat(exception.getMessage()).isEqualTo("text cannot be null");
    }

    @Test
    void testRfcExample()
    {
        // 1994-11-05T08:15:30-05:00 corresponds to November 5, 1994, 8:15:30 am, US Eastern Standard Time/
        // 1994-11-05T13:15:30Z corresponds to the same instant.
        final String a = "1994-11-05T08:15:30-05:00";
        final String b = "1994-11-05T13:15:30Z";
        final OffsetDateTime dA = ITU.parseDateTime(a);
        final OffsetDateTime dB = ITU.parseDateTime(b);
        assertThat(ITU.formatUtc(dA)).isEqualTo(ITU.formatUtc(dB));
    }

    @Test
    void testParseCommaFractionSeparator()
    {
        final ParseConfig config = ParseConfig.DEFAULT
                .withFractionSeparators('.', ',')
                .withDateTimeSeparators('T', '|');
        final ParsePosition pos = new ParsePosition(0);
        assertThat(ITU.parseLenient("1999-11-22|11:22:17,191", config, pos).toInstant()).isEqualTo(Instant.parse("1999-11-22T11:22:17.191Z"));
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
        assertThat(pos.getIndex()).isEqualTo(23);
    }

    @Test
    void testParseUnparseable()
    {
        final ParsePosition pos = new ParsePosition(0);
        assertThrows(DateTimeParseException.class, () -> ITU.parseLenient("1999-11-22|11:22:1", ParseConfig.DEFAULT, pos));
        assertThat(pos.getErrorIndex()).isEqualTo(10);
        assertThat(pos.getIndex()).isEqualTo(10);
    }

    @Test
    void testParsePosition()
    {
        final ParsePosition pos = new ParsePosition(0);
        ITU.parseLenient("1999-11-22T11:22:17.191", ParseConfig.DEFAULT, pos);
        assertThat(pos.getIndex()).isEqualTo(23);
    }

    @Test
    void testParsePositionDateTime()
    {
        final ParsePosition pos = new ParsePosition(0);
        ITU.parseDateTime("1999-11-22T11:22:17.191Z", pos);
        assertThat(pos.getIndex()).isEqualTo(24);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
    }

    @Test
    void testParsePositionDateTimeInvalid()
    {
        final ParsePosition pos = new ParsePosition(0);
        assertThrows(DateTimeException.class, () -> ITU.parseDateTime("1999-11-22X11:22:17.191Z", pos));
        assertThat(pos.getIndex()).isEqualTo(10);
        assertThat(pos.getErrorIndex()).isEqualTo(10);
    }
}
