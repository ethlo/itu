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
        final Duration d1 = ItuDurationParser.parse("PT4.9S");
        assertThat(d1.getSeconds()).isEqualTo(4);
        assertThat(d1.getNano()).isEqualTo(900_000_000);

        final Duration d2 = new Duration(4, 900_000_000);
        assertThat(d1).isEqualTo(d2);

        assertThat(d2.normalized()).isEqualTo("PT4.9S");
    }

    @Test
    void testNegativeNormalized()
    {
        final Duration d1 = ItuDurationParser.parse("-PT4.9S");
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
        Duration d1 = new Duration(5, 900_000_000); // 5.9 seconds
        Duration d2 = new Duration(-5, 100_000_000); // Represents -4.9 seconds (correctly normalized)

        Duration result = d1.add(d2);

        assertThat(result.getSeconds()).isEqualTo(1); // 5.9 -4.9 = 1.0 seconds
        assertThat(result.getNano()).isEqualTo(0);
    }

    @Test
    void testAddOverflow()
    {
        Duration d1 = new Duration(Long.MAX_VALUE, 0);
        Duration d2 = new Duration(1, 0);

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
        Duration d1 = new Duration(5, 500_000_000); // 5 seconds, 500 ms
        Duration d2 = new Duration(3, 800_000_000); // 3 seconds, 800 ms

        Duration result = d1.subtract(d2);

        // Expecting 1 second, -300 ms
        assertThat(result.getSeconds()).isEqualTo(1);
        assertThat(result.getNano()).isEqualTo(700_000_000);
    }

    @Test
    void testSubtractNegativeValues()
    {
        final Duration d1 = ItuDurationParser.parse("PT5.5S");
        final Duration d2 = ItuDurationParser.parse("-PT5.3S");

        Duration result = d1.subtract(d2);

        assertThat(result.normalized()).isEqualTo("PT10.8S");
    }

    @Test
    void testSubtractOverflow()
    {
        Duration d1 = new Duration(Long.MIN_VALUE, 0);
        Duration d2 = new Duration(1, 0);

        // Expecting ArithmeticException due to overflow
        assertThatThrownBy(() -> d1.subtract(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    void testSubtractUnderflow()
    {
        Duration d1 = new Duration(Long.MAX_VALUE, 0);
        Duration d2 = new Duration(-1, 0);

        // Expecting ArithmeticException due to underflow
        assertThatThrownBy(() -> d1.subtract(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    void testNegatePositiveDuration()
    {
        // 5.5 seconds is stored as (5, 500_000_000)
        Duration d = new Duration(5, 500_000_000);
        // Negation of 5.5 sec is -5.5 sec, represented as (-6, 500_000_000)
        Duration neg = d.negate();
        assertThat(neg.getSeconds()).isEqualTo(-6);
        assertThat(neg.getNano()).isEqualTo(500_000_000);
    }

    @Test
    void testNegateNegativeDuration()
    {
        // -5.5 seconds is represented as (-6, 500_000_000)
        Duration d = new Duration(-6, 500_000_000);
        // Negation should yield 5.5 seconds, represented as (5, 500_000_000)
        Duration neg = d.negate();
        assertThat(neg.getSeconds()).isEqualTo(5);
        assertThat(neg.getNano()).isEqualTo(500_000_000);
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
        Duration d = new Duration(5, 0);
        Duration neg = d.negate();
        assertThat(neg.getSeconds()).isEqualTo(-5);
        assertThat(neg.getNano()).isEqualTo(0);
    }

    @Test
    void testNegateWholeNegativeDuration()
    {
        Duration d = new Duration(-5, 0);
        Duration neg = d.negate();
        assertThat(neg.getSeconds()).isEqualTo(5);
        assertThat(neg.getNano()).isEqualTo(0);
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
        Duration d1 = new Duration(5, 600_000_000);
        Duration d2 = new Duration(3, 500_000_000);
        Duration result = d1.add(d2);

        assertThat(result.getSeconds()).isEqualTo(9);
        assertThat(result.getNano()).isEqualTo(100_000_000);
    }

    @Test
    void testAddWithCarry()
    {
        Duration d1 = new Duration(2, 800_000_000);
        Duration d2 = new Duration(3, 400_000_000);
        Duration result = d1.add(d2);

        assertThat(result.getSeconds()).isEqualTo(6);
        assertThat(result.getNano()).isEqualTo(200_000_000);
    }

    @Test
    void testAddNegativeDuration()
    {
        final Duration d1 = ItuDurationParser.parse("PT5.2S");
        final Duration d2 = ItuDurationParser.parse("-PT2.2S");
        Duration result = d1.add(d2);

        assertThat(result.normalized()).isEqualTo("PT3S");
    }

    @Test
    void testAddWithOverflow()
    {
        Duration d1 = new Duration(Long.MAX_VALUE, 0);
        Duration d2 = new Duration(1, 0);

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
        final Instant future = d1.future(now);
        assertThat(future.getEpochSecond()).isEqualTo(7);
        assertThat(future.getNano()).isEqualTo(800_000_000);
    }

    @Test
    void futureNegative()
    {
        final Instant now = Instant.ofEpochSecond(0, 0);
        final Duration d1 = ItuDurationParser.parse("-PT7.8S");
        final Instant future = d1.future(now);
        assertThat(future.getEpochSecond()).isEqualTo(-8);
        assertThat(future.getNano()).isEqualTo(200_000_000);
    }

    @Test
    void pastPositive()
    {
        final Instant now = Instant.ofEpochSecond(0, 0);
        final Duration d1 = ItuDurationParser.parse("PT7.8S");
        final Instant result = d1.past(now);
        assertThat(result.getEpochSecond()).isEqualTo(-8);
        assertThat(result.getNano()).isEqualTo(200_000_000);
    }

    @Test
    void pastNegative()
    {
        final Instant now = Instant.ofEpochSecond(0, 0);
        final Duration d1 = ItuDurationParser.parse("-PT7.8S");
        final Instant result = d1.past(now);
        assertThat(result.getEpochSecond()).isEqualTo(7);
        assertThat(result.getNano()).isEqualTo(800_000_000);
    }

    @Test
    void testSubtractWithBorrow()
    {
        Duration d1 = new Duration(5, 200_000_000);
        Duration d2 = new Duration(3, 500_000_000);
        Duration result = d1.subtract(d2);

        assertThat(result.getSeconds()).isEqualTo(1);
        assertThat(result.getNano()).isEqualTo(700_000_000);
    }

    @Test
    void testSubtractNegativeDuration()
    {
        final Duration d1 = ItuDurationParser.parse("-PT5.4S");
        final Duration d2 = ItuDurationParser.parse("-PT5.8S");
        Duration result = d1.subtract(d2);

        assertThat(result.normalized()).isEqualTo("PT0.4S");
    }

    @Test
    void testSubtractWithUnderflow()
    {
        Duration d1 = new Duration(Long.MIN_VALUE, 0);
        Duration d2 = new Duration(1, 0);

        assertThatThrownBy(() -> d1.subtract(d2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("long overflow");
    }
}
