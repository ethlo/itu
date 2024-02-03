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

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

public class ParsePositionTest
{
    @Test
    void testParseUnparseable()
    {
        final ParsePosition pos = new ParsePosition(0);
        assertThrows(DateTimeParseException.class, () -> ITU.parseLenient("1999-11-22|11:22:1", ParseConfig.DEFAULT, pos));
        assertThat(pos.getErrorIndex()).isEqualTo(10);
        assertThat(pos.getIndex()).isEqualTo(10);
    }

    @Test
    void testParsePositionYear()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "123456789,1999";
        ITU.parseLenient(input, ParseConfig.STRICT, pos);
        assertThat(pos.getIndex()).isEqualTo(input.length());
    }

    @Test
    void testParsePositionMonth()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "123456789,1999-11";
        ITU.parseLenient(input, ParseConfig.STRICT, pos);
        assertThat(pos.getIndex()).isEqualTo(input.length());
    }

    @Test
    void testParsePositionDay()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "123456789,1999-11-22";
        ITU.parseLenient(input, ParseConfig.STRICT, pos);
        assertThat(pos.getIndex()).isEqualTo(input.length());
    }

    @Test
    void testParsePositionMinute()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "123456789,1999-11-22T11:22";
        ITU.parseLenient(input, ParseConfig.STRICT, pos);
        assertThat(pos.getIndex()).isEqualTo(input.length());
    }

    @Test
    void testParsePositionMinuteUnparsable()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "123456789,1999-11-22T11:2x";
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(input, ParseConfig.STRICT, pos));
        assertThat(pos.getIndex()).isEqualTo(input.length() - 1);
        assertThat(pos.getErrorIndex()).isEqualTo(input.length() - 1);
        assertThat(exc.getErrorIndex()).isEqualTo(input.length() - 1);
    }

    @Test
    void testParsePositionSecond()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "123456789,1999-11-22T11:22:11";
        ITU.parseLenient(input, ParseConfig.STRICT, pos);
        assertThat(pos.getIndex()).isEqualTo(input.length());
    }

    @Test
    void testParsePositionSecondUnparsable()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "123456789,1999-11-22T11:22:3x";
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(input, ParseConfig.STRICT, pos));
        assertThat(pos.getErrorIndex()).isEqualTo(input.length() - 1);
        assertThat(exc.getErrorIndex()).isEqualTo(input.length() - 1);
    }

    @Test
    void testParsePositionMillis()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "123456789,1999-11-22T11:22:12.123";
        ITU.parseLenient(input, ParseConfig.STRICT, pos);
        assertThat(pos.getIndex()).isEqualTo(input.length());
    }

    @Test
    void testParsePositionDateTime()
    {
        final ParsePosition pos = new ParsePosition(0);
        final String input = "1999-11-22T11:22:17.191Z";
        ITU.parseDateTime(input, pos);
        assertThat(pos.getIndex()).isEqualTo(input.length());
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

    @Test
    void testParsePositionNotZeroDateTimeValidWithMillis()
    {
        final ParsePosition pos = new ParsePosition(8);
        ITU.parseDateTime("1234567,1999-11-22T11:22:17.191Z,some-other-data", pos);
        assertThat(pos.getIndex()).isEqualTo(32);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
    }

    @Test
    void testParsePositionNotZeroDateTimeValidZeroOffsetWithMillis()
    {
        final ParsePosition pos = new ParsePosition(8);
        ITU.parseDateTime("1234567,1999-11-22T11:22:17.191+00:00,some-other-data", pos);
        assertThat(pos.getIndex()).isEqualTo(37);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
    }

    @Test
    void testParsePositionNotZeroDateTimeValidNonZuluOffsetWithMillis()
    {
        final ParsePosition pos = new ParsePosition(8);
        ITU.parseDateTime("1234567,1999-11-22T11:22:17.191+05:00,some-other-data", pos);
        assertThat(pos.getIndex()).isEqualTo(37);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
    }

    @Test
    void testParsePositionNotZeroDateTimeValidNonZuluOffsetWithSecond()
    {
        final ParsePosition pos = new ParsePosition(8);
        ITU.parseDateTime("1234567,1999-11-22T11:22:17+05:00,some-other-data", pos);
        assertThat(pos.getIndex()).isEqualTo(33);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
    }

    @Test
    void testParsePositionNotZeroDateTimeValidNonZuluOffsetWithMinute()
    {
        final ParsePosition pos = new ParsePosition(8);
        ITU.parseDateTime("1234567,1999-11-22T11:22+05:00,some-other-data", pos);
        assertThat(pos.getIndex()).isEqualTo(30);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
    }

    @Test
    void testParsePositionRfc3339Zulu()
    {
        final ParsePosition pos = new ParsePosition(8);
        ITU.parseDateTime("1234567,1999-11-22T11:22:00Z,some-other-data", pos);
        assertThat(pos.getIndex()).isEqualTo(28);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
    }

    @Test
    void testParsePositionRfc3339Offset()
    {
        final ParsePosition pos = new ParsePosition(10);
        final String input = "some-data,1999-11-22T11:22:00+05:30,some-other-data";
        ITU.parseDateTime(input, pos);
        assertThat(pos.getIndex()).isEqualTo(35);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
        assertThat(input.substring(pos.getIndex())).isEqualTo(",some-other-data");
    }

    @Test
    void testParsePositionSubsequent()
    {
        final ParsePosition pos = new ParsePosition(4);
        final String input = "abc,2004-11-21T00:00Z1999-11-22T11:22+05:00,some-other-data";

        ITU.parseDateTime(input, pos);
        assertThat(pos.getIndex()).isEqualTo(21);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);

        ITU.parseDateTime(input, pos);
        assertThat(pos.getIndex()).isEqualTo(43);
        assertThat(pos.getErrorIndex()).isEqualTo(-1);
    }

    @Test
    void testParseOutOfBoundsPosition()
    {
        final ParsePosition pos = new ParsePosition(40);
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseDateTime("123", pos));
    }

    @Test
    void testParseOutOfBoundsPositionNegative()
    {
        final ParsePosition pos = new ParsePosition(-3);
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseDateTime("123", pos));
    }
}
