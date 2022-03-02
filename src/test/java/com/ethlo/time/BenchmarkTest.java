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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class BenchmarkTest extends AbstractTest
{
    private static final Chronograph chronograph = Chronograph.create(CaptureConfig.minInterval(Duration.ofMillis(25)));
    private static final int REPEATS = 3; //Optional.ofNullable(System.getenv("REPEATS")).map(Integer::parseInt).orElse(3);
    private final OffsetDateTime d = OffsetDateTime.of(2017, 12, 21, 15, 27, 39, 987_000_000, ZoneOffset.UTC);

    @Override
    protected long getRuns()
    {
        return 10_000_000;
    }

    @RepeatedTest(REPEATS)
    public void testParse0()
    {
        final String name = parser.getClass().getSimpleName() + " - parse(0)";
        perform(() -> parser.parseDateTime("2017-12-21T12:20:45Z"), name);
    }

    @RepeatedTest(REPEATS)
    public void testParse3()
    {
        final String name = parser.getClass().getSimpleName() + " - parse(3)";
        perform(() -> parser.parseDateTime("2017-12-21T12:20:45.987Z"), name);
    }

    @RepeatedTest(REPEATS)
    public void testParse6()
    {
        final String name = parser.getClass().getSimpleName() + " - parse(6)";
        perform(() -> parser.parseDateTime("2017-12-21T12:20:45.987654Z"), name);
    }

    @RepeatedTest(REPEATS)
    public void testParse9()
    {
        final String name = parser.getClass().getSimpleName() + " - parse(9)";
        perform(() -> parser.parseDateTime("2017-12-21T12:20:45.987654321Z"), name);
    }

    @RepeatedTest(REPEATS)
    public void testFormat0()
    {
        final String name = parser.getClass().getSimpleName() + " - formatUtc(0)";
        if (formatter != null)
        {
            perform(() -> formatter.formatUtc(d), name);
        }
    }

    @RepeatedTest(REPEATS)
    public void testFormat3()
    {
        final String name = parser.getClass().getSimpleName() + " - formatUtc(3)";
        if (formatter != null)
        {
            perform(() -> formatter.formatUtcMilli(d), name);
        }
    }

    @RepeatedTest(REPEATS)
    public void testFormat6()
    {
        final String name = parser.getClass().getSimpleName() + " - formatUtc(6)";
        if (formatter != null)
        {
            perform(() -> formatter.formatUtcMicro(d), name);
        }
    }

    @RepeatedTest(REPEATS)
    public void testFormat9()
    {
        final String name = parser.getClass().getSimpleName() + " - formatUtc(9)";
        if (formatter != null)
        {
            perform(() -> formatter.formatUtcMicro(d), name);
        }
    }
}
