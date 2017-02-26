package com.ethlo.time;

import java.time.OffsetDateTime;

import org.junit.Test;

public abstract class BenchmarkTest extends AbstractTest<InternetDateTimeUtil>
{
    @Test
    public void testParsePerformance()
    {
        final String s = "2017-12-21T15:27:39.987654321Z";
        perform(f->instance.parse(s), instance.getClass().getSimpleName() + " - parse");
    }
    
    @Test
    public void testFormatPerformance()
    {
        final String s = "2017-12-21T15:27:39.987654321Z";
        final OffsetDateTime d = instance.parse(s);
        perform(f->instance.formatUtc(d), instance.getClass().getSimpleName() + " - formatUtc");
    }
}