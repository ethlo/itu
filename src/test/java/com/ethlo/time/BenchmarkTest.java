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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public abstract class BenchmarkTest extends AbstractTest
{
    private final OffsetDateTime d = OffsetDateTime.of(2017, 12, 21, 15, 27, 39, 987_000_000, ZoneOffset.UTC);
    private static final Chronograph chronograph = Chronograph.create(CaptureConfig.minInterval(Duration.ofMillis(25)));

    @Override
    protected long getRuns()
    {
        return 10_000_000;
    }

    @RepeatedTest(5)
    public void testParseNoFractions()
    {
        final String name = parser.getClass().getSimpleName() + " - parse none";
        perform(() -> parser.parseDateTime("2017-12-21T12:20:45Z"), name);
    }

    @RepeatedTest(5)
    public void testParseMilliFractions()
    {
        final String name = parser.getClass().getSimpleName() + " - parse milli";
        perform(() -> parser.parseDateTime("2017-12-21T12:20:45.987Z"), name);
    }

    @RepeatedTest(5)
    public void testParseMicroFractions()
    {
        final String name = parser.getClass().getSimpleName() + " - parse micro";
        perform(() -> parser.parseDateTime("2017-12-21T12:20:45.987654Z"), name);
    }

    @RepeatedTest(5)
    public void testParseNanoFractions()
    {
        final String name = parser.getClass().getSimpleName() + " - parse nano";
        perform(() -> parser.parseDateTime("2017-12-21T12:20:45.987654321Z"), name);
    }

    protected Chronograph getChronograph()
    {
        return chronograph;
    }

    @Test
    public void testParseLenient()
    {
        final String s = "2017-12-21T12:20:45.987Z";
        final String name = parser.getClass().getSimpleName() + " - parseLenient";
        if (parser instanceof W3cDateTimeUtil)
        {
            final W3cDateTimeUtil w3cUtil = (W3cDateTimeUtil) parser;
            perform(() -> w3cUtil.parseLenient(s), name);
        }
        else
        {
            unsupported(getChronograph(), name);
        }
    }

    @RepeatedTest(5)
    public void testFormatPerformance()
    {
        final String name = parser.getClass().getSimpleName() + " - formatUtc";
        if (formatter != null)
        {
            perform(() -> formatter.formatUtcMicro(d), name);
        }
        else
        {
            unsupported(getChronograph(), name);
        }
    }

    @AfterAll
    static void printStats()
    {
        //        System.out.println(Report.prettyPrint(getChronograph().getTaskData(), OutputConfig.EXTENDED.percentiles(90, 95, 99, 99.5), TableTheme.RED_HERRING));
    }
}
