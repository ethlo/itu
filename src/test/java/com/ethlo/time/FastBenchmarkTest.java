package com.ethlo.time;

public class FastBenchmarkTest extends BenchmarkTest
{
    @Override
    protected InternetDateTimeUtil getInstance()
    {
        return new FastInternetDateTimeUtil();
    }
}
