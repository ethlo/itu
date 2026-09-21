package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2024 Morten Haraldsen (ethlo)
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

import com.ethlo.time.internal.token.DigitsToken;
import com.ethlo.time.internal.token.FractionsToken;
import com.ethlo.time.internal.token.OptionalFractionsToken;
import com.ethlo.time.internal.token.SeparatorToken;
import com.ethlo.time.internal.token.SeparatorsToken;
import com.ethlo.time.internal.token.ZoneOffsetToken;
import com.ethlo.time.token.DateTimeToken;

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

    /**
     * A fraction of a second that may be absent, as in RFC-3339: one of the given separators followed by 1-9 digits,
     * or nothing. Unlike {@code separators('.'), fractions()} the separator belongs to this token, so a text
     * without a fraction parses with {@link Field#SECOND} granularity.
     *
     * @param anyOf The characters that may introduce the fraction
     * @return A token for an optional fraction
     */
    public static DateTimeToken optionalFractions(char... anyOf)
    {
        if (anyOf == null || anyOf.length == 0)
        {
            throw new IllegalArgumentException("Need at least one separator character");
        }
        return new OptionalFractionsToken(anyOf);
    }

    public static DateTimeToken zoneOffset()
    {
        return new ZoneOffsetToken();
    }
}
