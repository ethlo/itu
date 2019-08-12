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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

public abstract class BenchmarkTest extends AbstractTest<Rfc3339>
{
    private final OffsetDateTime d = OffsetDateTime.of(2017, 12, 21, 15, 27, 39, 987, ZoneOffset.UTC);

    @Test
    public void testParsePerformance()
    {
        final String s = "2017-12-21T15:27:39.987Z";
        perform(f -> instance.parseDateTime(s), instance.getClass().getSimpleName() + " - parse");
    }

    @Test
    public void testParseLenient()
    {
        final String s = "2017-12-21T12:20:45.987Z";
        if (instance instanceof W3cDateTimeUtil)
        {
            final W3cDateTimeUtil w3cUtil = (W3cDateTimeUtil) instance;
            perform(f -> w3cUtil.parseLenient(s), instance.getClass().getSimpleName() + " - parseLenient");
        }
    }

    @Test
    public void testFormatPerformance()
    {
        perform(f -> instance.formatUtc(d), instance.getClass().getSimpleName() + " - formatUtc");
    }
}
