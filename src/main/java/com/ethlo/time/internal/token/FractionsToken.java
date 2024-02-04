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

import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.DIGIT_9;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.ZERO;

import java.text.ParsePosition;

import com.ethlo.time.Field;
import com.ethlo.time.token.DateTimeToken;

public class FractionsToken implements DateTimeToken
{
    @Override
    public int read(final String text, final ParsePosition parsePosition)
    {
        int idx = parsePosition.getIndex();
        final int length = text.length();
        int value = 0;
        while (idx < length)
        {
            final char c = text.charAt(idx);
            if (c < ZERO || c > DIGIT_9)
            {
                break;
            }
            else
            {
                value = value * 10 + (c - ZERO);
                idx++;
            }
        }
        parsePosition.setIndex(idx);
        return value;
    }

    @Override
    public Field getField()
    {
        return Field.NANO;
    }
}
