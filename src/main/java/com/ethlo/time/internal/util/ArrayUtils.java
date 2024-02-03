package com.ethlo.time.internal.util;

public class ArrayUtils
{
    public static char[] merge(char[] a, char[] b)
    {
        final char[] result = new char[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
