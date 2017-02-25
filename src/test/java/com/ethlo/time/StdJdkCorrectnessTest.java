package com.ethlo.time;

import org.junit.Ignore;
import org.junit.Test;

public class StdJdkCorrectnessTest extends CorrectnessTest
{
    @Override
    protected StdJdkInternetDateTimeUtil getInstance()
    {
        return new StdJdkInternetDateTimeUtil(FractionType.MILLISECONDS);
    }
    
    @Override
    @Test
    @Ignore
    public void testFormat4TrailingNoise()
    {
        
    }
}
