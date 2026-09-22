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
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * The buffer-writing overloads are held to the String methods: for every input and precision the characters
 * written must be the String, into both a {@code char[]} and a {@code byte[]}, at an offset, with nothing
 * outside the writer's window ({@code MAX_*_LENGTH} from the offset) touched.
 */
class FormatIntoBufferTest
{
    private static final int OFFSET = 7;

    @Test
    void dateTimeIntoBuffersMatchesString()
    {
        final char[] chars = new char[OFFSET + ITU.MAX_FORMAT_LENGTH + 3];
        final byte[] bytes = new byte[chars.length];
        for (final OffsetDateTime dateTime : dateTimes())
        {
            for (int digits = 0; digits <= 9; digits++)
            {
                assertSame(ITU.formatUtc(dateTime, digits), ITU.formatUtc(dateTime, digits, fill(chars), OFFSET), chars, ITU.formatUtc(dateTime, digits, fill(bytes), OFFSET), bytes);
                assertSame(ITU.format(dateTime, digits), ITU.format(dateTime, digits, fill(chars), OFFSET), chars, ITU.format(dateTime, digits, fill(bytes), OFFSET), bytes);
            }
        }
    }

    @Test
    void durationIntoBuffersMatchesString()
    {
        final char[] chars = new char[OFFSET + Duration.MAX_NORMALIZED_LENGTH + 3];
        final byte[] bytes = new byte[chars.length];
        for (final Duration duration : durations())
        {
            assertSame(duration.normalized(), duration.normalized(fill(chars), OFFSET), chars, duration.normalized(fill(bytes), OFFSET), bytes);
            for (final DurationUnit unit : DurationUnit.values())
            {
                assertSame(duration.normalized(unit), duration.normalized(unit, fill(chars), OFFSET), chars, duration.normalized(unit, fill(bytes), OFFSET), bytes);
            }
        }
    }

    /**
     * The offset conversion is the library's own arithmetic, so it is held to java.time's: every random input,
     * moved to UTC and to a second random offset, must format as java.time renders the moved value.
     */
    @Test
    void offsetConversionMatchesJavaTime()
    {
        final Random random = new Random(17);
        for (final OffsetDateTime dateTime : dateTimes())
        {
            assertThat(ITU.formatUtc(dateTime, 9)).isEqualTo(render(dateTime.withOffsetSameInstant(ZoneOffset.UTC)));
            final ZoneOffset target = ZoneOffset.ofTotalSeconds((random.nextInt(2 * 18 * 60 + 1) - 18 * 60) * 60);
            final OffsetDateTime moved = dateTime.withOffsetSameInstant(target);
            if (moved.getYear() < 0 || moved.getYear() > 9999)
            {
                assertThrows(DateTimeException.class, () -> ITU.format(dateTime.withOffsetSameInstant(target), 9));
                continue;
            }
            assertThat(ITU.format(moved, 9)).isEqualTo(render(moved));
            assertThat(ITU.formatUtc(moved, 9)).isEqualTo(render(dateTime.withOffsetSameInstant(ZoneOffset.UTC)));
        }
    }

    @Test
    void conversionAcrossTheYearRangeIsRejected()
    {
        assertThrows(DateTimeException.class, () -> ITU.formatUtc(OffsetDateTime.parse("9999-12-31T23:00:00-05:00"), 0));
        assertThrows(DateTimeException.class, () -> ITU.formatUtc(OffsetDateTime.parse("0000-01-01T01:00:00+05:00"), 0));
        assertThat(ITU.formatUtc(OffsetDateTime.parse("9999-12-31T23:00:00+05:00"), 0)).isEqualTo("9999-12-31T18:00:00Z");
        assertThat(ITU.formatUtc(OffsetDateTime.parse("0000-01-01T01:00:00-05:00"), 0)).isEqualTo("0000-01-01T06:00:00Z");
    }

    private static String render(final OffsetDateTime dateTime)
    {
        final int total = dateTime.getOffset().getTotalSeconds();
        final String tz = total == 0 ? "Z" : String.format(Locale.ROOT, "%s%02d:%02d", total < 0 ? "-" : "+", Math.abs(total) / 3600, Math.abs(total) / 60 % 60);
        return String.format(Locale.ROOT, "%04d-%02d-%02dT%02d:%02d:%02d.%09d%s", dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getNano(), tz);
    }

    @Test
    void bufferTooSmallIsRejectedBeforeAnythingIsWritten()
    {
        final OffsetDateTime dateTime = OffsetDateTime.parse("2012-12-27T19:07:22.123456789-03:00");
        final char[] chars = new char[ITU.MAX_FORMAT_LENGTH];
        final byte[] bytes = new byte[ITU.MAX_FORMAT_LENGTH];
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.formatUtc(dateTime, 0, chars, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.formatUtc(dateTime, 0, bytes, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.formatUtc(dateTime, 0, chars, -1));
        assertThat(new String(chars).trim()).isEmpty();
        assertThat(ITU.formatUtc(dateTime, 9, chars, 0)).isEqualTo("2012-12-27T22:07:22.123456789Z".length());

        final Duration duration = ITU.parseDuration("-P180DT23H27M19.193964536S");
        final char[] durationChars = new char[Duration.MAX_NORMALIZED_LENGTH];
        assertThrows(IndexOutOfBoundsException.class, () -> duration.normalized(durationChars, 1));
        assertThat(duration.normalized(durationChars, 0)).isEqualTo(duration.normalized().length());
    }

    private static void assertSame(final String expected, final int charLength, final char[] chars, final int byteLength, final byte[] bytes)
    {
        assertThat(new String(chars, OFFSET, charLength)).isEqualTo(expected);
        assertThat(new String(bytes, OFFSET, byteLength, StandardCharsets.ISO_8859_1)).isEqualTo(expected);
        // Nothing before the offset or after the writer's window; the window past the result may be scratch
        final int window = chars.length - OFFSET - 3;
        for (int i = 0; i < chars.length; i++)
        {
            if (i < OFFSET || i >= OFFSET + window)
            {
                assertThat(chars[i]).as("char %d of %s", i, expected).isEqualTo('#');
                assertThat(bytes[i]).as("byte %d of %s", i, expected).isEqualTo((byte) '#');
            }
        }
    }

    private static char[] fill(final char[] chars)
    {
        Arrays.fill(chars, '#');
        return chars;
    }

    private static byte[] fill(final byte[] bytes)
    {
        Arrays.fill(bytes, (byte) '#');
        return bytes;
    }

    private static List<OffsetDateTime> dateTimes()
    {
        final List<OffsetDateTime> list = new ArrayList<>();
        list.add(OffsetDateTime.parse("2012-12-27T19:07:22.123456789-03:00"));
        list.add(OffsetDateTime.parse("2017-12-21T12:20:45.987654321Z"));
        list.add(OffsetDateTime.parse("0000-01-01T00:00:00Z"));
        list.add(OffsetDateTime.parse("9999-12-31T23:59:59.999999999+14:00"));
        list.add(OffsetDateTime.parse("2000-02-29T00:00:00.000000001-00:30"));
        list.add(OffsetDateTime.parse("1970-01-01T00:00:00+05:45"));
        final Random random = new Random(11);
        for (int i = 0; i < 500; i++)
        {
            final long second = 1_000_000_000L + (long) (random.nextDouble() * 200_000_000_000L);
            final int offsetMinutes = random.nextInt(2 * 18 * 60 + 1) - 18 * 60;
            list.add(OffsetDateTime.of(java.time.LocalDateTime.ofEpochSecond(second, random.nextInt(1_000_000_000), ZoneOffset.UTC), ZoneOffset.ofTotalSeconds(offsetMinutes * 60)));
        }
        return list;
    }

    private static List<Duration> durations()
    {
        final List<Duration> list = new ArrayList<>();
        for (final String text : new String[]{"PT0S", "PT2H30M", "-P180DT23H27M19.193964536S", "P4W", "P1D", "PT0.000000001S", "-PT0.5S", "PT59S", "P4W6DT17H20M0.117392763S", "PT1H", "-P1W"})
        {
            list.add(ITU.parseDuration(text));
        }
        list.add(Duration.of(Long.MAX_VALUE, 999_999_999));
        list.add(Duration.of(Long.MIN_VALUE, 1));
        final Random random = new Random(13);
        for (int i = 0; i < 500; i++)
        {
            list.add(Duration.of(random.nextLong() >> random.nextInt(60), random.nextInt(1_000_000_000)));
        }
        return list;
    }
}
