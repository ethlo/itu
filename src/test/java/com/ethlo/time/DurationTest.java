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

import static com.ethlo.time.Duration.NANOS_PER_SECOND;
import static com.ethlo.time.Duration.SECONDS_PER_DAY;
import static com.ethlo.time.Duration.SECONDS_PER_HOUR;
import static com.ethlo.time.Duration.SECONDS_PER_MINUTE;
import static com.ethlo.time.Duration.SECONDS_PER_WEEK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class DurationTest
{

    @Test
    void testAddPositiveValues()
    {
        Duration d1 = new Duration(5, 500_000_000); // 5 seconds, 500 ms
        Duration d2 = new Duration(3, 800_000_000); // 3 seconds, 800 ms

        Duration result = d1.add(d2);

        // Expecting 8 seconds, 300 ms
        assertThat(result.getSeconds()).isEqualTo(9);
        assertThat(result.getNano()).isEqualTo(300_000_000);
    }

    @Test
    void testPositiveNormalized()
    {
        final Duration d1 = Duration.ofMillis(4_900);
        assertThat(d1.getSeconds()).isEqualTo(4);
        assertThat(d1.getNano()).isEqualTo(900_000_000);

        final Duration d2 = new Duration(4, 900_000_000);
        assertThat(d1).isEqualTo(d2);

        assertThat(d2.normalized()).isEqualTo("PT4.9S");
    }

    @Test
    void testNegativeNormalized()
    {
        final Duration d1 = Duration.ofMillis(-4_900);
        assertThat(d1.getSeconds()).isEqualTo(-5);
        assertThat(d1.getNano()).isEqualTo(100_000_000);

        // Represents -4.9 seconds (correctly normalized)
        final Duration d2 = new Duration(-5, 100_000_000);
        assertThat(d1).isEqualTo(d2);
        assertThat(d2.normalized()).isEqualTo("-PT4.9S");
    }

    @Test
    void testAddNegativeValues()
    {
        final Duration d1 = Duration.ofMillis(5_900);
        final Duration d2 = Duration.ofMillis(-4_900);
        final Duration result = d1.add(d2);
        assertThat(result.getSeconds()).isEqualTo(1); // 5.9 -4.9 = 1.0 seconds
        assertThat(result.getNano()).isEqualTo(0);
    }

    @Test
    void testAddOverflow()
    {
        final Duration d1 = new Duration(Long.MAX_VALUE, 0);
        final Duration d2 = new Duration(1, 0);
        // Expecting ArithmeticException due to overflow
        assertThatThrownBy(() -> d1.add(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    void testAddUnderflow()
    {
        Duration d1 = new Duration(Long.MIN_VALUE, 0);
        Duration d2 = new Duration(-1, 0);

        // Expecting ArithmeticException due to underflow
        assertThatThrownBy(() -> d1.add(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    void testSubtractPositiveValues()
    {
        Duration d1 = Duration.ofMillis(5_500);
        Duration d2 = Duration.ofMillis(3_800);

        Duration result = d1.subtract(d2);

        // Expecting 1 second, -300 ms
        assertThat(result.getSeconds()).isEqualTo(1);
        assertThat(result.getNano()).isEqualTo(700_000_000);
    }

    @Test
    void testOfNanosPositiveMax()
    {
        final Duration result = Duration.ofNanos(Long.MAX_VALUE);
        assertThat(result.getSeconds()).isEqualTo(9_223_372_036L);
        assertThat(result.getNano()).isEqualTo(854_775_807);
    }

    @Test
    void testOfNanosNegativeMax()
    {
        final Duration result = Duration.ofNanos(Long.MIN_VALUE);
        assertThat(result.getSeconds()).isEqualTo(-9_223_372_037L);
        assertThat(result.getNano()).isEqualTo(145_224_192);
    }

    @Test
    void testSubtractNegativeValues()
    {
        final Duration d1 = Duration.ofMillis(5_500);
        final Duration d2 = Duration.ofMillis(-5_300);
        final Duration result = d1.subtract(d2);
        assertThat(result.normalized()).isEqualTo("PT10.8S");
    }

    @Test
    void testSubtractOverflow()
    {
        final Duration d1 = new Duration(Long.MIN_VALUE, 0);
        final Duration d2 = new Duration(1, 0);
        // Expecting ArithmeticException due to overflow
        assertThatThrownBy(() -> d1.subtract(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    void testSubtractUnderflow()
    {
        final Duration d1 = new Duration(Long.MAX_VALUE, 0);
        final Duration d2 = new Duration(-1, 0);
        // Expecting ArithmeticException due to underflow
        assertThatThrownBy(() -> d1.subtract(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    void testNegatePositiveDuration()
    {
        final Duration d = Duration.ofMillis(5_500);
        final Duration result = d.negate();
        // Negation of 5.5 sec is -5.5 sec, represented as (-6, 500_000_000)
        assertThat(result.getSeconds()).isEqualTo(-6);
        assertThat(result.getNano()).isEqualTo(500_000_000);
    }

    @Test
    void testNegateNegativeDuration()
    {
        final Duration d = Duration.ofMillis(-5_400);
        final Duration result = d.negate();
        assertThat(result.getSeconds()).isEqualTo(5);
        assertThat(result.getNano()).isEqualTo(400_000_000);
    }

    @Test
    void testNegateZeroDuration()
    {
        Duration d = new Duration(0, 0);
        Duration neg = d.negate();
        assertThat(neg.getSeconds()).isEqualTo(0);
        assertThat(neg.getNano()).isEqualTo(0);
    }

    @Test
    void testNegateWholePositiveDuration()
    {
        final Duration d = new Duration(5, 0);
        final Duration result = d.negate();
        assertThat(result.getSeconds()).isEqualTo(-5);
        assertThat(result.getNano()).isEqualTo(0);
    }

    @Test
    void testNegateWholeNegativeDuration()
    {
        final Duration d = new Duration(-5, 0);
        final Duration result = d.negate();
        assertThat(result.getSeconds()).isEqualTo(5);
        assertThat(result.getNano()).isEqualTo(0);
    }

    @Test
    void testNegateOverflow()
    {
        // Negating Long.MIN_VALUE is not representable.
        Duration d = new Duration(Long.MIN_VALUE, 0);
        assertThatThrownBy(d::negate)
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    void testAddSimple()
    {
        final Duration d1 = Duration.ofMillis(5_600);
        final Duration d2 = Duration.ofMillis(3_500);
        final Duration result = d1.add(d2);
        assertThat(result.getSeconds()).isEqualTo(9);
        assertThat(result.getNano()).isEqualTo(100_000_000);
    }

    @Test
    void testAddWithCarry()
    {
        final Duration d1 = Duration.ofMillis(2_800);
        final Duration d2 = Duration.ofMillis(3_400);
        final Duration result = d1.add(d2);
        assertThat(result.getSeconds()).isEqualTo(6);
        assertThat(result.getNano()).isEqualTo(200_000_000);
    }

    @Test
    void testAddNegativeDuration()
    {
        final Duration d1 = Duration.ofMillis(5_200);
        final Duration d2 = Duration.ofMillis(-2_200);
        final Duration result = d1.add(d2);
        assertThat(result.normalized()).isEqualTo("PT3S");
    }

    @Test
    void testAddWithOverflow()
    {
        final Duration d1 = new Duration(Long.MAX_VALUE, 0);
        final Duration d2 = new Duration(1, 0);
        assertThatThrownBy(() -> d1.add(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("long overflow");
    }

    @Test
    void testSubtractSimple()
    {
        Duration d1 = new Duration(7, 800_000_000);
        Duration d2 = new Duration(3, 500_000_000);
        Duration result = d1.subtract(d2);

        assertThat(result.getSeconds()).isEqualTo(4);
        assertThat(result.getNano()).isEqualTo(300_000_000);
    }

    @Test
    void futurePositive()
    {
        final Instant now = Instant.ofEpochSecond(0, 0);
        final Duration d1 = new Duration(7, 800_000_000);
        final Instant result = d1.future(now);
        assertThat(result.getEpochSecond()).isEqualTo(7);
        assertThat(result.getNano()).isEqualTo(800_000_000);
    }

    @Test
    void futureNegative()
    {
        final Instant now = Instant.ofEpochSecond(0, 0);
        final Duration d1 = Duration.ofMillis(-7_800);
        final Instant result = d1.future(now);
        assertThat(result.getEpochSecond()).isEqualTo(-8);
        assertThat(result.getNano()).isEqualTo(200_000_000);
    }

    @Test
    void pastPositive()
    {
        final Instant now = Instant.ofEpochSecond(0, 0);
        final Duration d1 = Duration.ofMillis(7_800);
        final Instant result = d1.past(now);
        assertThat(result.getEpochSecond()).isEqualTo(-8);
        assertThat(result.getNano()).isEqualTo(200_000_000);
    }

    @Test
    void pastNegative()
    {
        final Instant now = Instant.ofEpochSecond(0, 0);
        final Duration d1 = Duration.ofMillis(-7_800);
        final Instant result = d1.past(now);
        assertThat(result.getEpochSecond()).isEqualTo(7);
        assertThat(result.getNano()).isEqualTo(800_000_000);
    }

    @Test
    void testSubtractWithBorrow()
    {
        final Duration d1 = Duration.ofMillis(5_200);
        final Duration d2 = Duration.ofMillis(3_500);
        final Duration result = d1.subtract(d2);
        assertThat(result.normalized()).isEqualTo("PT1.7S");
    }

    @Test
    void testSubtractNegativeDuration()
    {
        final Duration d1 = Duration.ofMillis(-5_400);
        final Duration d2 = Duration.ofMillis(-5_800);
        final Duration result = d1.subtract(d2);
        assertThat(result.normalized()).isEqualTo("PT0.4S");
    }

    @Test
    void testSubtractWithUnderflow()
    {
        final Duration d1 = new Duration(Long.MIN_VALUE, 0);
        final Duration d2 = new Duration(1, 0);
        assertThatThrownBy(() -> d1.subtract(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("long overflow");
    }

    @Test
    void ofMillis()
    {
        assertThat(Duration.ofMillis(100)).isEqualTo(new Duration(0, 100_000_000));
    }

    @Test
    void ofMicros()
    {
        assertThat(Duration.ofMicros(100)).isEqualTo(new Duration(0, 100_000));
    }

    @Test
    void ofNanos()
    {
        assertThat(Duration.ofNanos(100)).isEqualTo(new Duration(0, 100));
    }

    @Test
    void ofWeeks()
    {
        assertThat(Duration.ofWeeks(4)).isEqualTo(new Duration(SECONDS_PER_WEEK * 4, 0));
    }

    @Test
    void ofDays()
    {
        assertThat(Duration.ofDays(22)).isEqualTo(new Duration(SECONDS_PER_DAY * 22, 0));
    }

    @Test
    void ofHours()
    {
        assertThat(Duration.ofHours(4)).isEqualTo(new Duration(SECONDS_PER_HOUR * 4, 0));
    }

    @Test
    void ofMinutes()
    {
        assertThat(Duration.ofMinutes(122)).isEqualTo(new Duration(SECONDS_PER_MINUTE * 122, 0));
    }

    @Test
    void ofSeconds()
    {
        assertThat(Duration.ofSeconds(7894)).isEqualTo(new Duration(7894, 0));
    }

    @Test
    void past()
    {
        assertThat(Duration.ofNanos(500).past()).isInstanceOf(Instant.class);
    }

    @Test
    void future()
    {
        assertThat(Duration.ofNanos(500).future()).isInstanceOf(Instant.class);
    }

    @Test
    void toDuration()
    {
        final java.time.Duration result = Duration.ofNanos(-500).toDuration();
        assertThat(result.getSeconds()).isEqualTo(-1);
        assertThat(result.getNano()).isEqualTo(999_999_500);
    }

    @Test
    void testToString()
    {
        assertThat(Duration.ofSeconds(1000)).hasToString("PT16M40S");
    }

    @Test
    void testEquals()
    {
        final Duration d1 = Duration.ofMillis(2_200);
        final Duration d2 = Duration.ofMillis(2_200);
        final Duration d3 = Duration.ofMillis(-2_200);
        assertThat(d1).isEqualTo(d1)
                .isEqualTo(d2)
                .isNotEqualTo(d3);
        assertThat(d2).isNotEqualTo(d3);
    }

    @Test
    void testHashcode()
    {
        final Duration d1 = Duration.ofMillis(2_200);
        final Duration d2 = Duration.ofMillis(2_200);
        final Duration d3 = Duration.ofMillis(-2_200);
        assertThat(d1).hasSameHashCodeAs(d1)
                .hasSameHashCodeAs(d2)
                .doesNotHaveSameHashCodeAs(d3);
    }

    @Test
    void testNegativeNanos()
    {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> new Duration(0, -1));
        assertThat(exc).hasMessageContaining("nanos cannot be negative");
    }

    @Test
    void testNanoAt1Billion()
    {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> new Duration(0, NANOS_PER_SECOND));
        assertThat(exc).hasMessageContaining("nanos cannot be larger than 999,999,999");
    }
}
