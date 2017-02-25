package com.ethlo.time;

public class Assert
{
    public static void isTrue(boolean expr, String msg)
    {
        if (! expr)
        {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void isTrue(boolean b)
    {
        isTrue(b, "Expression must be true");
    }
}
