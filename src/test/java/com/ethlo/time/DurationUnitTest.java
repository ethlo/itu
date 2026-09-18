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

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DurationUnitTest
{
    private static final Duration SAMPLE = Duration.of(3_000_000, 117_392_763);
    private static final Duration EXTREME_POSITIVE = Duration.of(Long.MAX_VALUE, 999_999_999);
    private static final Duration EXTREME_NEGATIVE = Duration.of(Long.MIN_VALUE, 1);

    @Test
    void cappingFoldsLargerUnitsIntoTheCap()
    {
        assertThat(SAMPLE.normalized(DurationUnit.WEEKS)).isEqualTo("P4W6DT17H20M0.117392763S");
        assertThat(SAMPLE.normalized(DurationUnit.DAYS)).isEqualTo("P34DT17H20M0.117392763S");
        assertThat(SAMPLE.normalized(DurationUnit.HOURS)).isEqualTo("PT833H20M0.117392763S");
        assertThat(SAMPLE.normalized(DurationUnit.MINUTES)).isEqualTo("PT50000M0.117392763S");
        assertThat(SAMPLE.normalized(DurationUnit.SECONDS)).isEqualTo("PT3000000.117392763S");
    }

    @Test
    void noArgNormalizedStillUsesWeeks()
    {
        assertThat(SAMPLE.normalized()).isEqualTo(SAMPLE.normalized(DurationUnit.WEEKS));
        assertThat(SAMPLE.toString()).isEqualTo(SAMPLE.normalized(DurationUnit.WEEKS));
        assertThat(Duration.ofWeeks(1).normalized()).isEqualTo("P1W");
    }

    @Test
    void negativeDurationsKeepTheSingleLeadingSign()
    {
        final Duration d = Duration.of(-604_800, 500_000_000);
        assertThat(d.normalized(DurationUnit.WEEKS)).isEqualTo("-P6DT23H59M59.5S");
        assertThat(d.normalized(DurationUnit.DAYS)).isEqualTo("-P6DT23H59M59.5S");
        assertThat(d.normalized(DurationUnit.HOURS)).isEqualTo("-PT167H59M59.5S");
        assertThat(d.normalized(DurationUnit.MINUTES)).isEqualTo("-PT10079M59.5S");
        assertThat(d.normalized(DurationUnit.SECONDS)).isEqualTo("-PT604799.5S");
    }

    @Test
    void zeroIsUnaffectedByTheCap()
    {
        for (DurationUnit unit : DurationUnit.values())
        {
            assertThat(Duration.ZERO.normalized(unit)).isEqualTo("PT0S");
        }
    }

    @Test
    void nullCapIsRejected()
    {
        assertThatThrownBy(() -> SAMPLE.normalized(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("maxUnit");
    }

    /**
     * The reason the cap exists: {@code java.time.Duration.parse} rejects the week designator, so only the
     * default {@link DurationUnit#WEEKS} rendering is unreadable by the Java Time API.
     */
    @ParameterizedTest
    @EnumSource(DurationUnit.class)
    void everyCapBelowWeeksIsReadableByJavaTime(final DurationUnit unit)
    {
        final Duration[] cases = {SAMPLE, EXTREME_POSITIVE, EXTREME_NEGATIVE, Duration.ofWeeks(1),
                Duration.ofDays(1), Duration.of(-604_800, 500_000_000), Duration.ofNanos(1), Duration.ZERO};

        for (Duration d : cases)
        {
            final String rendered = d.normalized(unit);
            if (unit == DurationUnit.WEEKS && rendered.indexOf('W') >= 0)
            {
                assertThatThrownBy(() -> java.time.Duration.parse(rendered))
                        .isInstanceOf(java.time.format.DateTimeParseException.class);
                continue;
            }
            assertThat(java.time.Duration.parse(rendered))
                    .as("%s rendered as %s", d.normalized(), rendered)
                    .isEqualTo(java.time.Duration.ofSeconds(d.getSeconds(), d.getNanos()));
        }
    }

    @ParameterizedTest
    @EnumSource(DurationUnit.class)
    void everyCapRoundTripsThroughTheItuParser(final DurationUnit unit)
    {
        final Random r = new Random(1234);
        for (int i = 0; i < 200_000; i++)
        {
            final long seconds = r.nextLong() >> r.nextInt(64);
            final int nanos = r.nextInt(1_000_000_000);
            final Duration d = Duration.of(seconds, nanos);
            final String rendered;
            try
            {
                rendered = d.normalized(unit);
            }
            catch (ArithmeticException e)
            {
                // Long.MIN_VALUE with no fraction has no representable magnitude
                continue;
            }
            assertThat(ITU.parseDuration(rendered)).as("%s", rendered).isEqualTo(d);
        }
    }

    /**
     * The render buffer is sized for the longest weeks form, so every other cap has to stay within that
     * bound. Overrunning it would be an {@link ArrayIndexOutOfBoundsException} out of the formatter.
     */
    @ParameterizedTest
    @EnumSource(DurationUnit.class)
    void noCapExceedsTheWeeksFormLength(final DurationUnit unit)
    {
        // The genuine worst case, found by scanning the top and bottom of the range across every cap
        final Duration worstCase = Duration.of(-9_223_372_036_854_775_800L, 1);
        assertThat(worstCase.normalized(DurationUnit.WEEKS))
                .isEqualTo("-P15250284452471W3DT15H29M59.999999999S")
                .hasSize(39);

        assertThat(worstCase.normalized(unit).length()).isLessThanOrEqualTo(39);
        assertThat(EXTREME_POSITIVE.normalized(unit).length()).isLessThanOrEqualTo(39);
        assertThat(EXTREME_NEGATIVE.normalized(unit).length()).isLessThanOrEqualTo(39);
    }
}
