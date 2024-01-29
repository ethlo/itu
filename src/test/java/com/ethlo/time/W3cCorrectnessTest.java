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
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.ethlo.time.internal.EthloITU;
import com.ethlo.time.internal.Rfc3339;
import com.ethlo.time.internal.Rfc3339Formatter;
import com.ethlo.time.internal.W3cDateTimeUtil;

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
        final DateTime date = w3cDateUtil.parseLenient("2012");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMostGranularField()).isEqualTo(Field.YEAR);
        assertThat(date.toYear()).isEqualTo(Year.of(2012));
    }

    @Test
    public void testParseYearMonth()
    {
        final DateTime date = w3cDateUtil.parseLenient("2012-10");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getMostGranularField()).isEqualTo(Field.MONTH);
        final YearMonth yearMonth = w3cDateUtil.parseLenient("2012-10").toYearMonth();
        assertThat(yearMonth.getYear()).isEqualTo(2012);
        assertThat(yearMonth.getMonthValue()).isEqualTo(10);

        assertThrows(DateTimeException.class, () -> w3cDateUtil.parseLenient("2012").toYearMonth());
    }

    @Test
    public void testParseDate()
    {
        final String input = "2012-03-29";
        final DateTime date = w3cDateUtil.parseLenient(input);
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(3);
        assertThat(date.getDayOfMonth()).isEqualTo(29);
        assertThat(date.getMostGranularField()).isEqualTo(Field.DAY);
        assertThat(date.toString()).isEqualTo(input);
    }

    @Test
    public void testParseDateTimeNanos()
    {
        final String input = "2012-10-27T17:22:39.123456789+13:30";
        final DateTime date = w3cDateUtil.parseLenient(input);
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
        assertThat(date.getHour()).isEqualTo(17);
        assertThat(date.getMinute()).isEqualTo(22);
        assertThat(date.getSecond()).isEqualTo(39);
        assertThat(date.getNano()).isEqualTo(123456789);
        assertThat(date.getMostGranularField()).isEqualTo(Field.NANO);
        assertThat(date.getOffset()).isPresent();
        assertThat(date.getOffset().get().getHours()).isEqualTo(13);
        assertThat(date.getOffset().get().getMinutes()).isEqualTo(30);
        assertThat(date.getOffset()).hasValue(TimezoneOffset.ofHoursMinutes(13, 30));
        assertThat(date.toString(9)).isEqualTo(input);
    }

    @Test
    public void testParseDateTimeWithoutFractions()
    {
        final DateTime date = w3cDateUtil.parseLenient("2012-10-27T17:22:39+13:30");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
        assertThat(date.getHour()).isEqualTo(17);
        assertThat(date.getMinute()).isEqualTo(22);
        assertThat(date.getSecond()).isEqualTo(39);
        assertThat(date.getNano()).isEqualTo(0);
        assertThat(date.getMostGranularField()).isEqualTo(Field.SECOND);
        assertThat(date.getOffset()).isPresent();
        assertThat(date.getOffset().get().getHours()).isEqualTo(13);
        assertThat(date.getOffset().get().getMinutes()).isEqualTo(30);
        assertThat(date.getOffset()).hasValue(TimezoneOffset.ofHoursMinutes(13, 30));
    }

    @Test
    public void testTimezoneOffset()
    {
        final TimezoneOffset tz = TimezoneOffset.ofHoursMinutes(-17, -30);
        assertThat(tz.getHours()).isEqualTo(-17);
        assertThat(tz.getMinutes()).isEqualTo(-30);
    }

    @Test
    public void testOfZoneOffset()
    {
        final ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(-17, -33);
        final TimezoneOffset tz = TimezoneOffset.of(zoneOffset);
        assertThat(tz.getHours()).isEqualTo(-17);
        assertThat(tz.getMinutes()).isEqualTo(-33);
    }

    @Test
    public void testParseDateTimeWithoutSeconds()
    {
        final DateTime date = w3cDateUtil.parseLenient("2012-10-27T17:22Z");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
        assertThat(date.getHour()).isEqualTo(17);
        assertThat(date.getMinute()).isEqualTo(22);
        assertThat(date.getMostGranularField()).isEqualTo(Field.MINUTE);
        assertThat(date.getOffset()).hasValue(TimezoneOffset.UTC);
    }

    @Test
    public void testParseDateTimeWithoutSecondsAndTimezone()
    {
        final DateTime date = w3cDateUtil.parseLenient("2012-10-27T17:22");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
        assertThat(date.getHour()).isEqualTo(17);
        assertThat(date.getMinute()).isEqualTo(22);
        assertThat(date.getMostGranularField()).isEqualTo(Field.MINUTE);
        assertThat(date.getOffset()).isEmpty();

        final DateTimeException excOffsetDateTime = assertThrows(DateTimeException.class, date::toOffsetDatetime);
        assertThat(excOffsetDateTime).hasMessage("No timezone information: 2012-10-27T17:22");

        final LocalDateTime localDateTime = date.toLocalDatetime();
        assertThat(localDateTime.getYear()).isEqualTo(2012);
        assertThat(localDateTime.getMonthValue()).isEqualTo(10);
        assertThat(localDateTime.getDayOfMonth()).isEqualTo(27);
        assertThat(localDateTime.getHour()).isEqualTo(17);
        assertThat(localDateTime.getMinute()).isEqualTo(22);
    }

    @Test
    public void testParseDateTimeNoOffsetToLocalDateTime()
    {
        final DateTime date = w3cDateUtil.parseLenient("2012-10-27T17:22:39");
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonth()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
        assertThat(date.getHour()).isEqualTo(17);
        assertThat(date.getMinute()).isEqualTo(22);
        assertThat(date.getSecond()).isEqualTo(39);
        assertThat(date.getMostGranularField()).isEqualTo(Field.SECOND);
        assertThat(date.getOffset()).isEmpty();
        final LocalDateTime localDateTime = date.toLocalDatetime();
        assertThat(localDateTime.getYear()).isEqualTo(2012);
        assertThat(localDateTime.getMonthValue()).isEqualTo(10);
        assertThat(localDateTime.getDayOfMonth()).isEqualTo(27);
        assertThat(localDateTime.getHour()).isEqualTo(17);
        assertThat(localDateTime.getMinute()).isEqualTo(22);
        assertThat(localDateTime.getSecond()).isEqualTo(39);
    }

    @Test
    public void testConvertOffsetDateTimeToDateTime()
    {
        final OffsetDateTime input = OffsetDateTime.parse("2012-10-27T17:22:39+10:00");
        final DateTime dateTime = DateTime.of(input);
        assertThat(dateTime.toOffsetDatetime()).isEqualTo(input);
    }

    @Test
    public void testToOffsetDateTimeWithoutGranularEnoughData()
    {
        final DateTime dateTime = w3cDateUtil.parseLenient("2012-10-27");
        assertThrows(DateTimeException.class, dateTime::toOffsetDatetime);
    }

    @Test
    public void testParseLenientWithTimeToLocalDate()
    {
        final LocalDate date = w3cDateUtil.parseLenient("2012-10-27T17:22:39+10:00").toLocalDate();
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonthValue()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
    }

    @Test
    public void testParseLenientToLocalDate()
    {
        final LocalDate date = w3cDateUtil.parseLenient("2012-10-27").toLocalDate();
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonthValue()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
    }

    @Test
    public void testParseLenientToLocalDateNoDays()
    {
        assertThrows(DateTimeException.class, () -> w3cDateUtil.parseLenient("2012-10").toLocalDate());
    }

    @Test
    public void testParseBestEffort1DigitMinute()
    {
        Assertions.assertThrows(DateTimeException.class, () -> w3cDateUtil.parseLenient("2012-03-29T23:1"));
    }

    @Test
    public void testParseNull()
    {
        assertThrows(NullPointerException.class, () -> parser.parseDateTime(null));
    }

    @Override
    protected Rfc3339 getParser()
    {
        final EthloITU retVal = EthloITU.getInstance();
        this.w3cDateUtil = retVal;
        return retVal;
    }

    @Override
    protected Rfc3339Formatter getFormatter()
    {
        return EthloITU.getInstance();
    }
}
