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
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
public abstract class FormatterBenchmarkTest
{
    private static final OffsetDateTime input = OffsetDateTime.parse("2017-12-21T12:20:45.987654321Z");
    private static Rfc3339Formatter formatter;

    protected FormatterBenchmarkTest(Rfc3339Formatter formatter)
    {
        FormatterBenchmarkTest.formatter = formatter;
    }

    @Benchmark
    public void formatSeconds(final Blackhole blackhole)
    {
        blackhole.consume(formatter.formatUtc(input));
    }

    @Benchmark
    public void formatNanos(final Blackhole blackhole)
    {
        blackhole.consume(formatter.formatUtcNano(input));
    }
}
