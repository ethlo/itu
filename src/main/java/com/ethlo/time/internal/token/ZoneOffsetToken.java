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

import static com.ethlo.time.internal.fixed.ITUParser.MINUS;
import static com.ethlo.time.internal.fixed.ITUParser.PLUS;
import static com.ethlo.time.internal.fixed.ITUParser.ZULU_LOWER;
import static com.ethlo.time.internal.fixed.ITUParser.ZULU_UPPER;
import static com.ethlo.time.internal.util.ErrorUtil.raiseUnexpectedCharacter;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.parsePositiveInt;

import java.text.ParsePosition;
import java.time.format.DateTimeParseException;

import com.ethlo.time.Field;
import com.ethlo.time.token.DateTimeToken;

public class ZoneOffsetToken implements DateTimeToken
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
            parsePosition.setIndex(idx + 1);
            return 0;
        }

        final char sign = text.charAt(idx);
        if (sign != '+' && sign != '-')
        {
            raiseUnexpectedCharacter(text, idx, ZULU_UPPER, ZULU_LOWER, PLUS, MINUS);
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

        parsePosition.setIndex(idx + 6);
        return hours * 3600 + minutes * 60;
    }

    @Override
    public Field getField()
    {
        return Field.ZONE_OFFSET;
    }
}
