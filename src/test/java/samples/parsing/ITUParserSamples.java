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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

import com.ethlo.time.DateTime;
import com.ethlo.time.DateTimeParser;
import com.ethlo.time.DateTimeParsers;
import com.ethlo.time.Field;
import com.ethlo.time.ITU;
import com.ethlo.time.MutableDateTimeBuffer;
import com.ethlo.time.ParseConfig;
import com.ethlo.time.TemporalHandler;
import com.ethlo.time.TemporalType;

/*

## Parsing

 */
class ITUParserSamples
{
    /*
    The simplest and fastest way to parse an RFC-3339 timestamp. The input must be a complete date-time with an
    offset; the result is a `java.time.OffsetDateTime`.
     */
    @Test
    void parseRfc3339()
    {
        final String text = "2012-12-27T19:07:22.123456789-03:00";
        final OffsetDateTime dateTime = ITU.parseDateTime(text);
        assertThat(dateTime.toString()).isEqualTo(text);
    }

    /*
    Lenient parsing accepts anything from a year to a timestamp with nanoseconds, with or without an offset, and
    returns a `DateTime` that remembers exactly what was there: the most granular field, the number of fraction
    digits and whether there was an offset. Formatting it back gives the input.
     */
    @Test
    void parseLenient()
    {
        final String text = "2012-12-27T19:07:23.123";
        final DateTime dateTime = ITU.parseLenient(text);
        assertThat(dateTime.getMostGranularField()).isEqualTo(Field.NANO);
        assertThat(dateTime.getFractionDigits()).isEqualTo(3);
        assertThat(dateTime.getOffset()).isEmpty();
        assertThat(dateTime.toString()).isEqualTo(text);
    }

    /*
    The granularity is kept, so a partial date can be converted to the matching `java.time` type, and asking for a
    field that was not in the input is an error rather than a silent default.
     */
    @Test
    void parseLenientGranularity()
    {
        assertThat(ITU.parseLenient("2012").toYear().getValue()).isEqualTo(2012);
        assertThat(ITU.parseLenient("2012-12").toYearMonth().toString()).isEqualTo("2012-12");
        assertThat(ITU.parseLenient("2012-12-27").toLocalDate().toString()).isEqualTo("2012-12-27");
        assertThat(ITU.parseLenient("2012-12-27T19:07").toLocalDatetime().toString()).isEqualTo("2012-12-27T19:07");
        assertThat(ITU.parseLenient("2012-12-27T19:07:22+01:00").toOffsetDatetime().toString()).isEqualTo("2012-12-27T19:07:22+01:00");

        final DateTime dateOnly = ITU.parseLenient("2012-12-27");
        assertThat(dateOnly.includesGranularity(Field.HOUR)).isFalse();
        assertThrows(DateTimeException.class, dateOnly::toOffsetDatetime);
    }

    /*
    When a best-effort timestamp is all that is needed, a `DateTime` of any granularity converts to an `Instant`:
    missing month and day default to 1, missing time fields to 0, and a missing offset to UTC.
     */
    @Test
    void lenientTimestamp()
    {
        final Instant instant = ITU.parseLenient("2017-12-06").toInstant();
        assertThat(instant.toString()).isEqualTo("2017-12-06T00:00:00Z");
    }

    /*
    Checking validity without parsing, and without exceptions. The single-argument form checks for a full RFC-3339
    date-time; the varargs form accepts any of the listed granularities.
     */
    @Test
    void isValid()
    {
        assertThat(ITU.isValid("2012-12-27T19:07:22Z")).isTrue();
        assertThat(ITU.isValid("2012-12-27")).isFalse();
        assertThat(ITU.isValid("2012-12-27", TemporalType.LOCAL_DATE, TemporalType.OFFSET_DATE_TIME)).isTrue();
        assertThat(ITU.isValid("2012-13-27", TemporalType.LOCAL_DATE)).isFalse();
    }

    /*
    Errors are `java.time.format.DateTimeParseException` with the input and the 0-based index of the offending
    character, and the message names the 1-based position and what was expected.
     */
    @Test
    void parseError()
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseDateTime("2012-12-27T19:07:22.123456789"));
        assertThat(exc.getErrorIndex()).isEqualTo(29);
        assertThat(exc.getMessage()).isEqualTo("No timezone information: 2012-12-27T19:07:22.123456789");
    }

    /*
    `ParseConfig` widens what is accepted: the characters allowed between date and time and before the fraction,
    and whether text after the date-time is an error. `ParseConfig.DEFAULT` accepts `T`, `t` and space as the
    date-time separator; `ParseConfig.STRICT` only `T`.
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
    A `ParsePosition` starts the parse inside a larger text and reports where it stopped. Text after the
    date-time is not an error in this mode, since the position tells you where to continue.
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
    When the text is already available as characters, parse into a reusable buffer: nothing is allocated, and the
    fields are read straight off the buffer. This is the fastest way to parse. The window `[offset, offset + length)`
    is the text, so trailing junk inside it is rejected and anything outside it is never read.
     */
    @Test
    void parseIntoBuffer()
    {
        final char[] chars = "2012-12-27T19:07:22.123456789-03:00".toCharArray();
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        final int consumed = ITU.parseLenient(chars, 0, chars.length, buffer);
        assertThat(consumed).isEqualTo(35);
        assertThat(buffer.getYear()).isEqualTo(2012);
        assertThat(buffer.getNano()).isEqualTo(123456789);
        assertThat(buffer.getOffsetTotalSeconds()).isEqualTo(-3 * 3600);
        assertThat(buffer.toEpochMilli()).isEqualTo(1356646042123L);
    }

    /*
    A Unix epoch count written as text, in seconds or milliseconds, parses to the same `DateTime` as an RFC-3339
    string, so a field that may carry either can go through one code path. The `char[]` overloads into a
    `MutableDateTimeBuffer` exist for these too.
     */
    @Test
    void parseEpoch()
    {
        assertThat(ITU.parseEpochSecond("1695300000").toString()).isEqualTo("2023-09-21T12:40:00Z");
        assertThat(ITU.parseEpochMilli("1695300000123").toString()).isEqualTo("2023-09-21T12:40:00.123Z");
        assertThat(ITU.parseEpochMilli("-1").toInstant().toEpochMilli()).isEqualTo(-1);
    }

    /*
    To handle each granularity differently, a `TemporalHandler` receives the parsed value as the matching
    `java.time` type. `TemporalConsumer` is the same idea without a return value.
     */
    @Test
    void handleByGranularity()
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

        assertThat(ITU.parse("2017-12-06", handler).toString()).isEqualTo("2017-12-06T00:00Z");
        assertThat(ITU.parse("2017-12-06T10:15:30+02:00", handler).toString()).isEqualTo("2017-12-06T10:15:30+02:00");
    }

    /*
    A format that is not RFC-3339 can be described as a sequence of tokens. The parser produced is a
    `DateTimeParser`, the same interface the built-in ones implement.
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
    The built-in formats are available as `DateTimeParser` instances too, for code that takes a parser as a
    parameter: RFC-3339, local date and local time.
     */
    @Test
    void parseUsingInterface()
    {
        final DateTimeParser rfc3339 = DateTimeParsers.rfc3339();
        assertThat(rfc3339.parse("2000-12-31T23:59:37.123456Z").toString()).isEqualTo("2000-12-31T23:59:37.123456Z");

        final DateTimeParser localDate = DateTimeParsers.localDate();
        assertThat(localDate.parse("2013-12-24").toLocalDate()).isEqualTo(LocalDate.of(2013, 12, 24));

        final DateTimeParser localTime = DateTimeParsers.localTime();
        assertThat(localTime.parse("23:59:37.123456").toLocalTime()).isEqualTo(LocalTime.of(23, 59, 37, 123456000));
    }
}
