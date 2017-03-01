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
        return 200_000_000;
    }
}
