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

import java.time.OffsetDateTime;

/**
 * A reusable, mutable target for {@link ITU#parseLenient(char[], int, int, MutableDateTimeBuffer)}. Parsing into a
 * buffer allocates nothing: the fields are plain {@code int}s and the timezone offset is kept as total seconds
 * rather than as a {@link TimezoneOffset} object.
 * <p>
 * Every field is written on every successful parse, so a buffer can be reused without clearing it; a failed parse
 * leaves the previous contents untouched. Like a {@link StringBuilder}, an instance is not thread-safe: use one per
 * thread or per parser.
 * <p>
 * The allocating bridges {@link #toDateTime()} and {@link #toOffsetDateTime()} are explicit; nothing in this class
 * allocates unless one of them is called.
 */
public final class MutableDateTimeBuffer
{
    /**
     * The value of {@link #getOffsetTotalSeconds()} when the parsed text carried no timezone offset
     */
    public static final int NO_OFFSET = Integer.MIN_VALUE;

    private Field field;
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
    private int nano;
    private int fractionDigits;
    private int offsetTotalSeconds = NO_OFFSET;
    private int parseLength;

    /**
     * Overwrites every field. Used by the parser after it has validated the values; performs no validation itself.
     *
     * @param field              The most granular field present
     * @param year               year
     * @param month              month, 0 when absent
     * @param day                day, 0 when absent
     * @param hour               hour, 0 when absent
     * @param minute             minute, 0 when absent
     * @param second             second, 0 when absent
     * @param nano               nanoseconds, 0 when absent
     * @param fractionDigits     the number of fraction digits present, 0 when absent
     * @param offsetTotalSeconds the timezone offset in seconds, or {@link #NO_OFFSET}
     * @param parseLength        the number of characters consumed
     */
    public void set(final Field field, final int year, final int month, final int day, final int hour, final int minute, final int second, final int nano, final int fractionDigits, final int offsetTotalSeconds, final int parseLength)
    {
        this.field = field;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.nano = nano;
        this.fractionDigits = fractionDigits;
        this.offsetTotalSeconds = offsetTotalSeconds;
        this.parseLength = parseLength;
    }

    /**
     * Returns the most granular field found during parsing, or null if nothing has been parsed into this buffer yet
     *
     * @return The field found
     */
    public Field getMostGranularField()
    {
        return field;
    }

    /**
     * Returns if the specified field is part of the parsed date/date-time
     *
     * @param field The field to check for
     * @return True if included, otherwise false
     */
    public boolean includesGranularity(final Field field)
    {
        return this.field != null && field.ordinal() <= this.field.ordinal();
    }

    public int getYear()
    {
        return year;
    }

    public int getMonth()
    {
        return month;
    }

    public int getDayOfMonth()
    {
        return day;
    }

    public int getHour()
    {
        return hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public int getSecond()
    {
        return second;
    }

    public int getNano()
    {
        return nano;
    }

    /**
     * Returns the number of fraction digits that were present in the text, 0 if none
     *
     * @return The number of fraction digits
     */
    public int getFractionDigits()
    {
        return fractionDigits;
    }

    /**
     * Returns whether the parsed text carried a timezone offset
     *
     * @return True if an offset was present, otherwise false
     */
    public boolean hasOffset()
    {
        return offsetTotalSeconds != NO_OFFSET;
    }

    /**
     * Returns the timezone offset as total seconds, or {@link #NO_OFFSET} if none was present
     *
     * @return The offset in seconds
     */
    public int getOffsetTotalSeconds()
    {
        return offsetTotalSeconds;
    }

    /**
     * Returns the number of characters consumed by the last successful parse
     *
     * @return The number of characters consumed
     */
    public int getParseLength()
    {
        return parseLength;
    }

    /**
     * Creates an immutable {@link DateTime} from the current contents. This allocates.
     *
     * @return A new {@link DateTime}
     */
    public DateTime toDateTime()
    {
        return new DateTime(field, year, month, day, hour, minute, second, nano, hasOffset() ? TimezoneOffset.ofTotalSeconds(offsetTotalSeconds) : null, fractionDigits, parseLength);
    }

    /**
     * Creates an {@link OffsetDateTime} from the current contents. This allocates.
     *
     * @return A new {@link OffsetDateTime}
     */
    public OffsetDateTime toOffsetDateTime()
    {
        return toDateTime().toOffsetDatetime();
    }

    @Override
    public String toString()
    {
        return field == null ? "<empty>" : toDateTime().toString();
    }
}
