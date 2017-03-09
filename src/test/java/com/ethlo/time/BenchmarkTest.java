package com.ethlo.time;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

public abstract class BenchmarkTest extends AbstractTest<InternetDateTimeUtil>
{
    private final OffsetDateTime d = OffsetDateTime.of(2017, 12,21,15,27,39, 987, ZoneOffset.UTC);

    @Test
    public void testParsePerformance()
    {
        final String s = "2017-12-21T15:27:39.987Z";
        perform(f->instance.parse(s), instance.getClass().getSimpleName() + " - parse");
    }
    
    @Test
    public void testParseLenient()
    {
        final String s = "2017-12-21T12:20Z";
        final W3cDateTimeUtil w3cUtil = (W3cDateTimeUtil) instance;
        perform(f->w3cUtil.parseLenient(s), instance.getClass().getSimpleName() + " - parseLenient");
    }
    
    @Test
    public void testFormatPerformance()
    {
        perform(f->instance.formatUtc(d), instance.getClass().getSimpleName() + " - formatUtc");
    }
}