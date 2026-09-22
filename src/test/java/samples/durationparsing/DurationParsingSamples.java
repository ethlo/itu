package samples.durationparsing;

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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

import com.ethlo.time.Duration;
import com.ethlo.time.DurationUnit;
import com.ethlo.time.ITU;

/*

### Examples

 */
class DurationParsingSamples
{
    /*
    A duration parses to seconds and a nanosecond part, exact to the nanosecond.
     */
    @Test
    void parseDuration()
    {
        final Duration duration = ITU.parseDuration("P2DT3H4M5.678901234S");
        assertThat(duration.getSeconds()).isEqualTo(2 * 86_400 + 3 * 3_600 + 4 * 60 + 5);
        assertThat(duration.getNanos()).isEqualTo(678_901_234);
    }

    /*
    A negative duration is written with the sign in front of `P`. The sign lives in the seconds and the nanosecond
    part is never negative, as in `java.time.Duration`, so `-PT0.5S` is -1 second plus 500 million nanoseconds.
     */
    @Test
    void parseNegativeDuration()
    {
        final Duration duration = ITU.parseDuration("-PT0.5S");
        assertThat(duration.getSeconds()).isEqualTo(-1);
        assertThat(duration.getNanos()).isEqualTo(500_000_000);
        assertThat(duration.normalized()).isEqualTo("-PT0.5S");
    }

    /*
    `normalized()` renders with the largest units possible, so overflowing components are carried: 28 hours
    become a day and 4 hours. Trailing zero fractions are dropped.
     */
    @Test
    void normalized()
    {
        final Duration duration = ITU.parseDuration("P4W10DT28H122M1.123456S");
        assertThat(duration.normalized()).isEqualTo("P5W4DT6H2M1.123456S");
        assertThat(ITU.parseDuration("PT90M").normalized()).isEqualTo("PT1H30M");
    }

    /*
    `java.time.Duration.parse` does not accept weeks, so cap the largest unit at days (or lower) when the output
    has to be read back by the JDK.
     */
    @Test
    void normalizedWithMaximumUnit()
    {
        final Duration duration = ITU.parseDuration("P5W4DT6H2M1.123456S");
        assertThat(duration.normalized(DurationUnit.DAYS)).isEqualTo("P39DT6H2M1.123456S");
        assertThat(duration.normalized(DurationUnit.HOURS)).isEqualTo("PT942H2M1.123456S");
        assertThat(java.time.Duration.parse(duration.normalized(DurationUnit.DAYS)).getSeconds()).isEqualTo(duration.getSeconds());
    }

    /*
    Durations can be built, added, subtracted and compared, and placed on the timeline from an `Instant`.
     */
    @Test
    void arithmetic()
    {
        final Duration total = Duration.ofHours(2).add(ITU.parseDuration("PT30M")).subtract(Duration.ofSeconds(1));
        assertThat(total.normalized()).isEqualTo("PT2H29M59S");
        assertThat(total.negate().normalized()).isEqualTo("-PT2H29M59S");
        assertThat(total).isLessThan(Duration.ofHours(3));

        final Instant start = Instant.parse("2024-02-28T23:00:00Z");
        assertThat(total.timeline(start)).hasToString("2024-02-29T01:29:59Z");
    }

    /*
    `normalized` can also write into a `char[]` or `byte[]` at an offset, allocating nothing; the buffer needs
    room for `Duration.MAX_NORMALIZED_LENGTH` characters from the offset.
     */
    @Test
    void normalizedIntoBuffer()
    {
        final Duration duration = ITU.parseDuration("P5W4DT6H2M1.123456S");
        final byte[] bytes = new byte[Duration.MAX_NORMALIZED_LENGTH];
        final int length = duration.normalized(DurationUnit.DAYS, bytes, 0);
        assertThat(new String(bytes, 0, length, StandardCharsets.US_ASCII)).isEqualTo("P39DT6H2M1.123456S");
    }

    /*
    Years and months are rejected because their length depends on the calendar. As for date-times, the error
    is a `DateTimeParseException` with the 0-based index of the offending character.
     */
    @Test
    void parseDurationError()
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseDuration("P1Y2M"));
        assertThat(exc.getErrorIndex()).isEqualTo(2);
        assertThat(exc.getMessage()).isEqualTo("Invalid unit: Y: P1Y2M");
    }
}
