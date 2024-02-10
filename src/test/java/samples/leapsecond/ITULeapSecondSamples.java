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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.ethlo.time.ITU;
import com.ethlo.time.LeapSecondException;

/*

## Leap-second handling

 */
class ITULeapSecondSamples
{
    /*
    Parse a valid leap-second (i.e. it is on a date that would allow for it, and it is also in the list of known actual leap-seconds).
     */
    @Test
    void parseLeapSecond()
    {
        try
        {
            ITU.parseDateTime("1990-12-31T15:59:60-08:00");
        }
        catch (LeapSecondException exc)
        {
            // The following helper methods are available let you decide how to progress
            assertThat(exc.getSecondsInMinute()).isEqualTo(60);
            assertThat(exc.getNearestDateTime()).isEqualTo(OffsetDateTime.of(1990, 12, 31, 16, 0, 0, 0, ZoneOffset.ofHours(-8)));
            assertThat(exc.isVerifiedValidLeapYearMonth()).isTrue();
        }
    }
}
