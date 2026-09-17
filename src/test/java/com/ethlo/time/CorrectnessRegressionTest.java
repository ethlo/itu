package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2026 Morten Haraldsen (ethlo)
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

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.ethlo.time.internal.DateTimeFormatException;

/**
 * Regression tests for defects found during review of 1.14.0. Each nested class corresponds to one defect,
 * and every test in here fails on 1.14.0.
 */
class CorrectnessRegressionTest
{
    @Nested
    class TimezoneOffsetSeparator
    {
        /**
         * The offset separator position was never inspected, so any character was accepted between the hour
         * and minute part and the value silently parsed as if it had been a colon.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "2017-02-21T15:27:39+01x30",
                "2017-02-21T15:27:39+01930",
                "2017-02-21T15:27:39+01-30",
                "2017-02-21T15:27:39+01 30",
                "2017-02-21T15:27:39-01.30"
        })
        void rejectsNonColonOffsetSeparator(String input)
        {
            assertThatThrownBy(() -> ITU.parseDateTime(input))
                    .isInstanceOf(DateTimeParseException.class)
                    .hasMessageContaining("Expected character : at position 23");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "2017-02-21T15:27:39+01x30",
                "2017-02-21T15:27:39+01930"
        })
        void rejectsNonColonOffsetSeparatorWhenLenient(String input)
        {
            assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(input));
        }

        @Test
        void rejectsNonColonOffsetSeparatorInConfigurableParser()
        {
            final DateTimeParser parser = rfc3339LikeTokenParser();
            assertThrows(DateTimeParseException.class, () -> parser.parse("2017-02-21T15:27:39+01x30"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"2017-02-21T15:27:39+01:30", "2017-02-21T15:27:39-05:00", "2017-02-21T15:27:39Z"})
        void stillAcceptsWellFormedOffsets(String input)
        {
            assertThat(ITU.parseDateTime(input)).isEqualTo(OffsetDateTime.parse(input));
        }

        /**
         * A short offset must still be reported as such, rather than as a missing separator
         */
        @Test
        void shortOffsetIsStillReportedAsInvalidOffset()
        {
            assertThatThrownBy(() -> ITU.parseDateTime("2017-02-21T15:27:39+0000"))
                    .isInstanceOf(DateTimeParseException.class)
                    .hasMessage("Invalid timezone offset: 2017-02-21T15:27:39+0000");
        }
    }

    @Nested
    class FieldRangeValidation
    {
        /**
         * Only the date and the second field were range-checked, so an out-of-range hour or minute survived
         * lenient parsing and round-tripped through toString().
         */
        @ParameterizedTest
        @CsvSource({
                "2017-02-21T99:99, HourOfDay",
                "2017-02-21T24:00, HourOfDay",
                "2017-02-21T25:00:00Z, HourOfDay",
                "2017-02-21T23:60, MinuteOfHour",
                "2017-02-21T23:60:00Z, MinuteOfHour"
        })
        void rejectsOutOfRangeTimeFields(String input, String expectedField)
        {
            assertThatThrownBy(() -> ITU.parseLenient(input))
                    .isInstanceOf(DateTimeException.class)
                    .hasMessageContaining("Invalid value for " + expectedField);
        }

        @Test
        void rejectsOutOfRangeHourInConfigurableParser()
        {
            final DateTimeParser parser = rfc3339LikeTokenParser();
            assertThatThrownBy(() -> parser.parse("2017-02-21T99:27:39Z"))
                    .isInstanceOf(DateTimeException.class)
                    .hasMessageContaining("Invalid value for HourOfDay");
        }

        @Test
        void rejectsOutOfRangeOffsetWhenLenient()
        {
            assertThatThrownBy(() -> ITU.parseLenient("2017-02-21T15:27:39+99:99"))
                    .isInstanceOf(DateTimeException.class)
                    .hasMessage("Zone offset hours not in valid range: value 99 is not in the range -18 to 18");
        }

        @Test
        void rejectsOutOfRangeOffsetOnConstruction()
        {
            assertThrows(DateTimeException.class, () -> TimezoneOffset.ofHoursMinutes(19, 0));
            assertThrows(DateTimeException.class, () -> TimezoneOffset.ofHoursMinutes(0, 60));
            assertThrows(DateTimeException.class, () -> TimezoneOffset.ofHoursMinutes(18, 1));
        }

        /**
         * Hours and minutes carrying opposite signs produced an offset that was neither of the two
         */
        @Test
        void rejectsMixedSignOffset()
        {
            assertThrows(DateTimeException.class, () -> TimezoneOffset.ofHoursMinutes(1, -30));
            assertThrows(DateTimeException.class, () -> TimezoneOffset.ofHoursMinutes(-1, 30));
        }

        @Test
        void acceptsExtremesOfTheValidOffsetRange()
        {
            assertThat(TimezoneOffset.ofHoursMinutes(18, 0).getTotalSeconds()).isEqualTo(64800);
            assertThat(TimezoneOffset.ofHoursMinutes(-18, 0).getTotalSeconds()).isEqualTo(-64800);
            assertThat(TimezoneOffset.ofHoursMinutes(-1, -30).getTotalSeconds()).isEqualTo(-5400);
        }
    }

    @Nested
    class TrailingJunkConfiguration
    {
        /**
         * withFailOnTrailingJunk(..) dropped its argument and isFailOnTrailingJunk() was hard-coded to true
         */
        @Test
        void honoursDisabledTrailingJunkCheck()
        {
            final ParseConfig config = ParseConfig.DEFAULT.withFailOnTrailingJunk(false);
            assertThat(config.isFailOnTrailingJunk()).isFalse();
            assertThat(ITU.parseLenient("2017-02-21T15:27:39Zjunk", config).toString()).isEqualTo("2017-02-21T15:27:39Z");
        }

        @Test
        void honoursEnabledTrailingJunkCheck()
        {
            final ParseConfig config = ParseConfig.DEFAULT.withFailOnTrailingJunk(true);
            assertThat(config.isFailOnTrailingJunk()).isTrue();
            assertThrows(DateTimeParseException.class, () -> ITU.parseLenient("2017-02-21T15:27:39Zjunk", config));
        }

        /**
         * The Z form at second resolution short-cut straight to TimezoneOffset.UTC and never ran the
         * trailing junk check, so this one shape - by far the most common RFC-3339 form - let junk through
         * while every other shape rejected it.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "2017-02-21T15:27:39Zjunk",
                "2017-02-21T15:27:39zjunk",
                "2017-02-21T15:27:39Z ",
                "2017-02-21T15:27:39Z2017-02-21T15:27:39Z"
        })
        void rejectsTrailingJunkAfterZuluAtSecondResolution(String input)
        {
            assertThatThrownBy(() -> ITU.parseDateTime(input))
                    .isInstanceOf(DateTimeParseException.class)
                    .hasMessageContaining("Trailing junk data after position 21");
            assertThat(ITU.isValid(input)).isFalse();
        }

        /**
         * ..and the other shapes, which already worked, must keep working
         */
        @ParameterizedTest
        @CsvSource({
                "2017-02-21T15:27:39+01:00junk, 26",
                "2017-02-21T15:00:00.123ZGGG, 25",
                "2017-02-21T15:27Zjunk, 18"
        })
        void rejectsTrailingJunkInOtherShapes(String input, int position)
        {
            assertThatThrownBy(() -> ITU.parseDateTime(input))
                    .isInstanceOf(DateTimeParseException.class)
                    .hasMessageContaining("Trailing junk data after position " + position);
        }

        @Test
        void trailingJunkSettingSurvivesOtherWithers()
        {
            final ParseConfig config = ParseConfig.DEFAULT
                    .withFailOnTrailingJunk(false)
                    .withDateTimeSeparators('T')
                    .withFractionSeparators('.');
            assertThat(config.isFailOnTrailingJunk()).isFalse();
        }

        /**
         * The shared DEFAULT/STRICT instances handed out their internal arrays, so a caller could globally
         * reconfigure parsing for the whole process
         */
        @Test
        void separatorArraysAreNotSharedWithCallers()
        {
            final char[] separators = ParseConfig.DEFAULT.getDateTimeSeparators();
            separators[0] = 'X';
            assertThat(ParseConfig.DEFAULT.getDateTimeSeparators()).contains('T');
            assertThat(ParseConfig.DEFAULT.isDateTimeSeparator('T')).isTrue();
            assertThat(ParseConfig.DEFAULT.isDateTimeSeparator('X')).isFalse();

            final char[] fractions = ParseConfig.DEFAULT.getFractionSeparators();
            fractions[0] = 'X';
            assertThat(ParseConfig.DEFAULT.getFractionSeparators()).containsExactly('.');
        }

        @Test
        void callerSuppliedSeparatorArrayIsCopied()
        {
            final char[] allowed = new char[]{'T'};
            final ParseConfig config = ParseConfig.DEFAULT.withDateTimeSeparators(allowed);
            allowed[0] = 'X';
            assertThat(config.isDateTimeSeparator('T')).isTrue();
        }
    }

    @Nested
    class FractionFormatting
    {
        /**
         * Nano values below 10000 were rendered from the wrong end, so .000009999 came out as ".99"
         */
        @ParameterizedTest
        @CsvSource({
                "9999, 1, 2020-01-01T00:00:00.0Z",
                "9999, 2, 2020-01-01T00:00:00.00Z",
                "9999, 3, 2020-01-01T00:00:00.000Z",
                "9999, 4, 2020-01-01T00:00:00.0000Z",
                "9999, 5, 2020-01-01T00:00:00.00000Z",
                "9999, 9, 2020-01-01T00:00:00.000009999Z",
                "1, 9, 2020-01-01T00:00:00.000000001Z",
                "999, 3, 2020-01-01T00:00:00.000Z",
                "123456789, 3, 2020-01-01T00:00:00.123Z",
                "123456789, 9, 2020-01-01T00:00:00.123456789Z",
                "1000, 9, 2020-01-01T00:00:00.000001000Z"
        })
        void dateTimeToStringTruncatesFromTheLeft(int nano, int fractionDigits, String expected)
        {
            final DateTime dateTime = DateTime.of(2020, 1, 1, 0, 0, 0, nano, TimezoneOffset.UTC, 9);
            assertThat(dateTime.toString(fractionDigits)).isEqualTo(expected);
        }

        /**
         * The two formatting paths must agree with each other and with java.time
         */
        @ParameterizedTest
        @CsvSource({"9999, 2", "9999, 3", "1, 9", "123456789, 4", "999999999, 1"})
        void dateTimeAndFormatterAgree(int nano, int fractionDigits)
        {
            final OffsetDateTime offsetDateTime = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, nano, ZoneOffset.UTC);
            final DateTime dateTime = DateTime.of(offsetDateTime);
            assertThat(dateTime.toString(fractionDigits)).isEqualTo(ITU.formatUtc(offsetDateTime, fractionDigits));
        }

        /**
         * DateTime.toString(int) had no upper bound and overran its buffer at 16 digits
         */
        @ParameterizedTest
        @ValueSource(ints = {10, 16, 32, -1})
        void rejectsUnsupportedFractionDigitCount(int fractionDigits)
        {
            final DateTime dateTime = ITU.parseLenient("2017-02-21T15:27:39.123Z");
            assertThrows(DateTimeFormatException.class, () -> dateTime.toString(fractionDigits));
        }
    }

    @Nested
    class YearRange
    {
        /**
         * A year outside 0000-9999 was silently truncated to four digits, and a negative year threw
         * ArrayIndexOutOfBoundsException out of the formatter
         */
        @ParameterizedTest
        @ValueSource(ints = {10000, 12345, -1, -44})
        void rejectsUnrepresentableYear(int year)
        {
            final OffsetDateTime offsetDateTime = OffsetDateTime.of(year, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
            assertThatThrownBy(() -> ITU.formatUtc(offsetDateTime))
                    .isInstanceOf(DateTimeFormatException.class)
                    .hasMessageContaining("The year must be in the range 0 to 9999");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 999, 9999})
        void acceptsRepresentableYear(int year)
        {
            final OffsetDateTime offsetDateTime = OffsetDateTime.of(year, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
            assertThat(ITU.formatUtc(offsetDateTime)).isEqualTo(String.format(Locale.ROOT, "%04d-01-02T03:04:05Z", year));
        }
    }

    @Nested
    class DurationArithmetic
    {
        /**
         * The borrow was taken whenever the seconds were negative rather than when the nano remainder was,
         * which made every exact negative second unrepresentable
         */
        @ParameterizedTest
        @CsvSource({
                "-1000000000, -1, 0",
                "-2000000000, -2, 0",
                "-1500000000, -2, 500000000",
                "-1, -1, 999999999",
                "0, 0, 0",
                "1000000000, 1, 0",
                "1500000000, 1, 500000000"
        })
        void ofNanosHandlesNegativeWholeSeconds(long nanos, long expectedSeconds, int expectedNanos)
        {
            final Duration duration = Duration.ofNanos(nanos);
            assertThat(duration.getSeconds()).isEqualTo(expectedSeconds);
            assertThat(duration.getNanos()).isEqualTo(expectedNanos);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1000, -2000, -1, -1500, 0, 1000})
        void ofMillisHandlesNegativeWholeSeconds(long millis)
        {
            assertThat(Duration.ofMillis(millis).toDuration()).isEqualTo(java.time.Duration.ofMillis(millis));
        }

        /**
         * A negative argument produced a negative nano remainder that the constructor then rejected
         */
        @ParameterizedTest
        @CsvSource({
                "5, -1, 4, 999999999",
                "5, -1000000000, 4, 0",
                "0, -1, -1, 999999999",
                "5, 1, 5, 1"
        })
        void plusNanosAcceptsNegativeValues(long seconds, long delta, long expectedSeconds, int expectedNanos)
        {
            final Duration duration = Duration.ofSeconds(seconds).plusNanos(delta);
            assertThat(duration.getSeconds()).isEqualTo(expectedSeconds);
            assertThat(duration.getNanos()).isEqualTo(expectedNanos);
        }

        @Test
        void plusNanosMatchesJavaTime()
        {
            for (long delta : new long[]{-2_000_000_001L, -1_000_000_000L, -1, 0, 1, 1_000_000_000L})
            {
                assertThat(Duration.ofSeconds(5).plusNanos(delta).toDuration())
                        .as("delta %d", delta)
                        .isEqualTo(java.time.Duration.ofSeconds(5).plusNanos(delta));
            }
        }

        @Test
        void compareToOrdersBySecondsThenNanos()
        {
            assertThat(Duration.of(1, 0).compareTo(Duration.of(2, 0))).isNegative();
            assertThat(Duration.of(2, 0).compareTo(Duration.of(1, 0))).isPositive();
            assertThat(Duration.of(1, 1).compareTo(Duration.of(1, 2))).isNegative();
            assertThat(Duration.of(1, 2).compareTo(Duration.of(1, 2))).isZero();
            assertThat(Duration.of(-1, 0).compareTo(Duration.of(1, 0))).isNegative();
        }
    }

    @Nested
    class DurationFormattingAndParsing
    {
        /**
         * The fractional part was rendered with String.format("%09d", ..), which uses the default locale and
         * therefore emitted non-Latin digits under a locale with a different default numbering system
         */
        @Test
        void normalizedIsLocaleIndependent()
        {
            final Locale original = Locale.getDefault();
            try
            {
                Locale.setDefault(Locale.forLanguageTag("hi-IN-u-nu-deva"));
                assertThat(ITU.parseDuration("PT1.5S").normalized()).isEqualTo("PT1.5S");
                assertThat(ITU.parseDuration("-PT2.000000001S").normalized()).isEqualTo("-PT2.000000001S");
                assertThat(ITU.parseDuration("P1DT2H3M4.005S").normalized()).isEqualTo("P1DT2H3M4.005S");
            }
            finally
            {
                Locale.setDefault(original);
            }
        }

        /**
         * Overflow escaped as ArithmeticException, although the API documents DateTimeParseException
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "P99999999999999999999D",
                "P15250284452471WT9223372036854775807S",
                "P1DT9223372036854775807S"
        })
        void overflowIsReportedAsParseException(String input)
        {
            assertThatThrownBy(() -> ITU.parseDuration(input)).isInstanceOf(DateTimeParseException.class);
        }

        @Test
        void normalizedRoundTrips()
        {
            for (String input : new String[]{"PT0S", "PT1S", "-PT1S", "PT1.5S", "-PT1.5S", "P1W", "P1DT2H3M4S", "-PT0.000000001S"})
            {
                assertThat(ITU.parseDuration(input).normalized()).as(input).isEqualTo(input);
            }
        }
    }

    @Nested
    class ConfigurableParserFractions
    {
        /**
         * The token parser never bounded the fraction length, so the accumulator overflowed and the nano
         * value silently exceeded one second
         */
        @Test
        void rejectsTooManyFractionDigits()
        {
            assertThatThrownBy(() -> DateTimeParsers.localTime().parse("12:30:45.12345678901234"))
                    .isInstanceOf(DateTimeParseException.class)
                    .hasMessageContaining("Maximum supported number of fraction digits in second is 9, got 14");
        }

        @Test
        void rejectsMissingFractionDigits()
        {
            assertThatThrownBy(() -> DateTimeParsers.localTime().parse("12:30:45."))
                    .isInstanceOf(DateTimeParseException.class)
                    .hasMessageContaining("Must have at least 1 fraction digit");
        }

        @ParameterizedTest
        @CsvSource({"12:30:45.1, 100000000", "12:30:45.123456789, 123456789", "12:30:45.000000001, 1"})
        void stillAcceptsValidFractions(String input, int expectedNano)
        {
            assertThat(DateTimeParsers.localTime().parse(input).getNano()).isEqualTo(expectedNano);
        }
    }

    @Nested
    class ErrorIndex
    {
        /**
         * The offset was added twice when reporting the position of a fraction error, producing an index
         * beyond the end of the input that was then pushed into the caller's ParsePosition
         */
        @Test
        void fractionErrorIndexIsWithinTheInput()
        {
            final String text = "xxxxx2017-02-21T15:27:39.Z";
            final ParsePosition position = new ParsePosition(5);
            assertThatThrownBy(() -> ITU.parseLenient(text, ParseConfig.DEFAULT, position))
                    .isInstanceOf(DateTimeParseException.class)
                    .satisfies(exc -> assertThat(((DateTimeParseException) exc).getErrorIndex())
                            .isBetween(0, text.length() - 1)
                            .isEqualTo(24));
            assertThat(position.getErrorIndex()).isBetween(0, text.length() - 1);
        }

        @Test
        void fractionErrorIndexUnchangedAtZeroOffset()
        {
            assertThatThrownBy(() -> ITU.parseLenient("2017-02-21T15:27:39.Z"))
                    .isInstanceOf(DateTimeParseException.class)
                    .satisfies(exc -> assertThat(((DateTimeParseException) exc).getErrorIndex()).isEqualTo(19));
        }
    }

    @Nested
    class TemporalAccessorContract
    {
        /**
         * isSupported(..) must answer false for unsupported fields rather than throwing, or generic
         * java.time code cannot safely probe a DateTime
         */
        @Test
        void unsupportedFieldsReturnFalse()
        {
            final DateTime dateTime = DateTime.ofYear(2020);
            assertThat(dateTime.isSupported(ChronoField.ALIGNED_WEEK_OF_YEAR)).isFalse();
            assertThat(dateTime.isSupported(ChronoField.DAY_OF_WEEK)).isFalse();
            assertThat(dateTime.isSupported(ChronoField.OFFSET_SECONDS)).isFalse();
            assertThat(dateTime.isSupported(ChronoField.ERA)).isFalse();
            assertThat(dateTime.isSupported(null)).isFalse();
        }

        @Test
        void instantSecondsIsConsistentWithGetLong()
        {
            final DateTime dateTime = DateTime.ofYear(2020);
            assertThat(dateTime.isSupported(ChronoField.INSTANT_SECONDS)).isTrue();
            assertThat(dateTime.getLong(ChronoField.INSTANT_SECONDS)).isEqualTo(1577836800L);
        }

        /**
         * NANO_OF_DAY was reported as supported even for a year-only value, so toLocalTime() quietly
         * returned midnight
         */
        @Test
        void nanoOfDayRequiresTimeGranularity()
        {
            assertThat(DateTime.ofYear(2020).isSupported(ChronoField.NANO_OF_DAY)).isFalse();
            assertThat(DateTime.ofDate(2020, 1, 1).isSupported(ChronoField.NANO_OF_DAY)).isFalse();
            assertThat(ITU.parseLenient("2020-01-01T10:15Z").isSupported(ChronoField.NANO_OF_DAY)).isTrue();

            assertThrows(DateTimeException.class, () -> DateTime.ofYear(2020).toLocalTime());
            assertThat(ITU.parseLenient("2020-01-01T10:15Z").toLocalTime().toString()).isEqualTo("10:15");
        }

        @Test
        void supportedFieldsStillFollowGranularity()
        {
            final DateTime dateTime = ITU.parseLenient("2020-01-01T10:15Z");
            assertThat(dateTime.isSupported(ChronoField.YEAR)).isTrue();
            assertThat(dateTime.isSupported(ChronoField.MINUTE_OF_HOUR)).isTrue();
            assertThat(dateTime.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        }
    }

    @Nested
    class SubMinuteOffsets
    {
        /**
         * An offset that is not a whole number of minutes cannot be expressed in RFC-3339. It used to be
         * rounded down silently, shifting the formatted instant by up to 59 seconds.
         */
        @Test
        void rejectsSubMinuteOffset()
        {
            assertThrows(DateTimeException.class, () -> TimezoneOffset.of(ZoneOffset.ofTotalSeconds(-3630)));
            assertThrows(DateTimeException.class, () -> TimezoneOffset.ofTotalSeconds(-3630));
            assertThatThrownBy(() -> ITU.format(OffsetDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.ofTotalSeconds(-3630))))
                    .isInstanceOf(DateTimeException.class);
        }

        @Test
        void acceptsWholeMinuteOffsets()
        {
            assertThat(TimezoneOffset.of(ZoneOffset.ofTotalSeconds(-5400)).getTotalSeconds()).isEqualTo(-5400);
            assertThat(TimezoneOffset.ofTotalSeconds(0).getTotalSeconds()).isZero();
        }
    }

    @Nested
    class ParseLength
    {
        /**
         * Pins the distinction between the one-character Z form and the six-character +00:00 form, which
         * TimezoneOffset.getRequiredLength() relies on an identity comparison to tell apart
         */
        @Test
        void parseLengthCoversTheWholeOffset()
        {
            assertThat(ITU.parseLenient("2017-02-21T15:27:39Z").getParseLength()).isEqualTo(20);
            assertThat(ITU.parseLenient("2017-02-21T15:27:39+00:00").getParseLength()).isEqualTo(25);
            assertThat(ITU.parseLenient("2017-02-21T15:27:39.123+01:00").getParseLength()).isEqualTo(29);

            final ParsePosition position = new ParsePosition(0);
            DateTimeParsers.rfc3339().parse("2017-02-21T15:27:39+00:00", position);
            assertThat(position.getIndex()).isEqualTo(25);
        }
    }

    private static DateTimeParser rfc3339LikeTokenParser()
    {
        return DateTimeParsers.of(
                DateTimeTokens.digits(Field.YEAR, 4),
                DateTimeTokens.separators('-'),
                DateTimeTokens.digits(Field.MONTH, 2),
                DateTimeTokens.separators('-'),
                DateTimeTokens.digits(Field.DAY, 2),
                DateTimeTokens.separators('T'),
                DateTimeTokens.digits(Field.HOUR, 2),
                DateTimeTokens.separators(':'),
                DateTimeTokens.digits(Field.MINUTE, 2),
                DateTimeTokens.separators(':'),
                DateTimeTokens.digits(Field.SECOND, 2),
                DateTimeTokens.zoneOffset()
        );
    }
}
