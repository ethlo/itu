package com.ethlo.time;

public class StdJdkBenchmarkTest extends BenchmarkTest
{
    @Override
    protected StdJdkInternetDateTimeUtil getInstance()
    {
        return new StdJdkInternetDateTimeUtil();
    }

    @Override
    protected long getRuns()
    {
        return 20_000_000;
    }
}
