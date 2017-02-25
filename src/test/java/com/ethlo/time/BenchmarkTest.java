package com.ethlo.time;

import org.junit.Test;

public abstract class BenchmarkTest extends AbstractTest<InternetDateTimeUtil>
{
    @Test
    public void testParsePerformance()
    {
        final int runs = 25_000_000;
        final String s = "2017-12-21T15:27:39.987654321Z";
        perform(runs, f->instance.parse(s), instance.getClass().getSimpleName());
    }
}