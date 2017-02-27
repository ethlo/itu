package com.ethlo.time;

/**
 * 
 * @author mha
 *
 */
public final class LimitedCharArrayIntegerUtil
{
    public final static char[] DIGITS = {'0' , '1' , '2' , '3' , '4' , '5', '6' , '7' , '8' , '9'};
    
	public static int parsePositiveInt(char[] strNum, int radix, int startInclusive, int endExclusive)
	{
		int result = 0;
		for (int i = startInclusive; i < endExclusive; i++)
		{
			int digit = digit(strNum[i], radix);
			result *= radix;
			result -= digit;
		}
		return -result;
	}

    public static int toString(int value, char[] buf, int offset, int padTo) 
    {
        int charPos = offset + 32;
        value = -value;
        while (value <= -10) 
        {
            buf[charPos--] = DIGITS[-(value % 10)];
            value = value / 10;
        }
        buf[charPos] = DIGITS[-value];
        
        final int length = ((32 + offset) - charPos) + 1;
        int l = length;
        while (l < padTo)
        {
            buf[--charPos] = '0';
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
