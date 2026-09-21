package com.ethlo.time.internal.token;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2026 Morten Haraldsen @ethlo
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

import java.text.ParsePosition;
import java.util.Arrays;

import com.ethlo.time.Field;
import com.ethlo.time.token.DateTimeToken;

/**
 * A fraction of a second that may be absent: one of the separators followed by 1-9 digits, or nothing at all.
 * Consumes nothing when the next character is not a separator, so the following token sees the same position.
 * A separator that is not followed by a digit is an error, as it is for {@link FractionsToken}.
 */
public class OptionalFractionsToken implements DateTimeToken
{
    private static final FractionsToken FRACTIONS = new FractionsToken();

    private final char[] separators;

    public OptionalFractionsToken(final char... separators)
    {
        this.separators = separators;
    }

    @Override
    public int read(final String text, final ParsePosition parsePosition)
    {
        final int index = parsePosition.getIndex();
        if (index >= text.length())
        {
            return 0;
        }
        final char c = text.charAt(index);
        for (char sep : separators)
        {
            if (c == sep)
            {
                parsePosition.setIndex(index + 1);
                return FRACTIONS.read(text, parsePosition);
            }
        }
        return 0;
    }

    @Override
    public Field getField()
    {
        return Field.NANO;
    }

    @Override
    public String toString()
    {
        return "optionalFractions: " + Arrays.toString(separators);
    }
}
