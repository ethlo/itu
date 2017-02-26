package com.ethlo.time;

public class StdJdkBenchmarkTest extends BenchmarkTest
{
    @Override
    protected StdJdkInternetDateTimeUtil getInstance()
    {
        return new StdJdkInternetDateTimeUtil(FractionType.MILLISECONDS);
    }

    @Override
    protected long getRuns()
    {
        return 5_000_000;
    }
}
