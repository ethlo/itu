package com.ethlo.time.token;

import com.ethlo.time.Field;
import com.ethlo.time.internal.token.DigitsToken;
import com.ethlo.time.internal.token.FractionsToken;
import com.ethlo.time.internal.token.SeparatorToken;
import com.ethlo.time.internal.token.SeparatorsToken;
import com.ethlo.time.internal.token.TimeZoneOffsetToken;

public class DateTimeTokens
{
    public static DateTimeToken separators(char... anyOf)
    {
        if (anyOf == null || anyOf.length == 0)
        {
            throw new IllegalArgumentException("Need at least one separator character");
        }

        if (anyOf.length == 1)
        {
            return new SeparatorToken(anyOf[0]);
        }
        return new SeparatorsToken(anyOf);
    }

    public static DateTimeToken digits(Field field, int length)
    {
        return new DigitsToken(field, length);
    }

    public static DateTimeToken fractions()
    {
        return new FractionsToken();
    }

    public static DateTimeToken timeZoneOffset()
    {
        return new TimeZoneOffsetToken();
    }
}
