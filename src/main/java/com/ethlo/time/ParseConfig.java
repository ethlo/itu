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

public class ParseConfig
{
    public static final ParseConfig DEFAULT = new ParseConfig(new char[]{'T', 't', ' '}, new char[]{'.'});

    private final char[] allowedDateTimeSeparators;
    private final char[] allowedFractionSeparators;

    private ParseConfig(char[] allowedDateTimeSeparators, char[] allowedFractionSeparators)
    {
        this.allowedDateTimeSeparators = allowedDateTimeSeparators;
        this.allowedFractionSeparators = allowedFractionSeparators;
    }

    public ParseConfig withAllowedDateTimeSeparators(char... allowed)
    {
        assertChars(allowed);
        return new ParseConfig(allowed, allowedFractionSeparators);
    }

    private void assertChars(char[] allowed)
    {
        if (allowed == null)
        {
            throw new IllegalArgumentException("Cannot have null array of characters");
        }
        if (allowed.length == 0)
        {
            throw new IllegalArgumentException("Must have at least one character in allowed list");
        }
    }

    public ParseConfig withAllowedFractionSeparators(char... allowed)
    {
        assertChars(allowed);
        return new ParseConfig(allowedDateTimeSeparators, allowed);
    }

    public char[] getAllowedDateTimeSeparators()
    {
        return allowedDateTimeSeparators;
    }

    public boolean isAllowedDateTimeSeparator(char needle)
    {
        for (char c : allowedDateTimeSeparators)
        {
            if (c == needle)
            {
                return true;
            }
        }
        return false;
    }

    public boolean isAllowedFractionSeparator(char needle)
    {
        for (char c : allowedFractionSeparators)
        {
            if (c == needle)
            {
                return true;
            }
        }
        return false;
    }
}