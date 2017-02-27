package com.ethlo.time;

import java.time.DateTimeException;

import org.junit.Ignore;
import org.junit.Test;

public class StdJdkCorrectnessTest extends CorrectnessTest
{
    @Override
    protected InternetDateTimeUtil getInstance()
    {
        return new StdJdkInternetDateTimeUtil();
    }
    
    @Override
    @Test
    @Ignore
    public void testFormat4TrailingNoise()
    {
        
    }
    
    @Override
    @Test(expected=DateTimeException.class)
    @Ignore
    public void testParseUnknownLocalOffsetConvention()
    {
        
    }
}
