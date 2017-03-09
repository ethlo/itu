package com.ethlo.time;

public class ApacheFastDateUtilsBenchmarkTest extends BenchmarkTest
{
    @Override
    protected InternetDateTimeUtil getInstance()
    {
        return new ApacheFastDateUtilsInternetDateTimeUtil();
    }

    @Override
    protected long getRuns()
    {
        return 10_000_000;
    }
}
