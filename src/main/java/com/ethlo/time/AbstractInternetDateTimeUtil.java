package com.ethlo.time;

import java.time.DateTimeException;

public abstract class AbstractInternetDateTimeUtil implements InternetDateTimeUtil
{
    private static final int MAX_FRACTION_DIGITS = 9;
    
    private final boolean unknownLocalOffsetConvention;

    public AbstractInternetDateTimeUtil(boolean unknownLocalOffsetConvention)
    {
        this.unknownLocalOffsetConvention = unknownLocalOffsetConvention;
    }

    @Override
    public boolean allowUnknownLocalOffsetConvention()
    {
        return unknownLocalOffsetConvention;
    }
    
    protected void failUnknownLocalOffsetConvention()
    {
        throw new DateTimeException("Unknown Local Offset Convention date-times not allowed");
    }

    protected void assertMaxFractionDigits(int fractionDigits)
    {
        if (fractionDigits > MAX_FRACTION_DIGITS )
        {
            throw new DateTimeException("Maximum supported number of fraction digits in second is " 
                + MAX_FRACTION_DIGITS + ", got " + fractionDigits);
        }
    }
}