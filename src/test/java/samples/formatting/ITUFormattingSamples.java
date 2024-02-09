package samples.formatting;

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

import com.ethlo.time.DateTime;
import com.ethlo.time.Field;
import com.ethlo.time.ITU;

/*

## Formatting

This is a collection of usage examples for formatting.

 */
class ITUFormattingSamples
{
    /*
    The simplest and fastest way to format an RFC-3339 timestamp by far!
     */
    @Test
    void formatRfc3339WithUTC()
    {
        final OffsetDateTime input = OffsetDateTime.of(2012, 12, 27, 19, 7, 22, 123456789, ZoneOffset.ofHoursMinutes(-3, 0));
        assertThat(ITU.formatUtcNano(input)).isEqualTo("2012-12-27T22:07:22.123456789Z");
        assertThat(ITU.formatUtcMicro(input)).isEqualTo("2012-12-27T22:07:22.123456Z");
        assertThat(ITU.formatUtcMilli(input)).isEqualTo("2012-12-27T22:07:22.123Z");
        assertThat(ITU.formatUtc(input)).isEqualTo("2012-12-27T22:07:22Z");
    }

    /*
     Format with `DateTime`.
     */
    @Test
    void formatWithDateTime()
    {
        final DateTime input = DateTime.of(2020, 11, 27, 12, 39, 19, null);
        assertThat(input.toString(Field.MINUTE)).isEqualTo("2020-11-27T12:39");
        assertThat(input.toString(Field.SECOND)).isEqualTo("2020-11-27T12:39:19");
    }
}
