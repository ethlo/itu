package com.ethlo.time;

import org.junit.Test;

public class PerformanceTest
{
    @Test
    public void testParsePerformance()
    {
        final int runs = 100_000;
        final InternetDateTimeUtil itu = new InternetDateTimeUtil();
        final long start = System.nanoTime();
        for (int i = 0; i < runs; i++)
        {
            final String s = "2017-02-21T15:27:39.000Z";
            itu.parse(s);
        }
        final long end = System.nanoTime();
        final double secs = (end - start) / 1_000_000_000D;
        System.out.println(runs / secs);
    }
}
