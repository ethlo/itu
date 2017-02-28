package com.ethlo.time;

import java.time.DateTimeException;

/**
 * 
 * @author mha
 *
 */
public final class LimitedCharArrayIntegerUtil
{
    private final static char ZERO = '0';
    private final static char[] DIGITS = {'0' , '1' , '2' , '3' , '4' , '5', '6' , '7' , '8' , '9'};
    
	public static int parsePositiveInt(char[] strNum, int startInclusive, int endExclusive)
	{
	    if (endExclusive > strNum.length)
	    {
	        throw new DateTimeException("Unexpected end of expression at position " + strNum.length + " '" + new String(strNum) + "'");
	    }
	    
		int result = 0;
		for (int i = startInclusive; i < endExclusive; i++)
		{
		    if (! isDigit(strNum[i]))
		    {
		        throw new DateTimeException("Character " + strNum[i] + " is not a digit");
		    }
			int digit = digit(strNum[i], 10);
			result *= 10;
			result -= digit;
		}
		return -result;
	}

    public static int toString(int value, char[] buf, int offset, int padTo) 
    {
        int charPos = offset + 9;
        value = -value;
        int div;
        int rem;
        while (value <= -10) 
        {
            div = value / 10;
            rem = -(value - 10 * div);
            buf[charPos--] = DIGITS[rem];
            value = div;
        }
        buf[charPos] = DIGITS[-value];
        
        final int length = ((9 + offset) - charPos) + 1;
        int l = length;
        while (l < padTo)
        {
            buf[--charPos] = ZERO;
            l++;
        }
        final int srcPos = charPos;
        copy(buf, srcPos, offset, padTo);
        return offset + length;
    }

    private static void copy(char[] buf, int srcPos, int offset, int length)
    {
        for (int i = 0; i < length; i++)
        {
            buf[offset + i] = buf[srcPos + i];
        }
    }
    
    public static int indexOfNonDigit(char[] chars, int offset)
    {
        for (int i = offset; i < chars.length; i++)
        {
            if (! isDigit(chars[i]))
            {
                return i;
            }
        }
        return -1;
    }
    
    public static boolean isDigit(char c)
    {
        return (c >= 48 && c <= 57);
    }
    
    protected static int digit(char c, int radix) 
    {
        return c - 48;
    }
}
