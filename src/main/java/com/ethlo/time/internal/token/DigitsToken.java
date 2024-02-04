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

import com.ethlo.time.Field;
import com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil;
import com.ethlo.time.token.DateTimeToken;

public class DigitsToken implements DateTimeToken
{
    private final Field field;
    private final int length;

    public DigitsToken(Field field, int length)
    {
        this.field = field;
        this.length = length;
    }

    @Override
    public int read(String text, ParsePosition parsePosition)
    {
        final int offset = parsePosition.getIndex();
        final int end = offset + length;
        final int value = LimitedCharArrayIntegerUtil.parsePositiveInt(text, offset, end);
        parsePosition.setIndex(end);
        return value;
    }

    public Field getField()
    {
        return field;
    }

    @Override
    public String toString()
    {
        return "digits: " + field + "(" + length + ")";
    }
}
