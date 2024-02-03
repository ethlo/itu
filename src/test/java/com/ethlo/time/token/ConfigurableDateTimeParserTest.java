package com.ethlo.time.token;

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

import static com.ethlo.time.Field.DAY;
import static com.ethlo.time.Field.HOUR;
import static com.ethlo.time.Field.MINUTE;
import static com.ethlo.time.Field.MONTH;
import static com.ethlo.time.Field.SECOND;
import static com.ethlo.time.Field.YEAR;
import static com.ethlo.time.token.DigitsToken.ofFour;
import static com.ethlo.time.token.DigitsToken.ofTwo;
import static com.ethlo.time.token.SeparatorToken.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParsePosition;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

import com.ethlo.time.DateTime;
import com.ethlo.time.ITU;

public class ConfigurableDateTimeParserTest
{
    @Test
    void parseCustomFormat()
    {
        final ParsePosition pos = new ParsePosition(0);
        final String input = "31-12-2000 235937,123456";
        final DateTimeParser parser = new ConfigurableDateTimeParser(
                ofTwo(DAY),
                separator('-'),
                ofTwo(MONTH),
                separator('-'),
                ofFour(YEAR),
                separator(' '),
                ofTwo(HOUR),
                ofTwo(MINUTE),
                ofTwo(SECOND),
                separator(','),
                new FractionsToken()
        );
        final DateTime result = parser.parse(input, pos);
        assertThat(result).isEqualTo(DateTime.of(2000, 12, 31, 23, 59, 37, 123456000, null, 6));
    }

    @Test
    void parseRfc3339Format()
    {
        final String input = "2023-01-01T23:38:34.987654321+06:00";
        final DateTime fixed = ITU.parseLenient(input);
        final ParsePosition pos = new ParsePosition(0);
        final DateTime custom = DateTimeParsers.rfc3339().parse(input, pos);
        assertThat(custom).isEqualTo(fixed);
        assertThat(fixed.toString()).isEqualTo(input);
        assertThat(custom.toString()).isEqualTo(input);
    }

    @Test
    void testInvalidSeparator()
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> new ConfigurableDateTimeParser(separator('X')).parse("12"));
        assertThat(exc).hasMessage("Unexpected character 1 at position 1: 12");
    }

    @Test
    void testEndOfTextSeparator()
    {
        final ParsePosition pos = new ParsePosition(0);
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, ()->separator('-').read("", pos));
        assertThat(exc).hasMessage("Unexpected end of input: ");
    }

    @Test
    void reachEndOfFractions()
    {
        final int value = new FractionsToken().read("123456X", new ParsePosition(0));
        assertThat(value).isEqualTo(123456);
    }

    @Test
    void readTimeZoneZuluUpper()
    {
        final ParsePosition pos = new ParsePosition(0);
        assertThat(new TimeZoneOffsetToken().read("Z", pos)).isEqualTo(0);
    }

    @Test
    void readTimeZoneZuluLower()
    {
        final ParsePosition pos = new ParsePosition(0);
        assertThat(new TimeZoneOffsetToken().read("z", pos)).isEqualTo(0);
    }

    @Test
    void readTimeZoneUnexpectedChar()
    {
        final ParsePosition pos = new ParsePosition(0);
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, ()->new TimeZoneOffsetToken().read("X", pos));
        assertThat(exc).hasMessage("Unexpected character X at position 1: X");
    }

    @Test
    void readTimeZoneTooShort()
    {
        final ParsePosition pos = new ParsePosition(0);
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, ()->new TimeZoneOffsetToken().read("-06:0", pos));
        assertThat(exc).hasMessage("Invalid timezone offset: -06:0");
    }

    @Test
    void readTimeZoneNegative()
    {
        final ParsePosition pos = new ParsePosition(0);
        final int secs = new TimeZoneOffsetToken().read("-06:30", pos);
        assertThat(secs).isEqualTo(-23400);
    }
}
