package com.ethlo.time.internal;

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

import static com.ethlo.time.internal.ITUParser.MAX_FRACTION_DIGITS;

import java.time.format.DateTimeParseException;

import com.ethlo.time.Field;

public class ErrorUtil
{
    private ErrorUtil()
    {
    }

    public static DateTimeParseException raiseUnexpectedCharacter(String chars, int index)
    {
        throw new DateTimeParseException(String.format("Unexpected character %s at position %d: %s", chars.charAt(index), index + 1, chars), chars, index);
    }

    public static DateTimeParseException raiseUnexpectedEndOfText(final String chars, final int offset)
    {
        throw new DateTimeParseException(String.format("Unexpected end of input: %s", chars), chars, offset);
    }

    public static DateTimeParseException raiseMissingGranularity(Field field, final String chars, final int offset)
    {
        throw new DateTimeParseException(String.format("Unexpected end of input, missing field %s: %s", field.name(), chars), chars, offset);
    }

    public static void assertPositionContains(Field field, String chars, int index, char expected)
    {
        if (index >= chars.length())
        {
            throw raiseMissingGranularity(field, chars, index);
        }

        if (chars.charAt(index) != expected)
        {
            throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", expected, index + 1, chars.charAt(index), chars), chars, index);
        }
    }

    public static void assertFractionDigits(String chars, int fractionDigits, int idx)
    {
        if (fractionDigits == 0)
        {
            throw new DateTimeParseException(String.format("Must have at least 1 fraction digit: %s", chars), chars, idx);
        }

        if (fractionDigits > MAX_FRACTION_DIGITS)
        {
            throw new DateTimeParseException(String.format("Maximum supported number of fraction digits in second is 9, got %d: %s", fractionDigits, chars), chars, idx);
        }
    }
}
