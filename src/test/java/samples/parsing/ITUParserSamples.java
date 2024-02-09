package samples.parsing;

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

import static com.ethlo.time.DateTimeTokens.digits;
import static com.ethlo.time.DateTimeTokens.fractions;
import static com.ethlo.time.DateTimeTokens.separators;
import static com.ethlo.time.Field.DAY;
import static com.ethlo.time.Field.HOUR;
import static com.ethlo.time.Field.MINUTE;
import static com.ethlo.time.Field.MONTH;
import static com.ethlo.time.Field.SECOND;
import static com.ethlo.time.Field.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.ethlo.time.DateTime;
import com.ethlo.time.DateTimeParser;
import com.ethlo.time.DateTimeParsers;
import com.ethlo.time.ITU;
import com.ethlo.time.ParseConfig;
import com.ethlo.time.TemporalHandler;

/*

## Parsing

This is a collection of usage examples for parsing.

 */
class ITUParserSamples
{
    /*
    The simplest and fastest way to parse an RFC-3339 timestamp by far!
     */
    @Test
    void parseRfc3339()
    {
        final String text = "2012-12-27T19:07:22.123456789-03:00";
        final OffsetDateTime dateTime = ITU.parseDateTime(text);
        assertThat(dateTime.toString()).isEqualTo(text);
    }

    /*
    Parses a date-time with flexible granularity. Works for anything from a year to a timestamp with nanoseconds, with or without timezone offset.
     */
    @Test
    void parseLenient()
    {
        final String text = "2012-12-27T19:07:23.123";
        final DateTime dateTime = ITU.parseLenient(text);
        final String formatted = dateTime.toString();

        //Note the tracking of fractional resolution
        assertThat(formatted).isEqualTo(text);
    }

    /*
    In case you encounter the need for a somewhat different time-separator or fraction separator
    you can use the `ParseConfig` to set up you preferred delimiters.
     */
    @Test
    void parseLenientWithCustomSeparators()
    {
        final ParseConfig config = ParseConfig.DEFAULT
                .withDateTimeSeparators('T', '|')
                .withFractionSeparators('.', ',');
        final DateTime result = ITU.parseLenient("1999-11-22|11:22:17,191", config);
        assertThat(result.toString()).isEqualTo("1999-11-22T11:22:17.191");
    }

    /*
     This allows you to track where to start reading. Note that the check for trailing junk is disabled when using `ParsePosition`.
     */
    @Test
    void parsePosition()
    {
        final ParsePosition pos = new ParsePosition(10);
        final OffsetDateTime result = ITU.parseDateTime("some-data,1999-11-22T11:22:19+05:30,some-other-data", pos);
        assertThat(result.toString()).isEqualTo("1999-11-22T11:22:19+05:30");
        assertThat(pos.getIndex()).isEqualTo(35);
    }

    /*
     This is useful if you need to handle different granularity with different logic or interpolation.
     */
    @Test
    void explicitGranularity()
    {
        final TemporalHandler<OffsetDateTime> handler = new TemporalHandler<OffsetDateTime>()
        {
            @Override
            public OffsetDateTime handle(final LocalDate localDate)
            {
                return localDate.atTime(OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC));
            }

            @Override
            public OffsetDateTime handle(final OffsetDateTime offsetDateTime)
            {
                return offsetDateTime;
            }
        };

        final OffsetDateTime result = ITU.parse("2017-12-06", handler);
        assertThat(result.toString()).isEqualTo("2017-12-06T00:00Z");
    }

    /*
    In some real world scenarios, it is useful to parse a best-effort timestamp. To ease usage, we can easily convert a raw `DateTime` instance into `Instant`.

    Note the limitations and the assumption of UTC time-zone, as mentioned in the javadoc.
    */
    @Test
    void lenientTimestamp()
    {
        final Instant instant = ITU.parseLenient("2017-12-06").toInstant();
        assertThat(instant.toString()).isEqualTo("2017-12-06T00:00:00Z");
    }

    /*
     In case the format is not supported directly, you can build your own parser.
     */
    @Test
    void parseCustomFormat()
    {
        final DateTimeParser parser = DateTimeParsers.of(
                digits(DAY, 2),
                separators('-'),
                digits(MONTH, 2),
                separators('-'),
                digits(YEAR, 4),
                separators(' '),
                digits(HOUR, 2),
                digits(MINUTE, 2),
                digits(SECOND, 2),
                separators(','),
                fractions()
        );
        final String text = "31-12-2000 235937,123456";
        final DateTime result = parser.parse(text);
        assertThat(result.toString()).isEqualTo("2000-12-31T23:59:37.123456");
    }

    /*
    `DateTimerParser` interface for RFC-3339.
     */
    @Test
    void parseUsingInterfaceRfc33939()
    {
        final DateTimeParser parser = DateTimeParsers.rfc3339();
        final String text = "2000-12-31 23:59:37.123456";
        final DateTime result = parser.parse(text);
        assertThat(result.toString()).isEqualTo("2000-12-31T23:59:37.123456");
    }

    /*
    `DateTimerParser` interface for local time.
     */
    @Test
    void parseUsingInterfaceLocalTime()
    {
        final DateTimeParser parser = DateTimeParsers.localTime();
        final String text = "23:59:37.123456";
        final LocalTime result = parser.parse(text).toLocalTime();
        assertThat(result.toString()).isEqualTo(text);
    }

    /*
    `DateTimerParser` interface for local date.
     */
    @Test
    void parseUsingInterfaceLocalDate()
    {
        final DateTimeParser parser = DateTimeParsers.localDate();
        final String text = "2013-12-24";
        final LocalDate result = parser.parse(text).toLocalDate();
        assertThat(result.toString()).isEqualTo(text);
    }
}
