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

import static com.ethlo.time.internal.fixed.ITUParser.SEPARATOR_LOWER;
import static com.ethlo.time.internal.fixed.ITUParser.SEPARATOR_SPACE;
import static com.ethlo.time.internal.fixed.ITUParser.SEPARATOR_UPPER;

import java.util.Arrays;
import java.util.Optional;

public class ParseConfig
{
    private static final char[] DEFAULT_DATE_TIME_SEPARATORS = new char[]{SEPARATOR_UPPER, SEPARATOR_LOWER, SEPARATOR_SPACE};
    private static final char[] RFC_3339_FRACTION_SEPARATOR = new char[]{'.'};
    public static final ParseConfig DEFAULT = new ParseConfig(DEFAULT_DATE_TIME_SEPARATORS, RFC_3339_FRACTION_SEPARATOR);
    public static final ParseConfig STRICT = new ParseConfig(new char[]{SEPARATOR_UPPER}, RFC_3339_FRACTION_SEPARATOR);

    private final char[] dateTimeSeparators;
    private final char[] fractionSeparators;

    protected ParseConfig(char[] dateTimeSeparators, char[] allowedFractionSeparators)
    {
        this.dateTimeSeparators = Optional.ofNullable(dateTimeSeparators).orElse(DEFAULT_DATE_TIME_SEPARATORS);
        this.fractionSeparators = Optional.ofNullable(allowedFractionSeparators).orElse(RFC_3339_FRACTION_SEPARATOR);
    }

    public char[] getFractionSeparators()
    {
        return fractionSeparators;
    }

    public ParseConfig withDateTimeSeparators(char... allowed)
    {
        assertChars(allowed);
        return new ParseConfig(allowed, fractionSeparators);
    }

    private void assertChars(char[] chars)
    {
        if (chars == null)
        {
            throw new IllegalArgumentException("Cannot have null array of characters");
        }
        if (chars.length == 0)
        {
            throw new IllegalArgumentException("Must have at least one character in allowed list");
        }
    }

    public ParseConfig withFractionSeparators(char... allowed)
    {
        assertChars(allowed);
        return new ParseConfig(dateTimeSeparators, allowed);
    }

    public ParseConfig withFailOnTrailingJunk(boolean failOnTrailingJunk)
    {
        return new ParseConfig(dateTimeSeparators, fractionSeparators);
    }

    public boolean isFailOnTrailingJunk()
    {
        return true;
    }

    public char[] getDateTimeSeparators()
    {
        return dateTimeSeparators;
    }

    public boolean isDateTimeSeparator(char needle)
    {
        for (char c : dateTimeSeparators)
        {
            if (c == needle)
            {
                return true;
            }
        }
        return false;
    }

    public boolean isFractionSeparator(char needle)
    {
        for (char c : fractionSeparators)
        {
            if (c == needle)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "ParseConfig{" +
                "dateTimeSeparators=" + Arrays.toString(dateTimeSeparators) +
                ", fractionSeparators=" + Arrays.toString(fractionSeparators) +
                '}';
    }
}
