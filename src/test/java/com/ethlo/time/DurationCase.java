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

/**
 * One entry of {@code duration-corpus.json}, driven by {@link DurationCorpusTest}. Fields (snake_case in the file):
 * <ul>
 * <li>{@code input} - the text to parse, and {@code offset} - where in it to start (default 0)</li>
 * <li>{@code seconds} and {@code nanos} - the parsed value</li>
 * <li>{@code normalized} - the canonical rendering ({@link Duration#normalized()}) of the parsed value</li>
 * <li>{@code error} - the exact {@code DateTimeParseException} message, and {@code error_index} its index</li>
 * <li>{@code note} - why the case exists</li>
 * </ul>
 */
public class DurationCase
{
    private final String input;
    private final Integer offset;
    private final Long seconds;
    private final Integer nanos;
    private final String normalized;
    private final String error;
    private final Integer errorIndex;
    private final String note;

    @ConstructorProperties({"input", "offset", "seconds", "nanos", "normalized", "error", "error_index", "note"})
    public DurationCase(final String input, final Integer offset, final Long seconds, final Integer nanos, final String normalized, final String error, final Integer errorIndex, final String note)
    {
        this.input = input;
        this.offset = offset;
        this.seconds = seconds;
        this.nanos = nanos;
        this.normalized = normalized;
        this.error = error;
        this.errorIndex = errorIndex;
        this.note = note;
    }

    public String getInput()
    {
        return input;
    }

    public Integer getOffset()
    {
        return offset;
    }

    public Long getSeconds()
    {
        return seconds;
    }

    public Integer getNanos()
    {
        return nanos;
    }

    public String getNormalized()
    {
        return normalized;
    }

    public String getError()
    {
        return error;
    }

    public Integer getErrorIndex()
    {
        return errorIndex;
    }

    public String getNote()
    {
        return note;
    }

    @Override
    public String toString()
    {
        return "'" + input + "'"
                + (offset != null ? " @" + offset : "")
                + (note != null ? " - " + note : "");
    }
}
