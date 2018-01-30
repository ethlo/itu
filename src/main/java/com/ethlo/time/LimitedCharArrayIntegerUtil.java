package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.time.DateTimeException;
import java.util.Arrays;

public final class LimitedCharArrayIntegerUtil
{
    private static final char ZERO = '0';
    private static final char[] DIGITS = {'0' , '1' , '2' , '3' , '4' , '5', '6' , '7' , '8' , '9'};
    private static final int TABLE_WIDTH = 4;
    private static final int RADIX = 10;
    private static final int MAX_INT_WIDTH = 10;
    private static final int TABLE_SIZE = (int)Math.pow(RADIX, TABLE_WIDTH);
    private static final char[] INT_CONVERSION_CACHE = new char[(TABLE_SIZE * TABLE_WIDTH) + MAX_INT_WIDTH];
    
    private LimitedCharArrayIntegerUtil(){}
    
    static
    {
        int offset = 0;
        for (int i = 0; i < TABLE_SIZE; i++)
        {
            toString(i, INT_CONVERSION_CACHE, offset, TABLE_WIDTH, false);
            offset += TABLE_WIDTH;
        }
    }
    
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
			int digit = digit(strNum[i]);
			result *= RADIX;
			result -= digit;
		}
		return -result;
	}
	
	public static int toString(int value, char[] buf, int offset, int padTo)
	{
	    return toString(value, buf, offset, padTo, true);
	}
	
    public static int toString(int value, char[] buf, int offset, int charLength, boolean useTable) 
    {
        if (useTable && value < TABLE_SIZE)
        {
            final int length = Math.min(TABLE_WIDTH, charLength);
            final int padPrefixLen = charLength - length;
            final int start = charLength > TABLE_WIDTH ? TABLE_WIDTH : TABLE_WIDTH - charLength;
            final int targetOffset = offset + (padPrefixLen > 0 ? padPrefixLen : 0);
            final int srcPos = (value * TABLE_WIDTH) + (charLength < TABLE_WIDTH ? start : 0);
            copy(INT_CONVERSION_CACHE, srcPos, buf, targetOffset, length);
            if (padPrefixLen > 0)
            {
                zeroFill(buf, offset, padPrefixLen);
            }
            return offset + charLength;
        }

        int charPos = offset + MAX_INT_WIDTH;
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
        
        final int length = ((MAX_INT_WIDTH + offset) - charPos) + 1;
        int l = length;
        while (l < charLength)
        {
            buf[--charPos] = ZERO;
            l++;
        }
        final int srcPos = charPos;
        copy(buf, srcPos, offset, charLength);
        return offset + length;
    }

    private static void zeroFill(char[] buf, int offset, int padPrefixLen)
    {
        Arrays.fill(buf, offset, offset + padPrefixLen, ZERO);
    }

    private static void copy(char[] buf, int srcPos, int offset, int length)
    {
        copy(buf, srcPos, buf, offset, length);
    }
    
    private static void copy(char[] buf, int srcPos, char[] target, int offset, int length)
    {
        for (int i = 0; i < length; i++)
        {
            final int srcLoc = srcPos + i; 
            target[offset + i] = buf[srcLoc];
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
        return (c >= ZERO && c <= '9');
    }
    
    protected static int digit(char c) 
    {
        return c - ZERO;
    }
}
