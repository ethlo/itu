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
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Drives every entry of {@code date-time-corpus.json} through each implementation of the grammar. A case is
 * recorded once (see {@link DateTimeCase} for the fields) and then checked on the String path with the overloads
 * the entry names, and on the zero-allocation char[] path differentially against the String path.
 */
@Tag("CorrectnessTest")
public class DateTimeCorpusTest
{
    public static List<DateTimeCase> corpus() throws IOException
    {
        return new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .readValue(DateTimeCorpusTest.class.getResource("/date-time-corpus.json"), new TypeReference<List<DateTimeCase>>()
                {
                });
    }

    /**
     * The char[] path parses a window, which is the whole text at offset 0. An entry that starts at an offset is
     * about the ParsePosition overloads (which never apply the trailing junk rule) and has no char[] equivalent.
     */
    public static List<DateTimeCase> corpusAtOffsetZero() throws IOException
    {
        return corpus().stream().filter(c -> c.getOffset() == null || c.getOffset() == 0).collect(Collectors.toList());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("corpus")
    void stringPath(final DateTimeCase c)
    {
        if (!c.isLenient() && c.getConfig() != ParseConfig.DEFAULT)
        {
            fail("'%s': parseDateTime(..) always uses ParseConfig.DEFAULT, so a config is only meaningful with lenient: true", c.getInput());
        }

        final ParsePosition position = c.getOffset() != null ? new ParsePosition(c.getOffset()) : null;
        try
        {
            final DateTime result = parse(c, position);
            if (c.getError() != null)
            {
                fail("Expected error '%s' when parsing '%s', got %s", c.getError(), c.getInput(), result);
            }
            assertParsed(c, position, result);
        }
        catch (DateTimeException exc)
        {
            if (c.getError() == null)
            {
                throw exc;
            }
            assertFailed(c, position, exc);
        }

        if (!c.isLenient() && position == null)
        {
            assertThat(ITU.isValid(c.getInput())).isEqualTo(c.getError() == null);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("corpusAtOffsetZero")
    void charArrayPath(final DateTimeCase c)
    {
        CharArrayDifferential.assertSameAsStringPath(c.getInput(), c.getConfig());
    }

    /**
     * Runs the overloads the entry names. parseDateTime(..) is parseLenient(..) plus a granularity check, so the
     * strict form is also parsed leniently to get the DateTime the rest of the assertions are made against, and the
     * two must agree.
     */
    private static DateTime parse(final DateTimeCase c, final ParsePosition position)
    {
        if (c.isLenient())
        {
            return position != null ? ITU.parseLenient(c.getInput(), c.getConfig(), position) : ITU.parseLenient(c.getInput(), c.getConfig());
        }

        final OffsetDateTime strict = position != null ? ITU.parseDateTime(c.getInput(), position) : ITU.parseDateTime(c.getInput());
        final DateTime lenient = ITU.parseLenient(c.getInput(), c.getConfig(), new ParsePosition(position != null ? c.getOffset() : 0));
        assertThat(lenient.toOffsetDatetime()).as("strict and lenient parse of '%s'", c.getInput()).isEqualTo(strict);
        return lenient;
    }

    private static void assertParsed(final DateTimeCase c, final ParsePosition position, final DateTime result)
    {
        if (c.getExpected() != null)
        {
            assertThat(result.toString()).as("canonical form of '%s'", c.getInput()).isEqualTo(c.getExpected());
        }

        if (c.getInstant() != null || c.getExpected() == null)
        {
            final Instant expected = c.getInstant() != null ? c.getInstant() : javaTimeInstant(c);
            assertThat(result.toInstant())
                    .overridingErrorMessage("Expected %s (%s), was %s (%s) for '%s'", expected, asTs(expected), result.toInstant(), asTs(result.toInstant()), c.getInput())
                    .isEqualTo(expected);
        }

        if (c.getParseLength() != null)
        {
            assertThat(result.getParseLength()).as("parse length of '%s'", c.getInput()).isEqualTo(c.getParseLength());
        }

        if (position != null)
        {
            assertThat(position.getErrorIndex()).isEqualTo(-1);
            assertThat(position.getIndex()).as("position after parsing '%s'", c.getInput()).isEqualTo(c.getOffset() + result.getParseLength());
        }
    }

    private static void assertFailed(final DateTimeCase c, final ParsePosition position, final DateTimeException exc)
    {
        assertThat(exc).hasMessage(c.getError());

        if (c.getErrorIndex() != -1)
        {
            assertThat(exc).isInstanceOf(DateTimeParseException.class);
            assertThat(((DateTimeParseException) exc).getErrorIndex()).as("error index for '%s'", c.getInput()).isEqualTo(c.getErrorIndex());
            if (position != null)
            {
                assertThat(position.getErrorIndex()).isEqualTo(c.getErrorIndex());
                assertThat(position.getIndex()).isEqualTo(c.getErrorIndex());
            }
        }

        if (c.getLeapSecond() != null)
        {
            assertThat(exc).isInstanceOf(LeapSecondException.class);
            final LeapSecondException leap = (LeapSecondException) exc;
            assertThat(ITU.formatUtc(leap.getNearestDateTime())).isEqualTo(c.getLeapSecond().getNearest());
            assertThat(leap.isVerifiedValidLeapYearMonth()).isEqualTo(c.getLeapSecond().isVerified());
            assertThat(leap.getSecondsInMinute()).isEqualTo(60);
        }
    }

    private static Instant javaTimeInstant(final DateTimeCase c)
    {
        try
        {
            return Instant.parse(c.getInput());
        }
        catch (DateTimeException exc)
        {
            throw new IllegalArgumentException("java.time cannot parse '" + c.getInput() + "', so the entry must state 'expected' or 'instant': " + exc.getMessage(), exc);
        }
    }

    private static String asTs(final Instant instant)
    {
        return instant.getEpochSecond() + "," + instant.getNano();
    }
}
