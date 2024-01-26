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

import java.time.DateTimeException;
import java.time.format.DateTimeParseException;

import com.ethlo.time.Field;

public class ErrorUtil
{
    private ErrorUtil()
    {
    }

    public static DateTimeParseException raiseUnexpectedCharacter(String chars, int index)
    {
        throw new DateTimeParseException("Unexpected character " + chars.charAt(index) + " at position " + (index + 1) + ": " + chars, chars, index);
    }

    public static DateTimeParseException raiseUnexpectedEndOfText(final String chars, final int offset)
    {
        throw new DateTimeParseException("Unexpected end of input: " + chars, chars, offset);
    }

    public static DateTimeParseException raiseMissingTimeZone(String chars, int index)
    {
        throw new DateTimeParseException("No timezone information: " + chars, chars, index);
    }

    public static DateTimeException raiseMissingGranularity(Field field, final String chars, final int offset)
    {
        throw new DateTimeParseException("Unexpected end of input, missing field " + field.name() + ": " + chars, chars, offset);
    }
}
