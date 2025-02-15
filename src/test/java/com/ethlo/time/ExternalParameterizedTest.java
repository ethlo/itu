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
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExternalParameterizedTest
{
    private static final Logger logger = LoggerFactory.getLogger(ExternalParameterizedTest.class);

    public static List<TestParam> fromFile() throws IOException
    {
        final List<TestParam> result = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .readValue(ExternalParameterizedTest.class.getResource("/test-data.json"), new TypeReference<List<TestParam>>()
                {
                });
        logger.info("Loaded {} test-cases", result.size());
        return result;
    }

    @ParameterizedTest
    @MethodSource("fromFile")
    void testAll(TestParam param)
    {
        try
        {
            TemporalAccessor result;
            if (param.isLenient())
            {
                result = ITU.parseLenient(param.getInput(), param.getConfig() != null ? param.getConfig() : ParseConfig.DEFAULT);
            }
            else
            {
                result = ITU.parseDateTime(param.getInput());
            }

            if (param.getError() != null)
            {
                fail("Expected error '%s' when parsing '%s'", param.getError(), param.getInput());
            }

            // Compare to Java's parser result
            final Instant expected = getExpected(param);
            if (result instanceof DateTime)
            {
                assertEqualInstant(((DateTime) result).toInstant(), expected);
            }
            else
            {
                assertEqualInstant(((OffsetDateTime) result).toInstant(), expected);
            }
        }
        catch (DateTimeException exc)
        {
            if (param.getError() != null)
            {
                // expected an error, check if matching
                assertThat(exc).hasMessage(param.getError());

                if (param.getErrorIndex() != -1)
                {
                    assertThat(exc).isInstanceOf(DateTimeParseException.class);
                    final DateTimeParseException dateTimeParseException = (DateTimeParseException) exc;
                    assertThat(dateTimeParseException.getErrorIndex()).isEqualTo(param.getErrorIndex());
                }
            }
            else
            {
                throw exc;
            }
        }

    }

    private void assertEqualInstant(Instant result, Instant expected)
    {
        assertThat(result)
                .overridingErrorMessage("Expected %s (%s), was %s (%s)", expected, asTs(expected), result, asTs(result))
                .isEqualTo(expected);
    }

    private String asTs(Instant instant)
    {
        return instant.getEpochSecond() + "," + instant.getNano();
    }

    private Instant getExpected(TestParam testParam)
    {
        if (testParam.getExpected() != null)
        {
            return testParam.getExpected();
        }

        try
        {
            return Instant.parse(testParam.getInput());
        }
        catch (DateTimeException exc)
        {
            throw new IllegalArgumentException("Cannot parse using Instant: " + testParam.getInput() + ": " + exc.getMessage(), exc);
        }
    }
}
