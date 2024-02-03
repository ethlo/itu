package com.ethlo.time.token;

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

import java.text.ParsePosition;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.ethlo.time.Field;
import com.ethlo.time.internal.ErrorUtil;

public class SeparatorsToken implements DateTimeToken
{
    private final char[] separators;

    public SeparatorsToken(char... separators)
    {
        this.separators = separators;
    }

    @Override
    public int read(final String text, final ParsePosition parsePosition)
    {
        final int index = parsePosition.getIndex();
        if (text.length() <= index)
        {
            ErrorUtil.raiseUnexpectedEndOfText(text, text.length());
        }

        final char c = text.charAt(index);
        for (char sep : separators)
        {
            if (c == sep)
            {
                parsePosition.setIndex(index + 1);
                return 1;
            }
        }
        throw new DateTimeParseException(String.format("Expected character %s at position %d, found %s: %s", Arrays.toString(separators), index + 1, text.charAt(index), text), text, index);
    }

    @Override
    public Field getField()
    {
        return null;
    }
}
