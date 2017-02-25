package com.ethlo.util;

/**
 * 
 * @author Morten Haraldsen
 *
 */
public class CharArrayNumberUtil 
{
	final static char [] DigitTens = {
    	'0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
    	'1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
    	'2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
    	'3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
    	'4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
    	'5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
    	'6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
    	'7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
    	'8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
    	'9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    	}; 

    final static char [] DigitOnes = { 
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	};
	
    /**
     * All possible chars for representing a number as a String in radix 36
     */
    public final static char[] RADIX36_DIGITS = {
	'0' , '1' , '2' , '3' , '4' , '5' ,
	'6' , '7' , '8' , '9' , 'a' , 'b' ,
	'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
	'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
	'o' , 'p' , 'q' , 'r' , 's' , 't' ,
	'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };
    
    /**
     * All possible chars for representing a number as a String in radix 62 (case-sensitive)
     */
    public final static char[] RADIX62_DIGITS = {
	'0' , '1' , '2' , '3' , '4' , '5' ,
	'6' , '7' , '8' , '9' , 'A' , 'B' ,	
	'C' , 'D' , 'E' , 'F' ,	'G' , 'H' ,	
	'I' , 'J' , 'K' , 'L' ,	'M' , 'N' ,	
	'O' , 'P' , 'Q' , 'R' ,	'S' , 'T' ,	
	'U' , 'V' , 'W' , 'X' ,	'Y' , 'Z',	
	'a' , 'b' ,	'c' , 'd' ,	'e' , 'f' ,	
	'g' , 'h' ,	'i' , 'j' ,	'k' , 'l' ,	
	'm' , 'n' ,	'o' , 'p' ,	'q' , 'r' ,	
	's' , 't' , 'u' , 'v' ,	'w' , 'x' ,	
	'y' , 'z'
	};
	
	protected static int digitRadix36(char c)
	{
		if(c >= 48 && c <= 57)
		{
			// Numeric
			return c - 48;
		}
		else if (c >= 65 && c <= 90)
		{
			// Upper-case letter
			return c - 55;
		}
		else if (c >= 97 && c <= 122)
		{
			// Lower-case letter
			return c - 87;
		}
		throw new IllegalArgumentException("Invalid character: " + c);
	}
	
	protected static int digit(char c, int radix) 
	{
		final int res = digitRadix36(c);
		if (res < radix)
		{
			return res;
		}
		throw new IllegalArgumentException("Value of " + c 
			+ " is not representable in radix " + radix);
	}

	public static boolean isDigit(char c)
	{
		return (c >= 48 && c <= 57);
	}
	
	public static boolean isDigits(char[] str)
	{
		for (char c : str)
		{
			if (! isDigit(c))
			{
				return false;
			}
		}
		return true;
	}

	public static boolean isDigitBase62(char c) 
	{
		return (c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122);
	}
	
	public static boolean isDigitsBase62(char[] str) 
	{
		for (char c : str)
		{
			if (! isDigitBase62(c))
			{
				return false;
			}
		}
		return true;
	}
}
