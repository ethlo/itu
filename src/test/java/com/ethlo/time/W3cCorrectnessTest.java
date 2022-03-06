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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("CorrectnessTest")
public class W3cCorrectnessTest extends AbstractTest
{
    private W3cDateTimeUtil w3cDateUtil;

    @Test
    public void testParseEmptyString()
    {
        assertThrows(DateTimeException.class, () -> parser.parseDateTime(""));
    }

    @Test
    public void testFormatYear()
    {
        assertThat(w3cDateUtil.formatUtc(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.YEAR)).isEqualTo("2012");
    }

    @Test
    public void testFormatYearMonth()
    {
        assertThat(w3cDateUtil.formatUtc(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.MONTH)).isEqualTo("2012-01");
    }

    @Test
    public void testFormatYearMonthDay()
    {
        assertThat(w3cDateUtil.formatUtc(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.DAY)).isEqualTo("2012-01-14");
    }

    @Test
    public void testParseYear()
    {
        final DateTime date = w3cDateUtil.parse("2012");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getField()).isEqualTo(Field.YEAR);
    }

    @Test
    public void testParseYearMonth()
    {
        final DateTime date = w3cDateUtil.parse("2012-10");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getField()).isEqualTo(Field.MONTH);
    }

    @Test
    public void testParseDate()
    {
        final DateTime date = w3cDateUtil.parse("2012-03-29");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(3);
        assertThat(date.getDay()).isEqualTo(29);
        assertThat(date.getField()).isEqualTo(Field.DAY);
    }

    @Test
    public void testParseDateTime()
    {
        final DateTime date = w3cDateUtil.parse("2012-10-27T17:22:39+20:00");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getDay()).isEqualTo(27);
        assertThat(date.getHour()).isEqualTo(17);
        assertThat(date.getMinute()).isEqualTo(22);
        assertThat(date.getSecond()).isEqualTo(39);
        assertThat(date.getField()).isEqualTo(Field.SECOND);
        assertThat(date.getOffset()).isEqualTo(TimezoneOffset.ofHoursMinutes(20, 0));
    }

    @Test
    public void testParseDateTimeNoOffsetToLocalDateTime()
    {
        final DateTime date = w3cDateUtil.parse("2012-10-27T17:22:39");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getDay()).isEqualTo(27);
        assertThat(date.getHour()).isEqualTo(17);
        assertThat(date.getMinute()).isEqualTo(22);
        assertThat(date.getSecond()).isEqualTo(39);
        assertThat(date.getField()).isEqualTo(Field.SECOND);
        assertThat(date.getOffset()).isNull();
        final LocalDateTime localDateTime = date.toLocalDatetime();
        assertThat(localDateTime.getYear()).isEqualTo(2012);
        assertThat(localDateTime.getMonthValue()).isEqualTo(10);
        assertThat(localDateTime.getDayOfMonth()).isEqualTo(27);
        assertThat(localDateTime.getHour()).isEqualTo(17);
        assertThat(localDateTime.getMinute()).isEqualTo(22);
        assertThat(localDateTime.getSecond()).isEqualTo(39);
    }

    @Test
    public void testParseLenientWithTimeToLocalDate()
    {
        final LocalDate date = w3cDateUtil.parse("2012-10-27T17:22:39+20:00").toLocalDate();
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonthValue()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
    }

    @Test
    public void testParseLenientToLocalDate()
    {
        final LocalDate date = w3cDateUtil.parse("2012-10-27").toLocalDate();
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonthValue()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
    }

    @Test
    public void testParseLenientToLocalDateNoDays()
    {
        assertThrows(DateTimeException.class, () -> w3cDateUtil.parse("2012-10").toLocalDate());
    }

    @Test
    public void testParseBestEffort1DigitMinute()
    {
        Assertions.assertThrows(DateTimeException.class, () -> w3cDateUtil.parse("2012-03-29T23:1"));
    }

    @Test
    public void testParseNull()
    {
        assertThrows(NullPointerException.class, () -> parser.parseDateTime(null));
    }

    @Override
    protected Rfc3339 getParser()
    {
        final EthloITU retVal = new EthloITU();
        this.w3cDateUtil = retVal;
        return retVal;
    }

    @Override
    protected Rfc3339Formatter getFormatter()
    {
        return new EthloITU();
    }
}
