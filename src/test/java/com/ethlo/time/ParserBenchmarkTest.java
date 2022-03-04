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

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
public abstract class ParserBenchmarkTest
{
    private static Rfc3339Parser parser;

    protected ParserBenchmarkTest(Rfc3339Parser parser)
    {
        ParserBenchmarkTest.parser = parser;
    }

    @Benchmark
    public void testParse0(final Blackhole blackhole)
    {
        blackhole.consume(parser.parseDateTime("2017-12-21T12:20:45Z"));
    }

    @Benchmark
    public void testParse3(final Blackhole blackhole)
    {
        blackhole.consume(parser.parseDateTime("2017-12-21T12:20:45.987Z"));
    }

    @Benchmark
    public void testParse6(final Blackhole blackhole)
    {
        blackhole.consume(parser.parseDateTime("2017-12-21T12:20:45.987654Z"));
    }

    @Benchmark
    public void testParse9(final Blackhole blackhole)
    {
        blackhole.consume(parser.parseDateTime("2017-12-21T12:20:45.987654321Z"));
    }
}
