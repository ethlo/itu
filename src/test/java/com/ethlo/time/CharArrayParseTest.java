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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The zero-allocation char[] path is a second implementation of the same grammar, so it is tested differentially
 * against the String path (the reference) over the whole corpus, plus the behaviour that only exists on this path:
 * windows, buffer reuse, and the offset representation.
 */
@Tag("CorrectnessTest")
public class CharArrayParseTest
{
    private static final String VALID = "2017-05-01T16:23:12.123456789+05:30";

    public static List<TestParam> corpus() throws IOException
    {
        return ExternalParameterizedTest.fromFile();
    }

    /**
     * Inputs that probe every early-return and error branch of the parser, so the corpus is not the only line of defence
     */
    public static List<String> edgeCases()
    {
        final List<String> result = new ArrayList<>();
        result.add("");
        result.add("2");
        result.add("202");
        result.add("2020");
        result.add("2020-");
        result.add("2020-1");
        result.add("2020-01");
        result.add("2020-13");
        result.add("2020-01-");
        result.add("2020-01-0");
        result.add("2020-01-01");
        result.add("2020-01-01T");
        result.add("2020-01-01T1");
        result.add("2020-01-01T10");
        result.add("2020-01-01T10:");
        result.add("2020-01-01T10:0");
        result.add("2020-01-01T10:00");
        result.add("2020-01-01T10:00Z");
        result.add("2020-01-01T10:00z");
        result.add("2020-01-01T10:00+01:00");
        result.add("2020-01-01T10:00+01");
        result.add("2020-01-01T10:00+0100");
        result.add("2020-01-01T10:00-00:00");
        result.add("2020-01-01T10:00:");
        result.add("2020-01-01T10:00:0");
        result.add("2020-01-01T10:00:00");
        result.add("2020-01-01T10:00:00Z");
        result.add("2020-01-01T10:00:00+00:00");
        result.add("2020-01-01T10:00:00.");
        result.add("2020-01-01T10:00:00.1");
        result.add("2020-01-01T10:00:00.12");
        result.add("2020-01-01T10:00:00.123");
        result.add("2020-01-01T10:00:00.1234");
        result.add("2020-01-01T10:00:00.123456");
        result.add("2020-01-01T10:00:00.12345678");
        result.add("2020-01-01T10:00:00.123456789");
        result.add("2020-01-01T10:00:00.1234567890");
        result.add("2020-01-01T10:00:00.123456789012345");
        result.add("2020-01-01T10:00:00.123Z");
        result.add("2020-01-01T10:00:00.123+");
        result.add("2020-01-01T10:00:00.123+0");
        result.add("2020-01-01T10:00:00.123+01:0");
        result.add("2020-01-01T10:00:00.123+01:00");
        result.add("2020-01-01T10:00:00.123+01-00");
        result.add("2020-01-01T10:00:00.123-00:00");
        result.add("2020-01-01T10:00:00.123+19:00");
        result.add("2020-01-01T10:00:00.123-18:00");
        result.add("2020-01-01T10:00:00.123-18:01");
        result.add("2020-01-01T10:00:00.123+01:60");
        result.add("2020-01-01T10:00:00.123+1a:00");
        result.add("2020-01-01T10:00:00.123Zjunk");
        result.add("2020-01-01T10:00:00Zjunk");
        result.add("2020-01-01T10:00Zjunk");
        result.add("2020-01-01T10:00+01:00junk");
        result.add("2020-01-01T10:00:00.123+01:00junk");
        result.add("2020-02-30T10:00:00Z");
        result.add("2019-02-29T10:00:00Z");
        result.add("2020-02-29T10:00:00Z");
        result.add("2020-00-01T10:00:00Z");
        result.add("2020-01-00T10:00:00Z");
        result.add("2020-01-01T24:00:00Z");
        result.add("2020-01-01T23:60:00Z");
        result.add("2020-01-01T23:59:60Z");
        result.add("2016-12-31T23:59:60Z");
        result.add("2016-12-31T23:59:60+00:00");
        result.add("2016-12-31T23:59:60.123Z");
        result.add("2017-01-01T01:59:60+02:00");
        result.add("2015-06-30T23:59:60Z");
        result.add("2099-12-31T23:59:60Z");
        result.add("2020-01-01t10:00:00z");
        result.add("2020-01-01 10:00:00Z");
        result.add("2020-01-01X10:00:00Z");
        result.add("2020/01/01T10:00:00Z");
        result.add("2020-01-01T10.00:00Z");
        result.add("2020-01-01T10:00:00,123Z");
        result.add("abcd-01-01T10:00:00Z");
        result.add("2020-ab-01T10:00:00Z");
        result.add("2020-01-abT10:00:00Z");
        result.add("2020-01-01Tab:00:00Z");
        result.add("2020-01-01T10:ab:00Z");
        result.add("2020-01-01T10:00:abZ");
        result.add("2020-01-01T10:00:00.abcZ");
        result.add("2020-01-01T10:00:00.12aZ");
        result.add("0000-01-01T00:00:00Z");
        result.add("9999-12-31T23:59:59.999999999-18:00");
        result.add("2020-01-01T10:00:00٠Z");
        result.add("２０２０-01-01T10:00:00Z");
        return result;
    }

    @ParameterizedTest
    @MethodSource("corpus")
    void sameAsStringPathForCorpus(final TestParam param)
    {
        CharArrayDifferential.assertSameAsStringPath(param.getInput(), param.getConfig() != null ? param.getConfig() : ParseConfig.DEFAULT);
    }

    @ParameterizedTest
    @MethodSource("edgeCases")
    void sameAsStringPathForEdgeCases(final String input)
    {
        CharArrayDifferential.assertSameAsStringPath(input, ParseConfig.DEFAULT);
        CharArrayDifferential.assertSameAsStringPath(input, ParseConfig.STRICT);
        CharArrayDifferential.assertSameAsStringPath(input, ParseConfig.DEFAULT.withFailOnTrailingJunk(false));
        CharArrayDifferential.assertSameAsStringPath(input, ParseConfig.DEFAULT.withDateTimeSeparators('T', '|').withFractionSeparators('.', ','));
    }

    @Test
    void everyFieldIsOverwrittenOnReuse()
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        ITU.parseLenient(VALID.toCharArray(), 0, VALID.length(), buffer);
        assertThat(buffer.getMostGranularField()).isEqualTo(Field.NANO);
        assertThat(buffer.getNano()).isEqualTo(123456789);
        assertThat(buffer.hasOffset()).isTrue();

        final char[] year = "1999".toCharArray();
        assertThat(ITU.parseLenient(year, 0, 4, buffer)).isEqualTo(4);
        assertThat(buffer.getMostGranularField()).isEqualTo(Field.YEAR);
        assertThat(buffer.getYear()).isEqualTo(1999);
        assertThat(buffer.getMonth()).isZero();
        assertThat(buffer.getDayOfMonth()).isZero();
        assertThat(buffer.getHour()).isZero();
        assertThat(buffer.getMinute()).isZero();
        assertThat(buffer.getSecond()).isZero();
        assertThat(buffer.getNano()).isZero();
        assertThat(buffer.getFractionDigits()).isZero();
        assertThat(buffer.hasOffset()).isFalse();
        assertThat(buffer.getOffsetTotalSeconds()).isEqualTo(MutableDateTimeBuffer.NO_OFFSET);
        assertThat(buffer.getParseLength()).isEqualTo(4);
        assertThat(buffer.toDateTime()).isEqualTo(ITU.parseLenient("1999"));
    }

    @Test
    void bufferIsUntouchedOnFailure()
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        ITU.parseLenient(VALID.toCharArray(), 0, VALID.length(), buffer);
        final DateTime before = buffer.toDateTime();

        final char[] bad = "2017-05-01T16:23:12.123+25:00".toCharArray();
        assertThrows(java.time.DateTimeException.class, () -> ITU.parseLenient(bad, 0, bad.length, buffer));
        assertThat(buffer.toDateTime()).isEqualTo(before);
        assertThat(buffer.getParseLength()).isEqualTo(VALID.length());

        final char[] junk = "2017-05-01T16:23:12.123Zjunk".toCharArray();
        assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(junk, 0, junk.length, buffer));
        assertThat(buffer.toDateTime()).isEqualTo(before);
    }

    @Test
    void offsetRepresentation()
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();

        parse("2017-05-01T16:23", buffer);
        assertThat(buffer.hasOffset()).isFalse();
        assertThat(buffer.getParseLength()).isEqualTo(16);

        parse("2017-05-01T16:23Z", buffer);
        assertThat(buffer.hasOffset()).isTrue();
        assertThat(buffer.getOffsetTotalSeconds()).isZero();
        assertThat(buffer.getParseLength()).isEqualTo(17);

        parse("2017-05-01T16:23+00:00", buffer);
        assertThat(buffer.getOffsetTotalSeconds()).isZero();
        assertThat(buffer.getParseLength()).isEqualTo(22);

        parse("2017-05-01T16:23:12-03:30", buffer);
        assertThat(buffer.getOffsetTotalSeconds()).isEqualTo(-(3 * 3600 + 30 * 60));
        assertThat(buffer.toOffsetDateTime()).isEqualTo(OffsetDateTime.parse("2017-05-01T16:23:12-03:30"));

        parse("2017-05-01T16:23:12.5+18:00", buffer);
        assertThat(buffer.getOffsetTotalSeconds()).isEqualTo(18 * 3600);
        assertThat(buffer.getFractionDigits()).isEqualTo(1);
        assertThat(buffer.getNano()).isEqualTo(500_000_000);
    }

    /**
     * The window is the text: trailing junk inside it fails whatever the offset, unlike the String path with a
     * ParsePosition, and anything outside it is invisible
     */
    @Test
    void trailingJunkRuleAppliesToTheWindowRegardlessOfOffset()
    {
        final String text = "some-data," + VALID + "junk,some-other-data";
        final char[] chars = text.toCharArray();
        final int offset = "some-data,".length();
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();

        // Exactly the date-time: the junk after the window is not read
        assertThat(ITU.parseLenient(chars, offset, VALID.length(), buffer)).isEqualTo(VALID.length());
        assertThat(buffer.toDateTime()).isEqualTo(ITU.parseLenient(VALID));

        // Window includes the junk: rejected, index relative to the window
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(chars, offset, VALID.length() + 4, buffer));
        assertThat(exc).hasMessage("Trailing junk data after position 36: " + VALID + "junk");
        assertThat(exc.getErrorIndex()).isEqualTo(VALID.length());
        assertThat(exc.getParsedString()).isEqualTo(VALID + "junk");

        // ... unless the config allows it, in which case the consumed length says where the date-time ended
        assertThat(ITU.parseLenient(chars, offset, VALID.length() + 4, ParseConfig.DEFAULT.withFailOnTrailingJunk(false), buffer)).isEqualTo(VALID.length());
    }

    @Test
    void errorIndexIsRelativeToTheWindow()
    {
        final char[] chars = "xxxxx2017-05-01X16:23:12Z".toCharArray();
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(chars, 5, chars.length - 5, buffer));
        assertThat(exc.getErrorIndex()).isEqualTo(10);
        assertThat(exc).hasMessage("Expected character [T, t,  ] at position 11, found X: 2017-05-01X16:23:12Z");
    }

    @Test
    void windowArgumentsAreValidated()
    {
        final char[] chars = VALID.toCharArray();
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        assertThrows(NullPointerException.class, () -> ITU.parseLenient(null, 0, 4, buffer));
        assertThrows(NullPointerException.class, () -> ITU.parseLenient(chars, 0, chars.length, null));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseLenient(chars, -1, 4, buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseLenient(chars, 0, -1, buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseLenient(chars, 1, chars.length, buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseLenient(chars, chars.length + 1, 0, buffer));

        // An empty window at the end of the array is legal, and fails like an empty string does
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseLenient(chars, chars.length, 0, buffer));
        assertThat(exc).hasMessage("Unexpected end of input: ");
    }

    @Test
    void emptyBuffer()
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        assertThat(buffer.getMostGranularField()).isNull();
        assertThat(buffer.includesGranularity(Field.YEAR)).isFalse();
        assertThat(buffer.hasOffset()).isFalse();
        assertThat(buffer.toString()).isEqualTo("<empty>");
    }

    @Test
    void granularity()
    {
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        parse("2017-05-01T16:23", buffer);
        assertThat(buffer.includesGranularity(Field.MINUTE)).isTrue();
        assertThat(buffer.includesGranularity(Field.SECOND)).isFalse();
        assertThat(Arrays.asList(Field.YEAR, Field.MONTH, Field.DAY, Field.HOUR, Field.MINUTE)).allMatch(buffer::includesGranularity);
    }

    private static void parse(final String text, final MutableDateTimeBuffer buffer)
    {
        final char[] chars = text.toCharArray();
        ITU.parseLenient(chars, 0, chars.length, buffer);
    }
}
