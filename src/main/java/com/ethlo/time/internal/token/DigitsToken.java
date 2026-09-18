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
        final int value = read(text, offset);
        parsePosition.setIndex(offset + length);
        return value;
    }

    /**
     * Reads the digits at the given index. The token always consumes exactly {@link #getLength()} characters,
     * so the caller can advance the position without a round-trip through a {@link ParsePosition}.
     */
    public int read(final String text, final int offset)
    {
        switch (length)
        {
            // The common widths (year and every two-digit field) take the unrolled paths used by the fixed-format parser
            case 2:
                return LimitedCharArrayIntegerUtil.parse2(text, offset);
            case 4:
                return LimitedCharArrayIntegerUtil.parse4(text, offset);
            default:
                return LimitedCharArrayIntegerUtil.parsePositiveInt(text, offset, offset + length);
        }
    }

    public int getLength()
    {
        return length;
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
