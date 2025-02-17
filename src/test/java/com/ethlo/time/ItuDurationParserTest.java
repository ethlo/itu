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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

class ItuDurationParserTest
{
    @Test
    void testMissingP()
    {
        assertThat(assertThrows(DateTimeParseException.class, () -> ItuDurationParser.parse("1D")))
                .hasMessage("Duration must start with 'P'");
    }

    @Test
    void testValidMinimal()
    {
        final Duration result = ItuDurationParser.parse("PT1S");
        assertThat(result.getSeconds()).isOne();
        assertThat(result.getNano()).isZero();
        assertThat(result.normalized()).isEqualTo("PT1S");
    }

    @Test
    void testValidFullNotNormalizedToNormalized()
    {
        assertThat(ItuDurationParser.parse("P4W10DT28H122M1.123456S").normalized()).isEqualTo("P5W4DT6H2M1.123456S");
    }

    @Test
    void shouldParseZeroDuration()
    {
        java.time.Duration duration = ItuDurationParser.parse("P0D").toDuration();
        assertThat(duration).isEqualTo(java.time.Duration.ZERO);
    }

    @Test
    void shouldParseFractionalSeconds()
    {
        // Input: PT1.123456S (1 second, 123456 microseconds)
        java.time.Duration duration = ItuDurationParser.parse("PT1.123456S").toDuration();
        assertThat(duration).isEqualTo(java.time.Duration.ofSeconds(1).plusNanos(123456000));
    }

    @Test
    void shouldParseDurationWithNoTimeSection()
    {
        // Input: P1Y2M3D (no time section)
        java.time.Duration duration = ItuDurationParser.parse("P30D").toDuration();
        assertThat(duration).isEqualTo(java.time.Duration.ofDays(30));
    }

    @Test
    void shouldThrowDateTimeParseExceptionForOverflow()
    {
        final String input = "P999999999999D";
        assertThatThrownBy(() -> ItuDurationParser.parse(input))
                .isInstanceOf(DateTimeParseException.class)
                .hasMessageContaining("Numeric overflow while parsing value");
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
        // Input: PT1.123000S (1 second, 123 milliseconds)
        java.time.Duration duration = ItuDurationParser.parse("PT1.123000S").toDuration();
        assertThat(duration).isEqualTo(java.time.Duration.ofSeconds(1).plusNanos(123000000));
    }

    @Test
    void shouldHandleMultipleFractionalDigits()
    {
        // Input: PT1.123456789S (1 second, 123456789 nanoseconds)
        java.time.Duration duration = ItuDurationParser.parse("PT1.123456789S").toDuration();
        assertThat(duration).isEqualTo(java.time.Duration.ofSeconds(1).plusNanos(123456789));
    }

    @Test
    void shouldParseNegativeDuration()
    {
        final Duration result = ItuDurationParser.parse("-P1W3DT4H5M6.50S");
        assertThat(result.getSeconds()).isEqualTo(-878707L);
        assertThat(result.getNano()).isEqualTo(500_000_000);
        assertThat(result.normalized()).isEqualTo("-P1W3DT4H5M6.5S");
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
        // Input: P1W2D (1 week, 2 days)
        java.time.Duration duration = ItuDurationParser.parse("P1W2D").toDuration();
        assertThat(duration).isEqualTo(java.time.Duration.ofDays(7 + 2)); // 7 days + 2 days
    }

    @Test
    void shouldParseDurationWithTimeOnly()
    {
        // Input: PT10H30M45S (10 hours, 30 minutes, 45 seconds)
        java.time.Duration duration = ItuDurationParser.parse("PT10H30M45S").toDuration();

        assertThat(duration).isEqualTo(java.time.Duration.ofHours(10).plusMinutes(30).plusSeconds(45));
    }

    @Test
    void shouldParseEmptyDurationAsZero()
    {
        // Input: P (zero duration)
        java.time.Duration duration = ItuDurationParser.parse("P").toDuration();
        assertThat(duration).isEqualTo(java.time.Duration.ZERO);
    }
}
