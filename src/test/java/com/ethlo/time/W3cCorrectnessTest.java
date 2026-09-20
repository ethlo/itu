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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Formatting to, and conversion from, the W3C date-time granularities. Parsing them is covered by
 * {@code date-time-corpus.json}.
 */
@Tag("CorrectnessTest")
public class W3cCorrectnessTest
{
    @Test
    public void testFormatYear()
    {
        assertThat(ITU.formatUtc(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.YEAR)).isEqualTo("2012");
    }

    @Test
    public void testFormatYearMonth()
    {
        assertThat(ITU.formatUtc(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.MONTH)).isEqualTo("2012-01");
    }

    @Test
    public void testFormatYearMonthDay()
    {
        assertThat(ITU.formatUtc(OffsetDateTime.parse("2012-01-14T12:34:56Z"), Field.DAY)).isEqualTo("2012-01-14");
    }

    @Test
    public void testToYear()
    {
        assertThat(ITU.parseLenient("2012").toYear()).isEqualTo(Year.of(2012));
    }

    @Test
    public void testToYearMonth()
    {
        final YearMonth yearMonth = ITU.parseLenient("2012-10").toYearMonth();
        assertThat(yearMonth.getYear()).isEqualTo(2012);
        assertThat(yearMonth.getMonthValue()).isEqualTo(10);

        assertThrows(DateTimeException.class, () -> ITU.parseLenient("2012").toYearMonth());
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
    public void testToLocalDateTimeWithoutSecondsAndTimezone()
    {
        final DateTime date = ITU.parseLenient("2012-10-27T17:22");

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
    public void testToLocalDateTime()
    {
        final LocalDateTime localDateTime = ITU.parseLenient("2012-10-27T17:22:39").toLocalDatetime();
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
        final DateTime dateTime = ITU.parseLenient("2012-10-27");
        assertThrows(DateTimeException.class, dateTime::toOffsetDatetime);
    }

    @Test
    public void testToLocalDateWithTime()
    {
        final LocalDate date = ITU.parseLenient("2012-10-27T17:22:39+10:00").toLocalDate();
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonthValue()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
    }

    @Test
    public void testToLocalDate()
    {
        final LocalDate date = ITU.parseLenient("2012-10-27").toLocalDate();
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonthValue()).isEqualTo(10);
        assertThat(date.getDayOfMonth()).isEqualTo(27);
    }

    @Test
    public void testToLocalDateNoDays()
    {
        assertThrows(DateTimeException.class, () -> ITU.parseLenient("2012-10").toLocalDate());
    }
}
