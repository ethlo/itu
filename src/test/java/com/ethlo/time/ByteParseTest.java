package com.ethlo.time;

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

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

/**
 * What the {@code byte[]} parsers add beyond the corpus differential: the epoch variants, windows longer than the
 * buffer's scratch (the allocating path, both its exact error and its success with trailing junk allowed), a
 * non-ASCII byte, and the argument checks.
 */
class ByteParseTest
{
    private final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();

    @Test
    void epochBytesMatchChars()
    {
        for (final String text : new String[]{"1695300000", "-1", "0", "-62167219200", "253402300799", "0000000000000000000000000000000000000000000001695300000"})
        {
            final byte[] bytes = ("x" + text + "y").getBytes(StandardCharsets.US_ASCII);
            final MutableDateTimeBuffer expected = new MutableDateTimeBuffer();
            assertThat(ITU.parseEpochSecond(bytes, 1, text.length(), buffer)).isEqualTo(ITU.parseEpochSecond(text.toCharArray(), 0, text.length(), expected));
            assertThat(buffer.toString()).isEqualTo(expected.toString());
            assertThat(ITU.parseEpochMilli(bytes, 1, text.length(), buffer)).isEqualTo(ITU.parseEpochMilli(text.toCharArray(), 0, text.length(), expected));
            assertThat(buffer.toString()).isEqualTo(expected.toString());
        }
    }

    @Test
    void windowLongerThanTheScratchGetsTheSameError()
    {
        final String text = "2012-12-27T19:07:22.123456789-03:00 and then a lot of trailing text that does not fit the scratch";
        final byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        final DateTimeParseException fromBytes = assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(bytes, 0, bytes.length, buffer));
        final DateTimeParseException fromChars = assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(text.toCharArray(), 0, text.length(), buffer));
        assertThat(fromBytes.getMessage()).isEqualTo(fromChars.getMessage());
        assertThat(fromBytes.getErrorIndex()).isEqualTo(fromChars.getErrorIndex()).isEqualTo(35);

        // With trailing junk allowed the same window parses, consuming the date-time only
        final ParseConfig lenient = ParseConfig.DEFAULT.withFailOnTrailingJunk(false);
        assertThat(ITU.parseLenient(bytes, 0, bytes.length, lenient, buffer)).isEqualTo(35);
        assertThat(buffer).hasToString("2012-12-27T19:07:22.123456789-03:00");
    }

    @Test
    void nonAsciiByteIsReportedAtItsIndex()
    {
        final byte[] bytes = "2012-12-27T19:07:22é".getBytes(StandardCharsets.ISO_8859_1);
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(bytes, 0, bytes.length, buffer));
        assertThat(exc.getErrorIndex()).isEqualTo(19);
        assertThat(exc.getMessage()).contains("é");
    }

    @Test
    void argumentsAreChecked()
    {
        final byte[] bytes = "2012-12-27".getBytes(StandardCharsets.US_ASCII);
        assertThrows(NullPointerException.class, () -> ITU.parseLenient((byte[]) null, 0, 3, buffer));
        assertThrows(NullPointerException.class, () -> ITU.parseLenient(bytes, 0, 3, null));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseLenient(bytes, -1, 3, buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseLenient(bytes, 0, -1, buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseLenient(bytes, 5, 10, buffer));
        assertThrows(NullPointerException.class, () -> ITU.parseEpochMilli((byte[]) null, 0, 3, buffer));
    }
}
