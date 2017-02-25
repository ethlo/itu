package com.ethlo.time;

public class StdJdkBenchmarkTest extends BenchmarkTest
{
    @Override
    protected StdJdkInternetDateTimeUtil getInstance()
    {
        return new StdJdkInternetDateTimeUtil(FractionType.MILLISECONDS);
    }
}
