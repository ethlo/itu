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

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.ethlo.time.DateTime;
import com.ethlo.time.Field;
import com.ethlo.time.ITU;
import com.ethlo.time.TimezoneOffset;

/*

## Formatting

 */
class ITUFormattingSamples
{
    /*
    The simplest and fastest way to format an RFC-3339 timestamp: in UTC, with the fraction digits chosen by the
    method name. Milliseconds is the most common choice for interchange.
     */
    @Test
    void formatRfc3339WithUTC()
    {
        final OffsetDateTime input = OffsetDateTime.of(2012, 12, 27, 19, 7, 22, 123456789, ZoneOffset.ofHoursMinutes(-3, 0));
        assertThat(ITU.formatUtc(input)).isEqualTo("2012-12-27T22:07:22Z");
        assertThat(ITU.formatUtcMilli(input)).isEqualTo("2012-12-27T22:07:22.123Z");
        assertThat(ITU.formatUtcMicro(input)).isEqualTo("2012-12-27T22:07:22.123456Z");
        assertThat(ITU.formatUtcNano(input)).isEqualTo("2012-12-27T22:07:22.123456789Z");
    }

    /*
    `format` keeps the offset of the input instead of converting to UTC. Like `formatUtc` it defaults to whole
    seconds; both take the number of fraction digits as a parameter, and `formatUtc` can also stop at a given field.
     */
    @Test
    void formatWithOffsetAndPrecision()
    {
        final OffsetDateTime input = OffsetDateTime.of(2012, 12, 27, 19, 7, 22, 123456789, ZoneOffset.ofHoursMinutes(-3, 0));
        assertThat(ITU.format(input)).isEqualTo("2012-12-27T19:07:22-03:00");
        assertThat(ITU.format(input, 9)).isEqualTo("2012-12-27T19:07:22.123456789-03:00");
        assertThat(ITU.formatUtc(input, 6)).isEqualTo("2012-12-27T22:07:22.123456Z");
        assertThat(ITU.formatUtc(input, Field.MINUTE)).isEqualTo("2012-12-27T22:07Z");
    }

    /*
    When the text is going into a buffer anyway - a socket, a file, a JSON generator - format straight into it and
    allocate nothing. The buffer needs room for `ITU.MAX_FORMAT_LENGTH` characters from the offset; the result is
    the first `length` of them. There are `char[]` and `byte[]` variants, and the output is ASCII, so the bytes are
    the text in UTF-8 or any other ASCII-compatible encoding.
     */
    @Test
    void formatIntoBuffer()
    {
        final OffsetDateTime input = OffsetDateTime.of(2012, 12, 27, 19, 7, 22, 123456789, ZoneOffset.ofHoursMinutes(-3, 0));
        final byte[] bytes = new byte[ITU.MAX_FORMAT_LENGTH];
        final int length = ITU.formatUtc(input, 3, bytes, 0);
        assertThat(new String(bytes, 0, length, StandardCharsets.US_ASCII)).isEqualTo("2012-12-27T22:07:22.123Z");

        final char[] chars = new char[ITU.MAX_FORMAT_LENGTH];
        assertThat(new String(chars, 0, ITU.format(input, 0, chars, 0))).isEqualTo("2012-12-27T19:07:22-03:00");
    }

    /*
    A `DateTime` formats to the granularity it carries, or to any coarser one. With an offset the result is
    RFC-3339; without one it is the local form.
     */
    @Test
    void formatWithDateTime()
    {
        final DateTime local = DateTime.of(2020, 11, 27, 12, 39, 19, null);
        assertThat(local).hasToString("2020-11-27T12:39:19");
        assertThat(local.toString(Field.MINUTE)).isEqualTo("2020-11-27T12:39");
        assertThat(local.toString(Field.DAY)).isEqualTo("2020-11-27");

        final DateTime withOffset = DateTime.of(2020, 11, 27, 12, 39, 19, 500_000_000, TimezoneOffset.ofHoursMinutes(1, 0), 1);
        assertThat(withOffset).hasToString("2020-11-27T12:39:19.5+01:00");
        assertThat(withOffset.toString(3)).isEqualTo("2020-11-27T12:39:19.500+01:00");
    }
}
