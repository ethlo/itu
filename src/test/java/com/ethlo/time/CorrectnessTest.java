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
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("CorrectnessTest")
public abstract class CorrectnessTest extends AbstractTest
{
    private final String[] validFormats =
            {
                    "2017-02-21T15:27:39Z", "2017-02-21T15:27:39.123Z",
                    "2017-02-21T15:27:39.123456Z", "2017-02-21T15:27:39.123456789Z",
                    "2017-02-21T15:27:39+00:00", "2017-02-21T15:27:39.123+00:00",
                    "2017-02-21T15:27:39.123456+00:00", "2017-02-21T15:27:39.123456789+00:00",
                    "2017-02-21T15:27:39.1+00:00", "2017-02-21T15:27:39.12+00:00",
                    "2017-02-21T15:27:39.123+00:00", "2017-02-21T15:27:39.1234+00:00",
                    "2017-02-21T15:27:39.12345+00:00", "2017-02-21T15:27:39.123456+00:00",
                    "2017-02-21T15:27:39.1234567+00:00", "2017-02-21T15:27:39.12345678+00:00",
                    "2017-02-21T15:27:39.123456789+00:00"
            };

    private final String[] invalidFormats = {
            "2017-02-21T15:27:39", "2017-02-21T15:27:39.123",
            "2017-02-21T15:27:39.123456", "2017-02-21T15:27:39.123456789",
            "2017-02-21T15:27:39+0000", "2017-02-21T15:27:39.123+0000",
            "201702-21T15:27:39.123456+0000", "20170221T15:27:39.123456789+0000"};

    @Test
    public void testParseLeapSecondUTC()
    {
        verifyLeapSecondDateTime("1990-12-31T23:59:60Z", "1991-01-01T00:00:00Z", true);
    }

    @Test
    public void testParseDoubleLeapSecondUTC()
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
    public void testParseLeapSecondPST()
    {
        verifyLeapSecondDateTime("1990-12-31T15:59:60-08:00", "1991-01-01T00:00:00Z", true);
    }

    @Test
    public void parseWithFragmentsNoTimezone()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-12-21T12:20:45.987"));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testParseLeapSecondUTCJune()
    {
        final String leapSecondUTC = "1992-06-30T23:59:60Z";
        verifyLeapSecondDateTime(leapSecondUTC, "1992-07-01T00:00:00Z", true);
    }

    @Test
    public void testParseLeapSecondPSTJune()
    {
        verifyLeapSecondDateTime("1992-06-30T15:59:60-08:00", "1992-07-01T00:00:00Z", true);
    }

    @Test
    public void testParseLeapSecond()
    {
        verifyLeapSecondDateTime("1990-12-31T15:59:60-08:00", "1991-01-01T00:00:00Z", true);
    }

    @Test
    public void testParseLeapSecondPotentiallyCorrect()
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
    public void testFormat1()
    {
        final String s = "2017-02-21T15:27:39.0000000";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testFormat2()
    {
        final String s = "2017-02-21T15:27:39.000+30:00";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testFormat3()
    {
        final String s = "2017-02-21T10:00:00.000+12:00";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(date)).isEqualTo("2017-02-20T22:00:00.000Z");
    }

    @Test
    public void testInvalidNothingAfterFractionalSeconds()
    {
        final String s = "2017-02-21T10:00:00.12345";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testFormat4()
    {
        final String s = "2017-02-21T15:00:00.123Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(date)).isEqualTo("2017-02-21T15:00:00.123Z");
        assertThat(formatter.format(Date.from(date.atZoneSameInstant(ZoneOffset.UTC).toInstant()), "CET", 3)).isEqualTo("2017-02-21T16:00:00.123+01:00");
        assertThat(formatter.format(new Date(date.toInstant().toEpochMilli()), "EST", 3)).isEqualTo("2017-02-21T10:00:00.123-05:00");
    }

    @Test
    public void testParseMoreThanNanoResolutionFails()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime("2017-02-21T15:00:00.1234567891Z"));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testFormatMoreThanNanoResolutionFails()
    {
        final OffsetDateTime d = parser.parseDateTime("2017-02-21T15:00:00.123456789Z");
        final int fractionDigits = 10;
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> formatter.formatUtc(d, fractionDigits));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testFormatUtc()
    {
        final String s = "2017-02-21T15:09:03.123456789Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        final String expected = "2017-02-21T15:09:03Z";
        final String actual = formatter.formatUtc(date);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testFormatUtcMilli()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(date)).isEqualTo("2017-02-21T15:00:00.123Z");
    }

    @Test
    public void testFormatUtcMilliWithDate()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(new Date(date.toInstant().toEpochMilli()))).isEqualTo("2017-02-21T15:00:00.123Z");
    }

    @Test
    public void testFormatUtcMicro()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMicro(date)).isEqualTo("2017-02-21T15:00:00.123456Z");
    }

    @Test
    public void testFormatUtcNano()
    {
        final String s = "2017-02-21T15:00:00.987654321Z";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcNano(date)).isEqualTo(s);
    }

    @Test
    public void testFormat4TrailingNoise()
    {
        final String s = "2017-02-21T15:00:00.123ZGGG";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testFormat5()
    {
        final String s = "2017-02-21T15:27:39.123+13:00";
        final OffsetDateTime date = parser.parseDateTime(s);
        assertThat(formatter.formatUtcMilli(date)).isEqualTo("2017-02-21T02:27:39.123Z");
    }

    @Test
    public void testParseEmptyString()
    {
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(""));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testParseNull()
    {
        final NullPointerException exception = assertThrows(NullPointerException.class, () -> parser.parseDateTime(null));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testRfcExample()
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
    public void testBadSeparator()
    {
        final String a = "1994 11-05T08:15:30-05:00";
        assertThrows(DateTimeException.class, () -> parser.parseDateTime(a));
    }

    @Test
    public void testParseNonDigit()
    {
        final String a = "199g-11-05T08:15:30-05:00";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(a));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testInvalidDateTimeSeparator()
    {
        final String a = "1994-11-05X08:15:30-05:00";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(a));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testLowerCaseTseparator()
    {
        final String a = "1994-11-05t08:15:30z";
        assertThat(parser.parseDateTime(a)).isNotNull();
    }

    @Test
    public void testSpaceAsSeparator()
    {
        final String a = "1994-11-05 08:15:30z";
        assertThat(parser.parseDateTime(a)).isNotNull();
    }

    @Test
    public void testValid()
    {
        for (String f : this.validFormats)
        {
            assertThat(parser.isValid(f)).overridingErrorMessage("Expecting to be valid <%s>", f).isTrue();
        }
    }

    @Test
    public void testInvalid()
    {
        for (String f : this.invalidFormats)
        {
            assertThat(parser.isValid(f)).overridingErrorMessage(f).isFalse();
        }
    }

    @Test
    public void testMilitaryOffset()
    {
        final String s = "2017-02-21T15:27:39+0000";
        assertThat(parser.isValid(s)).isFalse();
    }

    @Test
    public void testParseUnknownLocalOffsetConvention()
    {
        final String s = "2017-02-21T15:27:39-00:00";
        final DateTimeException exception = assertThrows(DateTimeException.class, () -> parser.parseDateTime(s));
        assertThat(exception.getMessage()).isNotNull();
    }

    @Test
    public void testParseLowercaseZ()
    {
        final String s = "2017-02-21T15:27:39.000z";
        parser.parseDateTime(s);
    }

    @Test
    public void testFormatWithNamedTimeZoneDate()
    {
        final String s = "2017-02-21T15:27:39.321+00:00";
        final OffsetDateTime d = parser.parseDateTime(s);
        final String formatted = formatter.format(new Date(d.toInstant().toEpochMilli()), "EST");
        assertThat(formatted).isEqualTo("2017-02-21T10:27:39.321-05:00");
    }

    @Test
    public void testFormatUtcDate()
    {
        final String s = "2017-02-21T15:27:39.321+00:00";
        final OffsetDateTime d = parser.parseDateTime(s);
        final String formatted = formatter.formatUtc(new Date(d.toInstant().toEpochMilli()));
        assertThat(formatted).isEqualTo("2017-02-21T15:27:39.321Z");
    }

    @Test
    public void testFormatWithNamedTimeZone()
    {
        final String s = "2017-02-21T15:27:39.321+00:00";
        final OffsetDateTime d = parser.parseDateTime(s);
        final String formatted = formatter.format(Date.from(d.toInstant()), "America/New_York", 3);
        assertThat(formatted).isEqualTo("2017-02-21T10:27:39.321-05:00");
    }
}
