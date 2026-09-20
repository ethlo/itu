package com.ethlo.time;

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

import java.beans.ConstructorProperties;
import java.time.Instant;

/**
 * One entry of {@code date-time-corpus.json}: an input, how to parse it, and what must come out. The same entry is
 * driven through every implementation of the grammar by {@link DateTimeCorpusTest}, so a case recorded once is
 * checked on the String path, the ParsePosition path and the char[] path.
 * <p>
 * Fields (snake_case in the file):
 * <ul>
 * <li>{@code input} - the text to parse</li>
 * <li>{@code note} - why the case exists; the place for the regression story that used to live in a test name</li>
 * <li>{@code lenient} - {@code true} for {@code ITU.parseLenient(..)}, otherwise {@code ITU.parseDateTime(..)}</li>
 * <li>{@code offset} - when present the ParsePosition overloads are used, starting at this index</li>
 * <li>{@code config} - {@code allowed_date_separators}, {@code allowed_fraction_separators} (strings of characters)
 * and {@code fail_on_trailing_junk}; each defaults to {@link ParseConfig#DEFAULT}</li>
 * <li>{@code expected} - the canonical rendering ({@link DateTime#toString()}) of the parsed value, which pins every
 * field, the granularity, the number of fraction digits and the offset form</li>
 * <li>{@code instant} - {@code "epochSecond,nano"}; checked when present, and derived from {@code Instant.parse(input)}
 * when neither it nor {@code expected} is given</li>
 * <li>{@code parse_length} - characters consumed; always checked against the ParsePosition when {@code offset} is set</li>
 * <li>{@code error} - the exact exception message; {@code error_index} additionally requires a
 * {@code DateTimeParseException} with that index</li>
 * <li>{@code leap_second} - {@code nearest} (UTC text) and {@code verified} of the expected {@link LeapSecondException}</li>
 * </ul>
 */
public class DateTimeCase
{
    private final String input;
    private final boolean lenient;
    private final Integer offset;
    private final ParseConfig config;
    private final String expected;
    private final Instant instant;
    private final Integer parseLength;
    private final String error;
    private final int errorIndex;
    private final LeapSecond leapSecond;
    private final String note;

    @ConstructorProperties({"input", "lenient", "offset", "config", "expected", "instant", "parse_length", "error", "error_index", "leap_second", "note"})
    public DateTimeCase(final String input, final boolean lenient, final Integer offset, final SerializableParseConfig config, final String expected, final String instant, final Integer parseLength, final String error, final Integer errorIndex, final LeapSecond leapSecond, final String note)
    {
        this.input = input;
        this.lenient = lenient;
        this.offset = offset;
        this.config = config != null ? config : ParseConfig.DEFAULT;
        this.expected = expected;
        this.instant = parseInstant(instant);
        this.parseLength = parseLength;
        this.error = error;
        this.errorIndex = errorIndex != null ? errorIndex : -1;
        this.leapSecond = leapSecond;
        this.note = note;
    }

    private static Instant parseInstant(final String instant)
    {
        if (instant == null)
        {
            return null;
        }
        final String[] parts = instant.split(",");
        if (parts.length != 2)
        {
            throw new IllegalArgumentException("instant must be 'epochSecond,nano': " + instant);
        }
        return Instant.ofEpochSecond(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
    }

    public String getInput()
    {
        return input;
    }

    public boolean isLenient()
    {
        return lenient;
    }

    public Integer getOffset()
    {
        return offset;
    }

    public ParseConfig getConfig()
    {
        return config;
    }

    public String getExpected()
    {
        return expected;
    }

    public Instant getInstant()
    {
        return instant;
    }

    public Integer getParseLength()
    {
        return parseLength;
    }

    public String getError()
    {
        return error;
    }

    public int getErrorIndex()
    {
        return errorIndex;
    }

    public LeapSecond getLeapSecond()
    {
        return leapSecond;
    }

    public String getNote()
    {
        return note;
    }

    @Override
    public String toString()
    {
        return "'" + input + "'"
                + (lenient ? " lenient" : " strict")
                + (offset != null ? " @" + offset : "")
                + (config != ParseConfig.DEFAULT ? " " + config : "")
                + (note != null ? " - " + note : "");
    }

    public static class SerializableParseConfig extends ParseConfig
    {
        @ConstructorProperties({"allowed_date_separators", "allowed_fraction_separators", "fail_on_trailing_junk"})
        protected SerializableParseConfig(final char[] allowedDateTimeSeparators, final char[] allowedFractionSeparators, final Boolean failOnTrailingJunk)
        {
            super(allowedDateTimeSeparators, allowedFractionSeparators, failOnTrailingJunk == null || failOnTrailingJunk);
        }
    }

    public static class LeapSecond
    {
        private final String nearest;
        private final boolean verified;

        @ConstructorProperties({"nearest", "verified"})
        public LeapSecond(final String nearest, final boolean verified)
        {
            this.nearest = nearest;
            this.verified = verified;
        }

        public String getNearest()
        {
            return nearest;
        }

        public boolean isVerified()
        {
            return verified;
        }
    }
}
