package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2025 Morten Haraldsen @ethlo
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

import com.ethlo.time.internal.ItuDurationParser;

import org.junit.jupiter.api.Test;

import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItuDurationParserTest
{
    @Test
    void testMissingP()
    {
        assertThat(assertThrows(DateTimeParseException.class, () -> ItuDurationParser.parse("1D")))
                .hasMessage("Duration must start with 'P': 1D");
    }

    @Test
    void testHighestValue()
    {
        final String input = "PT" + Long.MAX_VALUE + ".999999999S";
        final Duration result = ITU.parseDuration(input);
        assertThat(result.getSeconds()).isEqualTo(Long.MAX_VALUE);
        assertThat(result.getNanos()).isEqualTo(999_999_999);
    }

    @Test
    void testLowestValue()
    {
        final String input = "-PT9223372036854775807S";
        final Duration result = ITU.parseDuration(input);
        assertThat(result.getSeconds()).isEqualTo(-Long.MAX_VALUE);
        assertThat(result.getNanos()).isEqualTo(0L);
    }

    @Test
    void testValidMinimal()
    {
        final Duration result = ItuDurationParser.parse("PT1S");
        assertThat(result.getSeconds()).isOne();
        assertThat(result.getNanos()).isZero();
        assertThat(result.normalized()).isEqualTo("PT1S");
    }

    @Test
    void testValidFullNotNormalizedToNormalized()
    {
        assertThat(ItuDurationParser.parse("P4W10DT28H122M1.123456S").normalized()).isEqualTo("P5W4DT6H2M1.123456S");
    }

    @Test
    void testValidFullNotNormalizedToNormalizedNegative()
    {
        assertThat(ItuDurationParser.parse("-P4W10DT28H122M1.123456S").normalized()).isEqualTo("-P5W4DT6H2M1.123456S");
    }

    @Test
    void testEmpty()
    {
        assertThrows(DateTimeParseException.class, () -> ItuDurationParser.parse(""));
    }

    @Test
    void testNull()
    {
        assertThrows(NullPointerException.class, () -> ItuDurationParser.parse(null));
    }

    @Test
    void testNothingAfterDot()
    {
        assertThrows(DateTimeParseException.class, () -> ItuDurationParser.parse("PT1."));
    }

    @Test
    void testNoUnitAfterFractions()
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ItuDurationParser.parse("PT1.5"));
        assertThat(exc).hasMessage("No unit defined for value 1.5: PT1.5");
    }

    @Test
    void testWeirdInputs()
    {
        final String[] inputs = {"-PTHHHH6.2S.6S2.62.2S2.0S.", "PT..0.S.", "PT1SP"};
        for (String input : inputs)
        {
            final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseDuration(input));
            System.out.println(exc.getMessage());
        }
    }

    @Test
    void testWrongOrder()
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseDuration("PT1S1H"));
        assertThat(exc).hasMessage("Units must be in order from largest to smallest: PT1S1H");
    }

    @Test
    void testParsingFromOffset()
    {
        final Duration duration = ItuDurationParser.parse("some,data,here,PT1.123456S", 15);
        assertThat(duration).isEqualTo(Duration.ofSeconds(1).plusNanos(123456000));
    }

    @Test
    void shouldParseZeroDuration()
    {
        final Duration duration = ItuDurationParser.parse("P0D");
        assertThat(duration).isEqualTo(Duration.ZERO);
        assertThat(duration.normalized()).isEqualTo("PT0S");
    }

    @Test
    void shouldParseFractionalSeconds()
    {
        // Input: PT1.123456S (1 second, 123456 microseconds)
        final Duration duration = ItuDurationParser.parse("PT1.123456S");
        assertThat(duration).isEqualTo(Duration.ofSeconds(1).plusNanos(123456000));
    }

    @Test
    void shouldParseDurationWithNoTimeSection()
    {
        // Input: P1Y2M3D (no time section)
        final Duration duration = ItuDurationParser.parse("P30D");
        assertThat(duration).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void shouldThrowDateTimeParseExceptionForOverflow()
    {
        final String input = "P20D999999999999999999999H";
        assertThatThrownBy(() -> ItuDurationParser.parse(input))
                .isInstanceOf(DateTimeParseException.class)
                .hasMessageContaining("Expression is too large: " + input);
    }

    @Test
    void shouldThrowDateTimeParseExceptionForInvalidUnits()
    {
        // Input: P1Y2X3D (invalid unit X)
        assertThatThrownBy(() -> ItuDurationParser.parse("P1D2X3D"))
                .isInstanceOf(DateTimeParseException.class)
                .hasMessageContaining("Invalid unit: X");
    }

    @Test
    void shouldParseFractionalSecondsWithoutTrailingZeros()
    {
        final Duration duration = ItuDurationParser.parse("PT1.123000S");
        assertThat(duration).isEqualTo(Duration.ofSeconds(1).plusNanos(123000000));
    }

    @Test
    void shouldHandleMultipleFractionalDigits()
    {
        final Duration duration = ItuDurationParser.parse("PT1.123456789S");
        assertThat(duration).isEqualTo(Duration.ofSeconds(1).plusNanos(123456789));
    }

    @Test
    void shouldParseNegativeDuration()
    {
        final Duration result = ItuDurationParser.parse("-P1W3DT4H5M6.50S");
        assertThat(result.getSeconds()).isEqualTo(-878707L);
        assertThat(result.getNanos()).isEqualTo(500_000_000);
        assertThat(result.normalized()).isEqualTo("-P1W3DT4H5M6.5S");
    }

    @Test
    void testSimpleNegativeDuration()
    {
        final Duration d = ItuDurationParser.parse("-PT7.0S");
        assertThat(d.getSeconds()).isEqualTo(-7);
        assertThat(d.getNanos()).isZero();
        assertThat(d.normalized()).isEqualTo("-PT7S");
    }

    @Test
    void shouldThrowDateTimeParseExceptionForMalformedDuration()
    {
        // Input: P1Y2D3 (missing "T" between date and time section)
        assertThatThrownBy(() -> ItuDurationParser.parse("P1D3S"))
                .isInstanceOf(DateTimeParseException.class)
                .hasMessageContaining("'S' (seconds) must appear after 'T' in the duration");
    }

    @Test
    void shouldParseDurationWithWeeks()
    {
        final Duration duration = ItuDurationParser.parse("P1W2D");
        assertThat(duration).isEqualTo(Duration.ofDays(7 + 2));
    }

    @Test
    void shouldParseDurationWithTimeOnly()
    {
        final Duration duration = ItuDurationParser.parse("PT10H30M45S");
        assertThat(duration).isEqualTo(Duration.ofHours(10).plusMinutes(30).plusSeconds(45));
    }

    @Test
    void parse0DDurationAsZero()
    {
        final Duration duration = ItuDurationParser.parse("P0D");
        assertThat(duration).isEqualTo(Duration.ZERO);
    }
}