package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 Morten Haraldsen (ethlo)
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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public abstract class BenchmarkTest extends AbstractTest
{
    private final OffsetDateTime d = OffsetDateTime.of(2017, 12, 21, 15, 27, 39, 987, ZoneOffset.UTC);

    private static final Chronograph chronograph = Chronograph.create(CaptureConfig.minInterval(Duration.ofMillis(1)));

    @Override
    protected long getRuns()
    {
        return 1_000_000;
    }

    @Test
    public void testParsePerformance()
    {
        final Map<String, OffsetDateTime> formats = new LinkedHashMap<>();
        for (String f : Arrays.asList(
                "2017-12-21T15:27:39.987Z",
                "2017-12-21T15:27:39.98Z",
                "2017-12-21T15:27:39.9Z",
                "2017-12-21T15:27:39Z"
        ))
        {
            formats.put(f, OffsetDateTime.parse(f));
        }

        final String name = parser.getClass().getSimpleName() + " - parse";
        perform(chronograph, () ->
        {
            for (Map.Entry<String, OffsetDateTime> e : formats.entrySet())
            {
                assertThat(parser.parseDateTime(e.getKey())).isEqualTo(e.getValue());
            }
        }, name);
    }

    @Test
    public void testParseLenient()
    {
        final String s = "2017-12-21T12:20:45.987Z";
        final String name = parser.getClass().getSimpleName() + " - parseLenient";
        if (parser instanceof W3cDateTimeUtil)
        {
            final W3cDateTimeUtil w3cUtil = (W3cDateTimeUtil) parser;
            perform(chronograph, () -> w3cUtil.parseLenient(s), name);
        }
        else
        {
            unsupported(chronograph, name);
        }
    }

    @Test
    public void testFormatPerformance()
    {
        final String name = parser.getClass().getSimpleName() + " - formatUtc";
        if (formatter != null)
        {
            perform(chronograph, () -> formatter.formatUtc(d), name);
        }
        else
        {
            unsupported(chronograph, name);
        }
    }

    @AfterAll
    static void printStats()
    {
        System.out.println(chronograph.prettyPrint());
    }
}
