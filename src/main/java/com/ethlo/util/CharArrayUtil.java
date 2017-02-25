package com.ethlo.util;

import java.util.Arrays;

/**
 * This class implements a few useful methods to avoid having to convert to string 
 * to perform substring, equals, upper/lower-case, etc.
 * 
 * @author Morten Haraldsen
 */
public final class CharArrayUtil 
{
	private CharArrayUtil()
	{
		
	}
	
    public static char[] zeroPad(char[] str, int length)
    {
    	final char[] res = new char[length];
		System.arraycopy(str, 0, res, length - str.length, str.length);
		Arrays.fill(res, 0, length - str.length, '0');
        return res;
    }
    
    /**
     * Check whether this char is a letter or digit in the ASCII encoding
     * @param c The char to test
     * @return True if ASCII, otherwise false
     */
    public static boolean isAsciiLetterOrDigit(char c)
    {
    	final int cp = (int) c;
    	return 
    		(cp >= 65 && cp <= 90) 
    	||  (cp >= 97 && cp <= 122)
    	||  (cp >= 48 && cp <= 57);
    }
    
    /**
     * Check if all characters in the array are ASCII letter or digit
     * @param arr the character array to check
     * @return true if all are ASCII alpha/numeric
     */
    public static boolean isAsciiAlphaNum(char[] arr)
    {
        for (char c : arr) {
            if (!isAsciiLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }
    
    public static int indexOf(char[] arr, char needle)
    {
    	return indexOf(arr, needle, 0);
    }
    
    public static int indexOf(char[] arr, char needle, int offset)
    {
    	for (int i = offset; i < arr.length; i++)
    	{
    		if (arr[i] == needle)
    		{
    			return i;
    		}
    	}
    	return -1;
    }
    
    /**
	 * Merge two char arrays
	 * @param a
	 * @param b
	 * @return
	 */
	public static char[] merge(char[] a, char[] b) 
	{
		final char[] res = new char[a.length + b.length];
		System.arraycopy(a, 0, res, 0, a.length);
		System.arraycopy(b, 0, res, a.length, b.length);
		return res;
	}
	
	/**
	 * Merge multiple char arrays
	 * @return
	 */
	public static char[] merge(char[]... arrs) 
	{
		int totalLen = 0;
		for (char[] arr : arrs)
		{
			totalLen+=arr.length;
		}
		final char[] res = new char[totalLen];
		int offset = 0;
		for (char[] arr : arrs)
		{
			System.arraycopy(arr, 0, res, offset, arr.length);
			offset += arr.length;
		}
		return res;
	}
	
	public static char[] toLowerCase(char[] arr) 
	{
		final char[] res = new char[arr.length];
		for (int i = 0; i < arr.length; i++)
		{
			res[i] = Character.toLowerCase(arr[i]);
		}
		return res;
	}
	
	public static char[] toUpperCase(char[] arr) 
	{
		final char[] res = new char[arr.length];
		for (int i = 0; i < arr.length; i++)
		{
			res[i] = Character.toUpperCase(arr[i]);
		}
		return res;
	}
	
	public static char[] substring(char[] src, int offset, int length)
	{
		final char[] res = new char[length];
		System.arraycopy(src, offset, res, 0, length);
		return res;
	}
	
	public static boolean equals(char[] a, char[] b)
	{
		return Arrays.equals(a, b);
	}

	public static boolean equalsIgnoreCase(char[] a, char[] b)
	{
		return equals(toLowerCase(a), toLowerCase(b));
	}

	public static boolean isNumeric(char[] chars)
	{
		for (char c : chars)
		{
            if (! Character.isDigit(c))
            {
                return false;
            }
        }
        return true;
	}
	
	public static boolean isDigits(char[] chars, int offset)
	{
		for (int i = offset; i < chars.length; i++)
		{
            if (! Character.isDigit(chars[i]))
            {
                return false;
            }
        }
        return true;
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

    private static boolean isDigit(char c)
    {
        return c == '0'
            || c == '1'
            || c == '2'
            || c == '3'
            || c == '4'
            || c == '5'
            || c == '6'
            || c == '7'
            || c == '8'
            || c == '9';
    }

}
