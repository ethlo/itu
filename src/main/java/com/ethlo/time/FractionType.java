package com.ethlo.time;

public enum FractionType
{
    NONE(0),
    MILLISECONDS(3),
    MICROSECONDS(6),
    NANOSECONDS(9);
    
    private FractionType(int digits)
    {
        this.digits = digits;
    }
    
    public int getDigits()
    {
        return digits;
    }
    
    private int digits;
}