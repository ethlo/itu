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

import static com.ethlo.time.internal.ErrorUtil.raiseUnexpectedCharacter;
import static com.ethlo.time.internal.LimitedCharArrayIntegerUtil.parsePositiveInt;

import java.text.ParsePosition;
import java.time.format.DateTimeParseException;

import com.ethlo.time.Field;

public class TimeZoneOffsetToken implements DateTimeToken
{
    @Override
    public int read(final String text, final ParsePosition parsePosition)
    {
        final int idx = parsePosition.getIndex();
        final int len = text.length();
        final int left = len - idx;

        if (left < 1)
        {
            return -1;
        }

        final char c = text.charAt(idx);
        if (c == 'Z' || c == 'z')
        {
            return 0;
        }

        final char sign = text.charAt(idx);
        if (sign != '+' && sign != '-')
        {
            raiseUnexpectedCharacter(text, idx);
        }

        if (left < 6)
        {
            throw new DateTimeParseException(String.format("Invalid timezone offset: %s", text), text, idx);
        }

        int hours = parsePositiveInt(text, idx + 1, idx + 3);
        int minutes = parsePositiveInt(text, idx + 4, idx + 4 + 2);
        if (sign == '-')
        {
            hours = -hours;
            minutes = -minutes;

            if (hours == 0 && minutes == 0)
            {
                throw new DateTimeParseException("Unknown 'Local Offset Convention' date-time not allowed", text, idx);
            }
        }

        return hours * 3600 + minutes * 60;
    }

    @Override
    public Field getField()
    {
        return Field.ZONE_OFFSET;
    }
}
