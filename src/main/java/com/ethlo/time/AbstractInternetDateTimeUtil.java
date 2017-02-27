package com.ethlo.time;

import java.time.DateTimeException;

public abstract class AbstractInternetDateTimeUtil implements InternetDateTimeUtil
{
    private final static int MAX_FRACTION_DIGITS = 9;
    
    private final boolean unknownLocalOffsetConvention;

    public AbstractInternetDateTimeUtil(boolean unknownLocalOffsetConvention)
    {
        this.unknownLocalOffsetConvention = unknownLocalOffsetConvention;
    }

    /**
    * RFC 3339 - 4.3. Unknown Local Offset Convention
    *
    * If the time in UTC is known, but the offset to local time is unknown,
    * this can be represented with an offset of "-00:00".  This differs
    * semantically from an offset of "Z" or "+00:00", which imply that UTC
    * is the preferred reference point for the specified time.
    *
    * @return True if allowed, otherwise false
    */
    public boolean allowUnknownLocalOffsetConvention()
    {
        return unknownLocalOffsetConvention;
    }
    
    protected void failUnknownLocalOffsetConvention()
    {
        throw new DateTimeException("Unknown Local Offset Convention date-times not allowed. See #allowUnknownLocalOffsetConvention()");
    }

    protected void assertMaxFractionDigits(int fractionDigits)
    {
        if (fractionDigits > MAX_FRACTION_DIGITS )
        {
            throw new DateTimeException("Maximum support number of fraction dicits in second is " + MAX_FRACTION_DIGITS + ", got " + fractionDigits);
        }
    }
}