package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2026 Morten Haraldsen @ethlo
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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.ethlo.time.internal.fixed.ITUEpochParser;

/**
 * Epoch text is checked against java.time, which is the reference for what an epoch count means, and the
 * {@code char[]} path is checked against the String path, as for the date-time parsers.
 */
public class EpochParseTest
{
    /**
     * Digits on both sides of the window, so that reading past either edge changes the value
     */
    private static final String JUNK_BEFORE = "9-";
    private static final String JUNK_AFTER = "5";

    static List<Long> epochSeconds()
    {
        final List<Long> seconds = new ArrayList<>();
        seconds.add(0L);
        seconds.add(1L);
        seconds.add(-1L);
        seconds.add(86_399L);
        seconds.add(86_400L);
        seconds.add(-86_400L);
        seconds.add(-86_401L);
        seconds.add(951_782_400L); // 2000-02-29
        seconds.add(1_695_300_000L);
        seconds.add(4_107_542_399L); // 2100-02-28T23:59:59Z, the non-leap century
        seconds.add(ITUEpochParser.MIN_EPOCH_SECOND);
        seconds.add(ITUEpochParser.MAX_EPOCH_SECOND);
        final Random random = new Random(42);
        for (int i = 0; i < 2_000; i++)
        {
            seconds.add(ITUEpochParser.MIN_EPOCH_SECOND + Math.floorMod(random.nextLong(), ITUEpochParser.MAX_EPOCH_SECOND - ITUEpochParser.MIN_EPOCH_SECOND + 1));
        }
        return seconds;
    }

    @ParameterizedTest
    @MethodSource("epochSeconds")
    void secondsMatchJavaTime(final long seconds)
    {
        final String text = Long.toString(seconds);
        final DateTime result = ITU.parseEpochSecond(text);
        assertThat(result.toInstant()).isEqualTo(Instant.ofEpochSecond(seconds));
        assertThat(result.getMostGranularField()).isEqualTo(Field.SECOND);
        assertThat(result.getFractionDigits()).isZero();
        assertThat(result.getOffset()).contains(TimezoneOffset.UTC);
        assertThat(result.getParseLength()).isEqualTo(text.length());
        assertThat(result).hasToString(ITU.formatUtc(Instant.ofEpochSecond(seconds).atOffset(ZoneOffset.UTC)));
        assertSameFromCharArray(text, true, result);
    }

    @ParameterizedTest
    @MethodSource("epochSeconds")
    void millisMatchJavaTime(final long seconds)
    {
        // Every millisecond offset within the second, and the sign that comes with it before 1970
        for (final int milli : new int[]{0, 1, 500, 999})
        {
            final long millis = seconds * 1_000 + milli;
            if (Math.floorDiv(millis, 1_000L) > ITUEpochParser.MAX_EPOCH_SECOND)
            {
                continue;
            }
            final String text = Long.toString(millis);
            final DateTime result = ITU.parseEpochMilli(text);
            assertThat(result.toInstant()).as(text).isEqualTo(Instant.ofEpochMilli(millis));
            assertThat(result.getMostGranularField()).isEqualTo(Field.NANO);
            assertThat(result.getFractionDigits()).isEqualTo(3);
            assertThat(result.getOffset()).contains(TimezoneOffset.UTC);
            assertThat(result.getParseLength()).isEqualTo(text.length());
            assertThat(result).hasToString(ITU.formatUtcMilli(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC)));
            assertSameFromCharArray(text, false, result);
        }
    }

    @Test
    void negativeMillisFloorTowardsTheEarlierSecond()
    {
        assertThat(ITU.parseEpochMilli("-1")).hasToString("1969-12-31T23:59:59.999Z");
        assertThat(ITU.parseEpochMilli("-1000")).hasToString("1969-12-31T23:59:59.000Z");
        assertThat(ITU.parseEpochMilli("-1001")).hasToString("1969-12-31T23:59:58.999Z");
    }

    @Test
    void wholeSecondMillisKeepThreeFractionDigits()
    {
        assertThat(ITU.parseEpochMilli("1695300000000")).hasToString("2023-09-21T12:40:00.000Z");
    }

    @Test
    void bufferToEpochRoundTrips()
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        final char[] seconds = "1695300000".toCharArray();
        ITU.parseEpochSecond(seconds, 0, seconds.length, buffer);
        assertThat(buffer.toEpochSecond()).isEqualTo(1_695_300_000L);
        final char[] millis = "-1695300000123".toCharArray();
        ITU.parseEpochMilli(millis, 0, millis.length, buffer);
        assertThat(buffer.toEpochMilli()).isEqualTo(-1_695_300_000_123L);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "''|Unexpected end of input: |0",
            "-|Unexpected end of input: -|1",
            "+1|Expected digit at position 1, found +: +1|0",
            "12a|Expected digit at position 3, found a: 12a|2",
            "12.5|Expected digit at position 3, found .: 12.5|2",
            " 1|Expected digit at position 1, found  :  1|0",
            "--1|Expected digit at position 2, found -: --1|1",
            "253402300800|Epoch value outside years 0000-9999 (-62167219200 to 253402300799 seconds): 253402300800|0",
            "-62167219201|Epoch value outside years 0000-9999 (-62167219200 to 253402300799 seconds): -62167219201|0",
            "9999999999999999999|Epoch value outside years 0000-9999 (-62167219200 to 253402300799 seconds): 9999999999999999999|0",
            "-9999999999999999999|Epoch value outside years 0000-9999 (-62167219200 to 253402300799 seconds): -9999999999999999999|0"
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    void secondsErrors(final String text, final String message, final int errorIndex)
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseEpochSecond(text));
        assertThat(exc).hasMessage(message);
        assertThat(exc.getErrorIndex()).isEqualTo(errorIndex);
        assertSameErrorFromCharArray(text, true, exc);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "''|Unexpected end of input: |0",
            "1e3|Expected digit at position 2, found e: 1e3|1",
            "253402300800000|Epoch value outside years 0000-9999 (-62167219200000 to 253402300799999 milliseconds): 253402300800000|0",
            "-62167219200001|Epoch value outside years 0000-9999 (-62167219200000 to 253402300799999 milliseconds): -62167219200001|0",
            "1000000000000000000|Epoch value outside years 0000-9999 (-62167219200000 to 253402300799999 milliseconds): 1000000000000000000|0"
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    void millisErrors(final String text, final String message, final int errorIndex)
    {
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseEpochMilli(text));
        assertThat(exc).hasMessage(message);
        assertThat(exc.getErrorIndex()).isEqualTo(errorIndex);
        assertSameErrorFromCharArray(text, false, exc);
    }

    @Test
    void edgeOfRangeMillisAreAccepted()
    {
        assertThat(ITU.parseEpochMilli("253402300799999")).hasToString("9999-12-31T23:59:59.999Z");
        assertThat(ITU.parseEpochMilli("-62167219200000")).hasToString("0000-01-01T00:00:00.000Z");
    }

    @Test
    void bufferIsUntouchedOnFailure()
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        final char[] good = "1695300000".toCharArray();
        ITU.parseEpochSecond(good, 0, good.length, buffer);
        final DateTime before = buffer.toDateTime();
        final char[] bad = "16953x0000".toCharArray();
        assertThrows(DateTimeParseException.class, () -> ITU.parseEpochSecond(bad, 0, bad.length, buffer));
        assertThat(buffer.toDateTime()).isEqualTo(before);
    }

    @Test
    void windowArgumentsAreValidated()
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        final char[] chars = "123".toCharArray();
        assertThrows(NullPointerException.class, () -> ITU.parseEpochSecond(null, 0, 0, buffer));
        assertThrows(NullPointerException.class, () -> ITU.parseEpochMilli(chars, 0, 3, null));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseEpochSecond(chars, -1, 3, buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseEpochSecond(chars, 0, -1, buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseEpochMilli(chars, 1, 3, buffer));
    }

    private static int parseFromChars(final boolean seconds, final char[] chars, final int offset, final int length, final MutableDateTimeBuffer buffer)
    {
        return seconds ? ITU.parseEpochSecond(chars, offset, length, buffer) : ITU.parseEpochMilli(chars, offset, length, buffer);
    }

    private static void assertSameFromCharArray(final String text, final boolean seconds, final DateTime expected)
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        for (final char[] chars : new char[][]{text.toCharArray(), (JUNK_BEFORE + text + JUNK_AFTER).toCharArray()})
        {
            final int offset = chars.length == text.length() ? 0 : JUNK_BEFORE.length();
            final int consumed = parseFromChars(seconds, chars, offset, text.length(), buffer);
            assertThat(consumed).isEqualTo(text.length());
            assertThat(buffer.toDateTime()).as(text).isEqualTo(expected);
            assertThat(buffer.getFractionDigits()).isEqualTo(expected.getFractionDigits());
            assertThat(buffer.getOffsetTotalSeconds()).isZero();
        }
    }

    private static void assertSameErrorFromCharArray(final String text, final boolean seconds, final DateTimeParseException expected)
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        final char[] chars = (JUNK_BEFORE + text + JUNK_AFTER).toCharArray();
        final int offset = JUNK_BEFORE.length();
        final DateTimeParseException actual = assertThrows(DateTimeParseException.class, () -> parseFromChars(seconds, chars, offset, text.length(), buffer));
        assertThat(actual).hasMessage(expected.getMessage());
        assertThat(actual.getErrorIndex()).isEqualTo(expected.getErrorIndex());
        assertThat(actual.getParsedString()).isEqualTo(text);
    }
}
