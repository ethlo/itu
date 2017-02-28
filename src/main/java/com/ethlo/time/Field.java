package com.ethlo.time;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;

public enum Field
{
    // 2000-12-31T16:11:34.123456
    YEAR(4), MONTH(7), DAY(10), MINUTE(16), SECOND(19);
    
    private int len;
    
    private Field(int len)
    {
        this.len = len;
    }
    
    public int getLength()
    {
        return this.len;
    }

    public static Field valueOf(Class<? extends Temporal> type)
    {
        if (Year.class.equals(type))
        {
            return YEAR;
        }
        else if (YearMonth.class.equals(type))
        {
            return MONTH;
        }
        else if (LocalDate.class.equals(type))
        {
            return DAY;
        }
        else if (OffsetDateTime.class.equals(type))
        {
            return SECOND;
        }
        
        throw new IllegalArgumentException("Type " + type.getSimpleName() + " is not supported");
    }
}
