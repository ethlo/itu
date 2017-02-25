package com.ethlo.time;

public class FastCorrectnessTest extends CorrectnessTest
{
    @Override
    protected InternetDateTimeUtil getInstance()
    {
        return new FastInternetDateTimeUtil();
    }
}
