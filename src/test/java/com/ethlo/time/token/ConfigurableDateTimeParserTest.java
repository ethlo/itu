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
import static com.ethlo.time.token.DateTimeTokens.digits;
import static com.ethlo.time.token.DateTimeTokens.fractions;
import static com.ethlo.time.token.DateTimeTokens.separators;
import static com.ethlo.time.token.DateTimeTokens.zoneOffset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParsePosition;
import java.time.format.DateTimeParseException;

import com.ethlo.time.DateTimeParser;
import com.ethlo.time.DateTimeParsers;

import org.junit.jupiter.api.Test;

import com.ethlo.time.DateTime;
import com.ethlo.time.ITU;
import com.ethlo.time.internal.token.FractionsToken;
import com.ethlo.time.internal.token.ZoneOffsetToken;

public class ConfigurableDateTimeParserTest
{
    private final DateTimeParser rfc3339Parser = DateTimeParsers.of(
            digits(YEAR, 4),
            separators('-'),
            digits(MONTH, 2),
            separators('-'),
            digits(DAY, 2),
            separators('T', 't', ' '),
            digits(HOUR, 2),
            separators(':'),
            digits(MINUTE, 2),
            separators(':'),
            digits(SECOND, 2),
            separators('.'),
            fractions(),
            zoneOffset()
    );

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
        final ParsePosition pos = new ParsePosition(0);
        final String input = "31-12-2000 235937,123456";
        final DateTime result = parser.parse(input, pos);
        assertThat(result).isEqualTo(DateTime.of(2000, 12, 31, 23, 59, 37, 123456000, null, 6));
    }

    @Test
    void duplicateField()
    {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> DateTimeParsers.of(
                        digits(HOUR, 2),
                        separators('a', 'z'),
                        digits(HOUR, 4),
                        separators('X')
                )
        );
        assertThat(exc).hasMessage("Duplicate field HOUR in list of tokens: [digits: HOUR(2), separators: [a, z], digits: HOUR(4), separator: X]");
    }

    @Test
    void parseRfc3339Format()
    {
        final DateTimeParser parser = DateTimeParsers.of(
                digits(YEAR, 4),
                separators('-'),
                digits(MONTH, 2),
                separators('-'),
                digits(DAY, 2),
                separators('T', 't'),
                digits(HOUR, 2),
                separators(':'),
                digits(MINUTE, 2),
                separators(':'),
                digits(SECOND, 2),
                separators('.'),
                fractions(),
                zoneOffset()
        );
        final String input = "2023-01-01T23:38:34.987654321+06:00";
        final DateTime fixed = ITU.parseLenient(input);

        final ParsePosition pos = new ParsePosition(0);
        final DateTime custom = parser.parse(input, pos);

        assertThat(custom).isEqualTo(fixed);
        assertThat(fixed.toString()).isEqualTo(input);
        assertThat(custom.toString()).isEqualTo(input);
    }

    @Test
    void testInvalidSeparators()
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> DateTimeParsers.of(separators('X')).parse("12"));
        assertThat(exc).hasMessage("Expected character [X] at position 1, found 1: 12");
    }

    @Test
    void testSeparators()
    {
        final ParsePosition position = new ParsePosition(0);
        final int result = separators('-', '_').read("_", position);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testInvalidSeparator()
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> DateTimeParsers.of(separators('X')).parse("12"));
        assertThat(exc).hasMessage("Expected character [X] at position 1, found 1: 12");
    }

    @Test
    void testEndOfTextSeparator()
    {
        final ParsePosition pos = new ParsePosition(0);
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> separators('-').read("", pos));
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
        assertThat(new ZoneOffsetToken().read("Z", pos)).isEqualTo(0);
    }

    @Test
    void readTimeZoneZuluLower()
    {
        final ParsePosition pos = new ParsePosition(0);
        assertThat(new ZoneOffsetToken().read("z", pos)).isEqualTo(0);
    }

    @Test
    void readTimeZoneUnexpectedChar()
    {
        final ParsePosition pos = new ParsePosition(0);
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> new ZoneOffsetToken().read("X", pos));
        assertThat(exc).hasMessage("Expected character [Z, z, +, -] at position 1, found X: X");
    }

    @Test
    void readTimeZoneTooShort()
    {
        final ParsePosition pos = new ParsePosition(0);
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> new ZoneOffsetToken().read("-06:0", pos));
        assertThat(exc).hasMessage("Invalid timezone offset: -06:0");
    }

    @Test
    void readTimeZoneNegative()
    {
        final ParsePosition pos = new ParsePosition(0);
        final int secs = new ZoneOffsetToken().read("-06:30", pos);
        assertThat(secs).isEqualTo(-23400);
    }

    @Test
    void testOffset()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String text = "2019-12-31T22:20:14.123+05:30";
        rfc3339Parser.parse("123456789," + text + ",something", pos);
        assertThat(pos.getIndex()).isEqualTo(10 + text.length());
    }

    @Test
    void testOffsetError()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String text = "2019-12-31T22:20X14.123+05:30";
        assertThrows(DateTimeParseException.class, () -> rfc3339Parser.parse("123456789," + text + ",something", pos));
        assertThat(pos.getIndex()).isEqualTo(26);
        assertThat(pos.getErrorIndex()).isEqualTo(26);
    }
}
