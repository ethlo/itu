package com.ethlo.time;

public class FastBenchmarkTest extends BenchmarkTest
{
    @Override
    protected InternetDateTimeUtil getInstance()
    {
        return new FastInternetDateTimeUtil();
    }
    
    @Override
    protected long getRuns()
    {
        return 100_000_000;
    }
}
