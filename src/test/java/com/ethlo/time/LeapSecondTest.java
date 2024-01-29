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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("CorrectnessTest")
public abstract class LeapSecondTest extends AbstractTest
{
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

    @Test
    void testParseLenientPotentialLeapSecond()
    {
        final DateTimeException exc = assertThrows(DateTimeException.class, () -> ITU.parseLenient("2011-10-02T22:00:60.003"));
        assertThat(exc).hasMessage("Invalid value for SecondOfMinute (valid values 0 - 59): 60");
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
}
