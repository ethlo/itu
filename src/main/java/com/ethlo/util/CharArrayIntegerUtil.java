package com.ethlo.util;

import com.ethlo.time.Assert;

/**
 * 
 * @author mha
 *
 */
public final class CharArrayIntegerUtil extends CharArrayNumberUtil
{
	private CharArrayIntegerUtil()
	{
		
	}
    
	/**
	 * Converts the integer value into a char representation, like int2char(10, 16) returns 'a' (hex)
	 * @param value The value to convert to char
	 * @return The char that represents the specified value
	 */
    public static char int2Char(int value, int radix) 
    {
    	Assert.isTrue(radix >= Character.MIN_RADIX || radix <= Character.MAX_RADIX, "radix must be between 1 and 36");
        Assert.isTrue(value < radix, "value must be < radix");
        return RADIX36_DIGITS[value];
    }
    
	public static int parsePositiveInt(char[] strNum)
	{
		return parsePositiveInt(strNum, 10, 0, strNum.length);
	}
	
	public static int parsePositiveInt(char[] strNum, int radix)
	{
		return parsePositiveInt(strNum, radix, 0, strNum.length);
	}
	
	public static int parsePositiveInt(char[] strNum, int radix, int offset, int length)
	{
		int result = 0;
		for (int i = offset; i < length; i++)
		{
			int digit = digit(strNum[i], radix);
			result *= radix;
			result -= digit;
		}
		return -result;
	}

    public static int toString(int i, char[] buf, int offset, int padTo)
    {
        return toString(i, 10, buf, offset, padTo);
    }

    /**
     * Handles only non-negative values
     * @param value
     * @param radix
     * @param buf
     * @param offset
     * @return
     */
    public static int toString(int value, int radix, char[] buf, int offset, int padTo) 
    {
        int charPos = offset + 32;
        value = -value;
        while (value <= -radix) 
        {
            buf[charPos--] = RADIX36_DIGITS[-(value % radix)];
            value = value / radix;
        }
        buf[charPos] = RADIX36_DIGITS[-value];
        
        final int length = ((32 + offset) - charPos) + 1;
        int l = length;
        while (l < padTo)
        {
            buf[--charPos] = '0';
            l++;
        }
        final int srcPos = charPos;
        System.arraycopy(buf, srcPos, buf, offset, padTo);
        return offset + length;
    }    
}
