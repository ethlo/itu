package com.ethlo.time.internal;

import java.time.format.DateTimeParseException;

public class ErrorUtil
{
    private ErrorUtil(){}

    public static DateTimeParseException raiseUnexpectedCharacter(String chars, int index)
    {
        throw new DateTimeParseException("Unexpected character " + chars.charAt(index) + " at position " + (index + 1) + ": " + chars, chars, index);
    }

    public static void raiseUnexpectedEndOfText(final String chars, final int offset)
    {
        throw new DateTimeParseException("Unexpected end of input: " + chars, chars, offset);
    }
}
