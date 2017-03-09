package com.ethlo.time;

public class Java7BenchmarkTest extends BenchmarkTest
{
    @Override
    protected InternetDateTimeUtil getInstance()
    {
        return new Java7InternetDateTimeUtil();
    }

    @Override
    protected long getRuns()
    {
        return 10_000_000;
    }
}
