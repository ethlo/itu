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

import static com.ethlo.time.Field.NANO;
import static com.ethlo.time.Field.YEAR;

import java.text.ParsePosition;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ethlo.time.DateTime;
import com.ethlo.time.DateTimeParser;
import com.ethlo.time.Field;
import com.ethlo.time.TimezoneOffset;
import com.ethlo.time.internal.token.FractionsToken;

/**
 * A configurable format `DateTimeParser`.
 */
public class ConfigurableDateTimeParser implements DateTimeParser
{
    private final DateTimeToken[] tokens;

    private ConfigurableDateTimeParser(DateTimeToken... tokens)
    {
        final Set<Field> fieldsSeen = new HashSet<>();
        Arrays.asList(tokens).forEach(t -> {
            if (t.getField() != null && !fieldsSeen.add(t.getField()))
            {
                throw new IllegalArgumentException("Duplicate field " + t.getField() + " in list of tokens: " + Arrays.toString(tokens));
            }
        });
        this.tokens = tokens;
    }

    /**
     * Create a new parser with the specified tokens
     * @param tokens The tokens expected in the format
     * @return A new parser instance
     */
    public static DateTimeParser of(DateTimeToken... tokens)
    {
        return new ConfigurableDateTimeParser(tokens);
    }

    @Override
    public DateTime parse(String text, ParsePosition parsePosition)
    {
        try
        {
            return doParse(text, parsePosition);
        }
        catch (DateTimeParseException exc)
        {
            parsePosition.setIndex(exc.getErrorIndex());
            parsePosition.setErrorIndex(exc.getErrorIndex());
            throw exc;
        }
    }

    private DateTime doParse(String text, ParsePosition parsePosition)
    {
        int fractionsLength = 0;
        int highestOrdinal = YEAR.ordinal();
        final int[] values = new int[]{0, 1, 1, 0, 0, 0, 0, -1};

        for (DateTimeToken token : tokens)
        {
            final int index = parsePosition.getIndex();
            final int value = token.read(text, parsePosition);
            final Field field = token.getField();
            if (field != null)
            {
                final int ordinal = field.ordinal();
                values[ordinal] = value;
                highestOrdinal = Math.max(ordinal, highestOrdinal);
                if (token instanceof FractionsToken)
                {
                    fractionsLength = parsePosition.getIndex() - index;
                    values[ordinal] = scale(value, fractionsLength);
                }
            }
        }

        return new DateTime(Field.values()[Math.min(highestOrdinal, NANO.ordinal())], values[Field.YEAR.ordinal()], values[Field.MONTH.ordinal()], values[Field.DAY.ordinal()], values[Field.HOUR.ordinal()], values[Field.MINUTE.ordinal()], values[Field.SECOND.ordinal()], values[Field.NANO.ordinal()], values[Field.ZONE_OFFSET.ordinal()] != -1 ? TimezoneOffset.ofTotalSeconds(values[Field.ZONE_OFFSET.ordinal()]) : null, fractionsLength);
    }

    private int scale(int value, int length)
    {
        int pos = length;
        while (pos < 9)
        {
            value *= 10;
            pos++;
        }
        return value;
    }
}
