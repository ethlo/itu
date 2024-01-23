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
import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("CorrectnessTest")
public abstract class CorrectnessTest extends AbstractTest
{
    @ParameterizedTest
    @ValueSource(strings = {
            "2017-02-21T15:27:39Z", "2017-02-21T15:27:39.123Z",
            "2017-02-21T15:27:39.123456Z", "2017-02-21T15:27:39.123456789Z",
            "2017-02-21T15:27:39+00:00", "2017-02-21T15:27:39.123+00:00",
            "2017-02-21T15:27:39.123456+00:00", "2017-02-21T15:27:39.123456789+00:00",
            "2017-02-21T15:27:39.1+00:00", "2017-02-21T15:27:39.12+00:00",
            "2017-02-21T15:27:39.123+00:00", "2017-02-21T15:27:39.1234+00:00",
            "2017-02-21T15:27:39.12345+00:00", "2017-02-21T15:27:39.123456+00:00",
            "2017-02-21T15:27:39.1234567+00:00", "2017-02-21T15:27:39.12345678+00:00",
            "2017-02-21T15:27:39.123456789+00:00"
    })
    void testValid(String valid)
    {
        final OffsetDateTime result = parser.parseDateTime(valid);
        assertThat(result).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2017-02-21T15",
            "2017-02-21T15Z",
            "2017-02-21T15:27",
            "2017-02-21T15:27Z",
            "2017-02-21T15:27:19~10:00",
            "2017-02-21T15:27:39.+00:00", // No fractions after dot
            "2017-02-21T15:27:39", // No timezone
            "2017-02-21T15:27:39.123",
            "2017-02-21T15:27:39.123456",
            "2017-02-21T15:27:39.123456789",
            "2017-02-21T15:27:39+0000",
            "2017-02-21T15:27:39.123+0000",
            "201702-21T15:27:39.123456+0000",
            "20170221T15:27:39.123456789+0000"})
    void testInvalid(final String invalid)
    {
        assertThrows(DateTimeException.class, () -> parser.parseDateTime(invalid));
    }

    @Test
    void testParseLeapSecondUTC()
    {
        verifyLeapSecondDateTime("1990-12-31T23:59:60Z", "1991-01-01T00:00:00Z", true);
    }

    @Test
    void testParseDoubleLeapSecondUTC()
    {
        assertThrows(DateTimeException.class, () -> verifyLeapSecondDateTime("1990-12-31T23:59:61Z", "1991-01-01T00:00:01Z", true));
    }

    private void verifyLeapSecondDateTime(String input, String expectedInUtc, boolean isVerifiedLeapSecond)
    {
        final LeapSecondException exc = getLeapSecondsException(input);
        assertThat(exc.isVerifiedValidLeapYearMonth()).isEqualTo(isVerifiedLeapSecond);
        assertThat(formatter.formatUtc(exc.getNearestDateTime())).isEqualTo(expectedInUtc);
        assertThat(exc.getSecondsInMinute()).isEqualTo(60);
    }

    @Test
    void testParseLeapSecondPST()
    {
        verifyLeapSecondDateTime("1990-12-31T15:59:60-08:00", "1991-01-01T00:00:00Z", true);
    }

    @Test
    void parseWithFragmentsNoTimezone()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-12-21T12:20:45.987"));
        assertThat(exception.getMessage()).isEqualTo("No timezone information: 2017-12-21T12:20:45.987");
    }

    @Test
    void parseWithFragmentsNonDigit()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-12-21T12:20:45.9b7Z"));
        assertThat(exception.getMessage()).isEqualTo("Invalid character starting at position 21: 2017-12-21T12:20:45.9b7Z");
    }

    @Test
    void testParseLeapSecondUTCJune()
    {
        final String leapSecondUTC = "1992-06-30T23:59:60Z";
        verifyLeapSecondDateTime(leapSecondUTC, "1992-07-01T00:00:00Z", true);
    }

    @Test
    void testParseLeapSecondPSTJune()
    {
        verifyLeapSecondDateTime("1992-06-30T15:59:60-08:00", "1992-07-01T00:00:00Z", true);
    }

    @Test
    void testParseLeapSecond()
    {
        verifyLeapSecondDateTime("1990-12-31T15:59:60-08:00", "1991-01-01T00:00:00Z", true);
    }

    @Test
    void testParseLeapSecondPotentiallyCorrect()
    {
        verifyLeapSecondDateTime("2032-06-30T15:59:60-08:00", "2032-07-01T00:00:00Z", false);
    }

    private LeapSecondException getLeapSecondsException(final String dateTime)
    {
        try
        {
            parser.parseDateTime(dateTime);
            throw new IllegalArgumentException("Should have thrown LeapSecondException");
        }
        catch (LeapSecondException exc)
        {
            return exc;
        }
    }

    @Test
    void testFormat1()
    {
        final String s = "2017-02-21T15:27:39.0000000";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isEqualTo("No timezone information: 2017-02-21T15:27:39.0000000");
    }

    @Test
    void testFormat2()
    {
        final String s = "2017-02-21T15:27:39.000+30:00";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isEqualTo("Zone offset hours not in valid range: value 30 is not in the range -18 to 18");
    }

    @Test
    void testFormat3()
    {
        final String s = "2017-02-21T10:00:00.000+12:00";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(date)).isEqualTo("2017-02-20T22:00:00.000Z");
    }

    @Test
    void testInvalidNothingAfterFractionalSeconds()
    {
        final String s = "2017-02-21T10:00:00.12345";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isEqualTo("No timezone information: 2017-02-21T10:00:00.12345");
    }

    @Test
    void parseWithoutSeconds()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-12-21T12:20Z"));
        assertThat(exception.getMessage()).isEqualTo("No SECOND field found");
    }

    @Test
    void testFormat4()
    {
        final String s = "2017-02-21T15:00:00.123Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(date)).isEqualTo("2017-02-21T15:00:00.123Z");
    }

    @Test
    void testParseMoreThanNanoResolutionFails()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-02-21T15:00:00.1234567891Z"));
        assertThat(exception.getMessage()).isEqualTo("Too many fraction digits: 2017-02-21T15:00:00.1234567891Z");
    }

    @Test
    void testParseMonthOutOfBounds()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-13-21T15:00:00Z"));
        assertThat(exception.getMessage()).isEqualTo("Invalid value for MonthOfYear (valid values 1 - 12): 13");
    }

    @Test
    void testParseDayOutOfBounds()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-11-32T15:00:00Z"));
        assertThat(exception.getMessage()).isEqualTo("Invalid value for DayOfMonth (valid values 1 - 28/31): 32");
    }

    @Test
    void testParseHourOutOfBounds()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-12-21T24:00:00Z"));
        assertThat(exception.getMessage()).isEqualTo("Invalid value for HourOfDay (valid values 0 - 23): 24");
    }

    @Test
    void testParseMinuteOfBounds()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-12-21T23:60:00Z"));
        assertThat(exception.getMessage()).isEqualTo("Invalid value for MinuteOfHour (valid values 0 - 59): 60");
    }

    @Test
    void testParseSecondOutOfBounds()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-12-21T23:00:61Z"));
        assertThat(exception.getMessage()).isEqualTo("Invalid value for SecondOfMinute (valid values 0 - 59): 61");
    }

    @Test
    void testFormatMoreThanNanoResolutionFails()
    {
        final OffsetDateTime d = parser.parseDateTime("2017-02-21T15:00:00.123456789Z");
        final int fractionDigits = 10;
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> formatter.formatUtc(d, fractionDigits));
        assertThat(exception.getMessage()).isEqualTo("Maximum supported number of fraction digits in second is 9, got 10");
    }

    @Test
    void testFormatUtc()
    {
        final String s = "2017-02-21T15:09:03.123456789Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        final String expected = "2017-02-21T15:09:03Z";
        final String actual = formatter.formatUtc(date);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testFormatUtcMilli()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(date)).isEqualTo("2017-02-21T15:00:00.123Z");
    }

    @Test
    void testFormatUtcMicro()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMicro(date)).isEqualTo("2017-02-21T15:00:00.123456Z");
    }

    @Test
    void testFormatUtcNano()
    {
        final String s = "2017-02-21T15:00:00.987654321Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcNano(date)).isEqualTo(s);
    }

    @Test
    void testFormat4TrailingNoise()
    {
        final String s = "2017-02-21T15:00:00.123ZGGG";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isEqualTo("Trailing junk data after position 24: 2017-02-21T15:00:00.123ZGGG");
    }

    @Test
    void testFormat5()
    {
        final String s = "2017-02-21T15:27:39.123+13:00";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(date)).isEqualTo("2017-02-21T02:27:39.123Z");
    }

    @Test
    void testParseEmptyString()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(""));
        assertThat(exception.getMessage()).isEqualTo("Unexpected end of expression at position 0: ''");
    }

    @Test
    void testParseNull()
    {
        final NullPointerException exception = assertThrows(NullPointerException.class, () -> parser.parseDateTime(null));
        assertThat(exception.getMessage()).isEqualTo("text cannot be null");
    }

    @Test
    void testRfcExample()
    {
        // 1994-11-05T08:15:30-05:00 corresponds to November 5, 1994, 8:15:30 am, US Eastern Standard Time/
        // 1994-11-05T13:15:30Z corresponds to the same instant.
        final String a = "1994-11-05T08:15:30-05:00";
        final String b = "1994-11-05T13:15:30Z";
        final OffsetDateTime dA = parser.parseDateTime(a);
        final OffsetDateTime dB = parser.parseDateTime(b);
        assertThat(formatter.formatUtc(dA)).isEqualTo(formatter.formatUtc(dB));
    }

    @Test
    void testBadSeparator()
    {
        final String a = "1994 11-05T08:15:30-05:00";
        assertThrows(DateTimeException.class, () -> parser.parseDateTime(a));
    }

    @Test
    void testParseNonDigit()
    {
        final String a = "199g-11-05T08:15:30-05:00";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(a));
        assertThat(exception.getMessage()).isEqualTo("Character g is not a digit");
    }

    @Test
    void testInvalidDateTimeSeparator()
    {
        final String a = "1994-11-05X08:15:30-05:00";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(a));
        assertThat(exception.getMessage()).isEqualTo("Expected character [T, t,  ] at position 11 '1994-11-05X08:15:30-05:00'");
    }

    @Test
    void testLowerCaseTseparator()
    {
        final String a = "1994-11-05t08:15:30z";
        assertThat(parser.parseDateTime(a)).isNotNull();
    }

    @Test
    void testSpaceAsSeparator()
    {
        assertThat(parser.parseDateTime("1994-11-05 08:15:30z")).isNotNull();
    }

    @Test
    void testMilitaryOffset()
    {
        assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-02-21T15:27:39+0000"));
    }

    @Test
    void testParseUnknownLocalOffsetConvention()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-02-21T15:27:39-00:00"));
        assertThat(exception.getMessage()).isEqualTo("Unknown 'Local Offset Convention' date-time not allowed");
    }

    @Test
    void testParseLowercaseZ()
    {
        assertThat(parser.parseDateTime("2017-02-21T15:27:39.000z")).isEqualTo(OffsetDateTime.parse("2017-02-21T15:27:39.000z"));
    }

    @Test
    void testTemporalAccessorYear()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isFalse();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isFalse();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isFalse();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isFalse();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isFalse();
        assertThat(parsed.getLong(ChronoField.YEAR)).isEqualTo(2017);
    }

    @Test
    void testTemporalAccessorMonth()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isFalse();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isFalse();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isFalse();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isFalse();

        assertThat(parsed.getLong(ChronoField.MONTH_OF_YEAR)).isEqualTo(1);
    }

    @Test
    void testTemporalAccessorDay()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isFalse();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isFalse();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isFalse();

        assertThat(parsed.getLong(ChronoField.DAY_OF_MONTH)).isEqualTo(27);
    }

    @Test
    void testTemporalAccessorMinute()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27T15:34");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isFalse();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isFalse();

        assertThat(parsed.getLong(ChronoField.MINUTE_OF_HOUR)).isEqualTo(34);
    }

    @Test
    void testTemporalAccessorSecond()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27T15:34:49");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isTrue();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isFalse();

        assertThat(parsed.getLong(ChronoField.SECOND_OF_MINUTE)).isEqualTo(49);
    }

    @Test
    void testTemporalAccessorNanos()
    {
        final TemporalAccessor parsed = ITU.parseLenient("2017-01-27T15:34:49.987654321");
        assertThat(parsed.isSupported(ChronoField.YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MONTH_OF_YEAR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.DAY_OF_MONTH)).isTrue();
        assertThat(parsed.isSupported(ChronoField.HOUR_OF_DAY)).isTrue();
        assertThat(parsed.isSupported(ChronoField.MINUTE_OF_HOUR)).isTrue();
        assertThat(parsed.isSupported(ChronoField.SECOND_OF_MINUTE)).isTrue();
        assertThat(parsed.isSupported(ChronoField.NANO_OF_SECOND)).isTrue();

        assertThat(parsed.getLong(ChronoField.NANO_OF_SECOND)).isEqualTo(987654321);
    }

    @Test
    void testParseLeapSecondWhenNoTimeOffsetPresent()
    {
        ITU.parseLenient("3011-10-02T22:00:60.003");
    }
}
