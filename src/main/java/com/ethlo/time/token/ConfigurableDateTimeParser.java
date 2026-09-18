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
import com.ethlo.time.internal.token.DigitsToken;
import com.ethlo.time.internal.token.FractionsToken;
import com.ethlo.time.internal.token.SeparatorToken;
import com.ethlo.time.internal.token.SeparatorsToken;
import com.ethlo.time.internal.token.ZoneOffsetToken;

/**
 * A configurable format `DateTimeParser`.
 */
public class ConfigurableDateTimeParser implements DateTimeParser
{
    // NOTE: Field.values() clones its backing array on every call, so it is cached here rather than being
    // called once per parse
    private static final Field[] FIELDS = Field.values();

    // Token kinds resolved at construction time. The built-in tokens are invoked through their concrete classes so
    // the JIT can inline them, instead of going through an interface call that has too many receiver types to
    // ever be inlined. Anything else falls back to the interface.
    private static final byte KIND_OTHER = 0;
    private static final byte KIND_DIGITS = 1;
    private static final byte KIND_SEPARATOR = 2;
    private static final byte KIND_SEPARATORS = 3;
    private static final byte KIND_FRACTIONS = 4;
    private static final byte KIND_ZONE_OFFSET = 5;

    private final DateTimeToken[] tokens;
    private final byte[] kinds;
    private final int[] ordinals;

    private ConfigurableDateTimeParser(DateTimeToken... tokens)
    {
        final Set<Field> fieldsSeen = new HashSet<>();
        Arrays.asList(tokens).forEach(t -> {
            if (t.getField() != null && !fieldsSeen.add(t.getField()))
            {
                throw new IllegalArgumentException("Duplicate field " + t.getField() + " in list of tokens: " + Arrays.toString(tokens));
            }
        });
        // Snapshot the caller-owned array so the cached classification below cannot diverge from it
        this.tokens = tokens.clone();
        this.kinds = new byte[this.tokens.length];
        this.ordinals = new int[this.tokens.length];
        for (int i = 0; i < this.tokens.length; i++)
        {
            this.kinds[i] = kindOf(this.tokens[i]);
            final Field field = this.tokens[i].getField();
            this.ordinals[i] = field != null ? field.ordinal() : -1;
        }
    }

    private static byte kindOf(final DateTimeToken token)
    {
        // Exact class matches only, so a subclass overriding read() is never bypassed
        final Class<?> type = token.getClass();
        if (type == DigitsToken.class)
        {
            return KIND_DIGITS;
        }
        if (type == SeparatorToken.class)
        {
            return KIND_SEPARATOR;
        }
        if (type == SeparatorsToken.class)
        {
            return KIND_SEPARATORS;
        }
        if (type == FractionsToken.class)
        {
            return KIND_FRACTIONS;
        }
        if (type == ZoneOffsetToken.class)
        {
            return KIND_ZONE_OFFSET;
        }
        return KIND_OTHER;
    }

    /**
     * Create a new parser with the specified tokens
     *
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

        // The position is tracked in a local and only synced with the ParsePosition for tokens that need it, so
        // the fixed-width built-ins do not pay for a memory round-trip per token
        int pos = parsePosition.getIndex();
        for (int i = 0; i < tokens.length; i++)
        {
            final DateTimeToken token = tokens[i];
            final byte kind = kinds[i];
            final int index = pos;
            final int value;
            switch (kind)
            {
                case KIND_DIGITS:
                    final DigitsToken digits = (DigitsToken) token;
                    value = digits.read(text, pos);
                    pos += digits.getLength();
                    break;
                case KIND_SEPARATOR:
                    ((SeparatorToken) token).read(text, pos);
                    pos++;
                    value = 1;
                    break;
                case KIND_SEPARATORS:
                    ((SeparatorsToken) token).read(text, pos);
                    pos++;
                    value = 1;
                    break;
                case KIND_FRACTIONS:
                    parsePosition.setIndex(pos);
                    value = ((FractionsToken) token).read(text, parsePosition);
                    pos = parsePosition.getIndex();
                    break;
                case KIND_ZONE_OFFSET:
                    parsePosition.setIndex(pos);
                    value = ((ZoneOffsetToken) token).read(text, parsePosition);
                    pos = parsePosition.getIndex();
                    break;
                default:
                    parsePosition.setIndex(pos);
                    value = token.read(text, parsePosition);
                    pos = parsePosition.getIndex();
            }

            final int ordinal = ordinals[i];
            if (ordinal != -1)
            {
                values[ordinal] = value;
                highestOrdinal = Math.max(ordinal, highestOrdinal);
                if (kind == KIND_FRACTIONS)
                {
                    fractionsLength = pos - index;
                    values[ordinal] = scale(value, fractionsLength);
                }
            }
        }
        parsePosition.setIndex(pos);

        return new DateTime(FIELDS[Math.min(highestOrdinal, NANO.ordinal())], values[Field.YEAR.ordinal()], values[Field.MONTH.ordinal()], values[Field.DAY.ordinal()], values[Field.HOUR.ordinal()], values[Field.MINUTE.ordinal()], values[Field.SECOND.ordinal()], values[Field.NANO.ordinal()], values[Field.ZONE_OFFSET.ordinal()] != -1 ? TimezoneOffset.ofTotalSeconds(values[Field.ZONE_OFFSET.ordinal()]) : null, fractionsLength);
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
