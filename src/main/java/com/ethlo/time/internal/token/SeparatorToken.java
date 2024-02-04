package com.ethlo.time.internal.token;

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

import com.ethlo.time.internal.util.ErrorUtil;
import com.ethlo.time.token.DateTimeToken;

public class SeparatorToken implements DateTimeToken
{
    private final char separator;

    public SeparatorToken(char separator)
    {
        this.separator = separator;
    }

    @Override
    public int read(final String text, final ParsePosition parsePosition)
    {
        final int index = parsePosition.getIndex();
        if (text.length() > index && text.charAt(index) == separator)
        {
            parsePosition.setIndex(index + 1);
        }
        else if (text.length() <= index)
        {
            ErrorUtil.raiseUnexpectedEndOfText(text, text.length());
        }
        else if (text.charAt(index) != separator)
        {
            ErrorUtil.raiseUnexpectedCharacter(text, index, separator);
        }
        parsePosition.setIndex(index + 1);
        return 1;
    }

    @Override
    public String toString()
    {
        return "separator: " + separator;
    }
}
