package samples.leapsecond;

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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.ethlo.time.ITU;
import com.ethlo.time.LeapSecondException;

/*

## Leap seconds

 */
class ITULeapSecondSamples
{
    /*
    `java.time` cannot represent second 60, so a leap second is reported as a `LeapSecondException` rather than
    silently changed. The exception tells you whether the leap second is a real one (on the list of announced leap
    seconds) and gives the nearest representable date-time, which is the next minute.
     */
    @Test
    void parseLeapSecond()
    {
        final LeapSecondException exc = assertThrows(LeapSecondException.class, () -> ITU.parseDateTime("1990-12-31T15:59:60-08:00"));
        assertThat(exc.getSecondsInMinute()).isEqualTo(60);
        assertThat(exc.isVerifiedValidLeapYearMonth()).isTrue();
        assertThat(exc.getNearestDateTime()).isEqualTo(OffsetDateTime.of(1990, 12, 31, 16, 0, 0, 0, ZoneOffset.ofHours(-8)));
    }

    /*
    A second of 60 on a date that never had a leap second is still a leap-second exception, since the syntax is
    valid, but `isVerifiedValidLeapYearMonth` is false so you can choose to reject it.
     */
    @Test
    void parseUnknownLeapSecond()
    {
        final LeapSecondException exc = assertThrows(LeapSecondException.class, () -> ITU.parseDateTime("2020-06-30T23:59:60Z"));
        assertThat(exc.isVerifiedValidLeapYearMonth()).isFalse();
        assertThat(exc.getNearestDateTime().toString()).isEqualTo("2020-07-01T00:00Z");
    }
}
