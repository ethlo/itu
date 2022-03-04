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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

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
        assertThat(w3cDateUtil.format(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.YEAR)).isEqualTo("2012");
    }

    @Test
    public void testFormatYearMonth()
    {
        assertThat(w3cDateUtil.format(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.MONTH)).isEqualTo("2012-01");
    }

    @Test
    public void testFormatYearMonthDay()
    {
        assertThat(w3cDateUtil.format(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.DAY)).isEqualTo("2012-01-14");
    }

    @Test
    public void testParseYearString()
    {
        final String s = "2012";
        final Year date = w3cDateUtil.parseLenient(s, Year.class);
        assertThat(date.getValue()).isEqualTo(2012);
    }

    @Test
    public void testParseYearStringLenient()
    {
        final String s = "2012";
        final Temporal date = w3cDateUtil.parseLenient(s);
        assertThat(date.get(ChronoField.YEAR)).isEqualTo(2012);
    }

    @Test
    public void testParseYearMonthString()
    {
        final String s = "2012-10";
        final YearMonth date = w3cDateUtil.parseLenient(s, YearMonth.class);
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonthValue()).isEqualTo(10);
    }

    @Test
    public void testParseYearMonthStringLenient()
    {
        final String s = "2012-10";
        final Temporal date = w3cDateUtil.parseLenient(s);
        assertThat(date.get(ChronoField.YEAR)).isEqualTo(2012);
        assertThat(date.get(ChronoField.MONTH_OF_YEAR)).isEqualTo(10);
    }

    @Test
    public void testParseDateString()
    {
        final String s = "2012-03-29";
        final LocalDate date = w3cDateUtil.parseLenient(s, LocalDate.class);
        assertThat(formatter.formatUtc(OffsetDateTime.of(date, LocalTime.MIN, ZoneOffset.UTC))).isEqualTo("2012-03-29T00:00:00Z");
    }

    @Test
    public void testParseBestEffort1DigitMinute()
    {
        final String s = "2012-03-29T23:1";
        Assertions.assertThrows(DateTimeException.class, () -> w3cDateUtil.parseLenient(s));
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
