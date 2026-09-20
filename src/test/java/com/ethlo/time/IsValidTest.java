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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The edges of {@link ITU#isValid(String)} that the validator decides itself rather than by parsing: each case is
 * checked against the expected answer and against {@link ITU#parseDateTime(String)}, so the expectation documents the
 * rule and the differential proves the validator applies it the same way the parser does.
 */
public class IsValidTest
{
    static Stream<Arguments> cases()
    {
        return Stream.of(
                // Shortest valid input, and one short of it
                Arguments.of("2023-01-01T00:00:00Z", true),
                Arguments.of("2023-01-01T00:00:0Z", false),
                Arguments.of("2023-01-01T00:00:00", false),
                // Date/time separators accepted by ParseConfig.DEFAULT
                Arguments.of("2023-01-01t00:00:00z", true),
                Arguments.of("2023-01-01 00:00:00Z", true),
                Arguments.of("2023-01-01_00:00:00Z", false),
                // Fixed separators
                Arguments.of("2023/01/01T00:00:00Z", false),
                Arguments.of("2023-01-01T00.00.00Z", false),
                // Non-digits in a digit position, including a non-ASCII one
                Arguments.of("2023-01-01T00:00:0xZ", false),
                Arguments.of("2023-01-01T00:00:0٠Z", false),
                Arguments.of("+023-01-01T00:00:00Z", false),
                // Field ranges
                Arguments.of("0000-01-01T00:00:00Z", true),
                Arguments.of("2023-00-01T00:00:00Z", false),
                Arguments.of("2023-13-01T00:00:00Z", false),
                Arguments.of("2023-01-00T00:00:00Z", false),
                Arguments.of("2023-01-32T00:00:00Z", false),
                Arguments.of("2023-04-31T00:00:00Z", false),
                Arguments.of("2023-02-29T00:00:00Z", false),
                Arguments.of("2024-02-29T00:00:00Z", true),
                Arguments.of("1900-02-29T00:00:00Z", false),
                Arguments.of("2000-02-29T00:00:00Z", true),
                Arguments.of("2023-01-01T24:00:00Z", false),
                Arguments.of("2023-01-01T23:60:00Z", false),
                Arguments.of("2023-01-01T23:59:59Z", true),
                // Second 60: a leap second where one can occur (LeapSecondException), out of range everywhere else
                Arguments.of("2016-12-31T23:59:60Z", false),
                Arguments.of("2023-01-01T00:00:60Z", false),
                // Fraction: 1-9 digits, and never last
                Arguments.of("2023-01-01T00:00:00.1Z", true),
                Arguments.of("2023-01-01T00:00:00.123456789Z", true),
                Arguments.of("2023-01-01T00:00:00.1234567890Z", false),
                Arguments.of("2023-01-01T00:00:00.Z", false),
                Arguments.of("2023-01-01T00:00:00.123", false),
                Arguments.of("2023-01-01T00:00:00,123Z", false),
                // Offset: Z or a sign and HH:MM within +/-18:00, and -00:00 is the unknown local offset
                Arguments.of("2023-01-01T00:00:00+00:00", true),
                Arguments.of("2023-01-01T00:00:00-00:00", false),
                Arguments.of("2023-01-01T00:00:00-00:01", true),
                Arguments.of("2023-01-01T00:00:00+18:00", true),
                Arguments.of("2023-01-01T00:00:00-18:00", true),
                Arguments.of("2023-01-01T00:00:00+18:01", false),
                Arguments.of("2023-01-01T00:00:00+17:60", false),
                Arguments.of("2023-01-01T00:00:00+19:00", false),
                Arguments.of("2023-01-01T00:00:00+0100", false),
                Arguments.of("2023-01-01T00:00:00+01", false),
                Arguments.of("2023-01-01T00:00:00+01:0", false),
                Arguments.of("2023-01-01T00:00:00+01:00:00", false),
                Arguments.of("2023-01-01T00:00:00+01-00", false),
                Arguments.of("2023-01-01T00:00:00X", false),
                // Trailing junk
                Arguments.of("2023-01-01T00:00:00Zx", false),
                Arguments.of("2023-01-01T00:00:00Z ", false),
                Arguments.of("2023-01-01T00:00:00.123Z0", false),
                Arguments.of("2023-01-01T00:00:00+01:00Z", false),
                // Not a date at all
                Arguments.of("", false),
                Arguments.of("not-a-date-not-a-date", false)
        );
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("cases")
    void matchesExpectationAndParser(final String text, final boolean expected)
    {
        assertThat(ITU.isValid(text)).as("isValid('%s')", text).isEqualTo(expected);
        IsValidDifferential.assertSameAsParseDateTime(text);
    }
}
