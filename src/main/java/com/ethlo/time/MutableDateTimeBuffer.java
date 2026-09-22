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
import java.time.ZoneOffset;

import com.ethlo.time.internal.util.DateTimeMath;

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
 * allocates unless one of them is called. {@link #toEpochSecond()} and {@link #toEpochMilli()} give the point in
 * time as a primitive without either.
 */
public final class MutableDateTimeBuffer
{
    /**
     * The value of {@link #getOffsetTotalSeconds()} when the parsed text carried no timezone offset
     */
    public static final int NO_OFFSET = Integer.MIN_VALUE;

    /**
     * {@link Field#values()}, indexed by {@link #fieldOrdinal}
     */
    private static final Field[] FIELDS = Field.values();
    private static final int NO_FIELD = -1;

    /**
     * The ordinal of the most granular field, or {@link #NO_FIELD}. Kept as an int rather than a {@link Field} so that a
     * parse stores only primitives: a reference store costs a GC write barrier on every call (perf-log S4.7)
     */
    private int fieldOrdinal = NO_FIELD;
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
        this.fieldOrdinal = field.ordinal();
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
        return fieldOrdinal == NO_FIELD ? null : FIELDS[fieldOrdinal];
    }

    /**
     * Returns if the specified field is part of the parsed date/date-time
     *
     * @param field The field to check for
     * @return True if included, otherwise false
     */
    public boolean includesGranularity(final Field field)
    {
        return fieldOrdinal != NO_FIELD && field.ordinal() <= fieldOrdinal;
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
        return new DateTime(getMostGranularField(), year, month, day, hour, minute, second, nano, hasOffset() ? TimezoneOffset.ofTotalSeconds(offsetTotalSeconds) : null, fractionDigits, parseLength);
    }

    /**
     * The seconds since 1970-01-01T00:00:00Z, resolved the way {@link DateTime#toInstant()} resolves missing
     * fields: 1 for month and day, 0 for time fields, and UTC when the text carried no offset. Allocates nothing.
     *
     * @return The epoch second
     */
    public long toEpochSecond()
    {
        final long days = DateTimeMath.daysFromCivil(year, month != 0 ? month : 1, day != 0 ? day : 1);
        final long offset = offsetTotalSeconds != NO_OFFSET ? offsetTotalSeconds : 0;
        return days * 86_400 + hour * 3_600L + minute * 60L + second - offset;
    }

    /**
     * The milliseconds since 1970-01-01T00:00:00Z, with the fraction truncated, resolved as by
     * {@link #toEpochSecond()}. Allocates nothing.
     *
     * @return The epoch millisecond
     */
    public long toEpochMilli()
    {
        // nano is never negative, so this is the floor for dates before 1970 too, as Instant.toEpochMilli() gives
        return toEpochSecond() * 1_000 + nano / 1_000_000;
    }

    /**
     * Creates an {@link OffsetDateTime} from the current contents. This allocates.
     *
     * @return A new {@link OffsetDateTime}
     */
    public OffsetDateTime toOffsetDateTime()
    {
        if (includesGranularity(Field.MINUTE) && hasOffset())
        {
            // NOTE: Built directly rather than via toDateTime() so that the intermediate DateTime and TimezoneOffset
            // are never allocated. java.time performs its own range checks, so a buffer filled through set(..) with
            // out-of-range values still fails, just with java.time's message
            return OffsetDateTime.of(year, month, day, hour, minute, second, nano, ZoneOffset.ofTotalSeconds(offsetTotalSeconds));
        }
        // Missing granularity or offset: let DateTime produce the exception it always has
        return toDateTime().toOffsetDatetime();
    }

    @Override
    public String toString()
    {
        return fieldOrdinal == NO_FIELD ? "<empty>" : toDateTime().toString();
    }
}
