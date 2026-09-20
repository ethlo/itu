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
import java.time.format.DateTimeParseException;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Drives every entry of {@code duration-corpus.json} (see {@link DurationCase}) through the duration parser. Besides
 * what the entry states, every successfully parsed value must survive a round trip through its normalized form, and
 * every input that java.time can also parse (anything without weeks) must agree with it.
 */
@Tag("CorrectnessTest")
public class DurationCorpusTest
{
    public static List<DurationCase> corpus() throws IOException
    {
        return new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .readValue(DurationCorpusTest.class.getResource("/duration-corpus.json"), new TypeReference<List<DurationCase>>()
                {
                });
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("corpus")
    void parse(final DurationCase c)
    {
        final Duration result;
        try
        {
            result = c.getOffset() != null ? ITU.parseDuration(c.getInput(), c.getOffset()) : ITU.parseDuration(c.getInput());
        }
        catch (DateTimeParseException exc)
        {
            if (c.getError() == null)
            {
                throw exc;
            }
            assertThat(exc).hasMessage(c.getError());
            if (c.getErrorIndex() != null)
            {
                assertThat(exc.getErrorIndex()).as("error index for '%s'", c.getInput()).isEqualTo(c.getErrorIndex());
            }
            return;
        }

        if (c.getError() != null)
        {
            fail("Expected error '%s' when parsing '%s', got %s", c.getError(), c.getInput(), result);
        }
        if (c.getSeconds() != null)
        {
            assertThat(result.getSeconds()).as("seconds of '%s'", c.getInput()).isEqualTo(c.getSeconds());
        }
        if (c.getNanos() != null)
        {
            assertThat(result.getNanos()).as("nanos of '%s'", c.getInput()).isEqualTo(c.getNanos());
        }
        if (c.getNormalized() != null)
        {
            assertThat(result.normalized()).as("normalized form of '%s'", c.getInput()).isEqualTo(c.getNormalized());
        }

        assertThat(ITU.parseDuration(result.normalized())).as("round trip of '%s' through %s", c.getInput(), result.normalized()).isEqualTo(result);

        final String text = c.getOffset() != null ? c.getInput().substring(c.getOffset()) : c.getInput();
        if (text.indexOf('W') == -1 && text.indexOf('w') == -1)
        {
            assertThat(result.toDuration()).as("java.time's reading of '%s'", text).isEqualTo(java.time.Duration.parse(text));
        }
    }
}
