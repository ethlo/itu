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

import static com.ethlo.time.internal.EthloITU.FRACTION_SEPARATOR;
import static com.ethlo.time.internal.EthloITU.SEPARATOR_LOWER;
import static com.ethlo.time.internal.EthloITU.SEPARATOR_SPACE;
import static com.ethlo.time.internal.EthloITU.SEPARATOR_UPPER;

import java.util.Arrays;
import java.util.Optional;

public class ParseConfig
{
    public static final ParseConfig DEFAULT = new ParseConfig(null, null, true);

    private final char[] allowedDateTimeSeparators;
    private final char[] allowedFractionSeparators;
    private final boolean failOnTrailingJunk;

    protected ParseConfig(char[] allowedDateTimeSeparators, char[] allowedFractionSeparators, boolean failOnTrailingJunk)
    {
        this.allowedDateTimeSeparators = Optional.ofNullable(allowedDateTimeSeparators).orElse(new char[]{SEPARATOR_UPPER, SEPARATOR_LOWER, SEPARATOR_SPACE});
        this.allowedFractionSeparators = Optional.ofNullable(allowedFractionSeparators).orElse(new char[]{FRACTION_SEPARATOR});
        this.failOnTrailingJunk = failOnTrailingJunk;
    }

    public ParseConfig withDateTimeSeparators(char... allowed)
    {
        assertChars(allowed);
        return new ParseConfig(allowed, allowedFractionSeparators, failOnTrailingJunk);
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

    public ParseConfig withFractionSeparators(char... allowed)
    {
        assertChars(allowed);
        return new ParseConfig(allowedDateTimeSeparators, allowed, failOnTrailingJunk);
    }

    public ParseConfig failOnTrailingJunk(boolean failOnTrailingJunk)
    {
        return new ParseConfig(allowedDateTimeSeparators, allowedFractionSeparators, failOnTrailingJunk);
    }

    public boolean isFailOnTrailingJunk()
    {
        return failOnTrailingJunk;
    }

    public char[] getDateTimeSeparators()
    {
        return allowedDateTimeSeparators;
    }

    public boolean isDateTimeSeparator(char needle)
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

    public boolean isFractionSeparator(char needle)
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

    @Override
    public String toString()
    {
        return "ParseConfig{" +
                "dateTimeSeparators=" + Arrays.toString(allowedDateTimeSeparators) +
                ", fractionSeparators=" + Arrays.toString(allowedFractionSeparators) +
                ", failOnTrailingJunk=" + failOnTrailingJunk +
                '}';
    }
}
