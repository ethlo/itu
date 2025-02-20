package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 Morten Haraldsen (ethlo)
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

import com.ethlo.time.internal.ItuDurationParser;
import com.ethlo.time.internal.fixed.ITUFormatter;
import com.ethlo.time.internal.fixed.ITUParser;

import java.text.ParsePosition;
import java.time.*;

/**
 * The main access to the parse and formatting functions in this library.
 */
public class ITU
{
    private ITU()
    {
    }

    /**
     * Parse an RFC-3339 formatted date-time to an {@link OffsetDateTime}
     *
     * @param text The text to parse
     * @return The date and time parsed
     */
    public static OffsetDateTime parseDateTime(String text)
    {
        return ITUParser.parseDateTime(text, 0);
    }

    /**
     * Parses a duration string, a strict subset of ISO 8601 durations.
     * <p>
     * This method supports time-based durations with the following units:
     * <ul>
     *     <li>Weeks (`W`)</li>
     *     <li>Days (`D`)</li>
     *     <li>Hours (`H`)</li>
     *     <li>Minutes (`M`)</li>
     *     <li>Seconds (`S`), including fractional seconds up to nanosecond precision</li>
     * </ul>
     * The following units are explicitly <b>not allowed</b> to avoid ambiguity:
     * <ul>
     *     <li>Years (`Y`)</li>
     *     <li>Months (`M` in the date section)</li>
     * </ul>
     * <p>
     * Negative durations are supported and must be prefixed with `-P`, as specified in ISO 8601.
     * The parsed duration will be represented using a {@code long} for total seconds
     * and an {@code int} for nanosecond precision. The nanosecond component is always positive,
     * with the sign absorbed by the seconds field, following Java and ISO 8601 conventions.
     * </p>
     *
     * <b>Examples of Valid Input</b>
     * <ul>
     *     <li>{@code P2DT3H4M5.678901234S} → 2 days, 3 hours, 4 minutes, 5.678901234 seconds</li>
     *     <li>{@code PT5M30S} → 5 minutes, 30 seconds</li>
     *     <li>{@code -PT2.5S} → Negative 2.5 seconds</li>
     *     <li>{@code -P1D} → Negative 1 day</li>
     * </ul>
     *
     * <b>Examples of Invalid Input</b>
     * <ul>
     *     <li>{@code P1Y2M3DT4H} → Contains `Y` and `M`</li>
     *     <li>{@code PT} → Missing time values after `T`</li>
     *     <li>{@code P-1D} → Incorrect negative placement</li>
     * </ul>
     * <p>
     *
     * @param text the duration string to parse
     * @return a {@link Duration} instance representing the parsed duration
     * @throws java.time.format.DateTimeParseException if the input does not conform to the expected format
     */
    public static Duration parseDuration(String text)
    {
        return ItuDurationParser.parse(text, 0);
    }

    /**
     * Parses a duration string starting at the specified offset. See {@link #parseDuration(String)} for more information.
     *
     * @param text   The text to parse
     * @param offset the offset in the text to start at
     * @return a {@link Duration} instance representing the parsed duration
     * @throws java.time.format.DateTimeParseException if the input does not conform to the expected format
     */
    public static Duration parseDuration(String text, int offset)
    {
        return ItuDurationParser.parse(text, offset);
    }

    public static OffsetDateTime parseDateTime(String text, ParsePosition position)
    {
        return parseLenient(text, ParseConfig.DEFAULT, position).toOffsetDatetime();
    }

    /**
     * Parse an ISO formatted date and optionally time to a {@link DateTime}. The result has
     * rudimentary checks for correctness, but will not be aware of number of days per specific month or leap-years.
     *
     * @param text The text to parse
     * @return The date and time parsed
     */
    public static DateTime parseLenient(String text)
    {
        return ITUParser.parseLenient(text, ParseConfig.DEFAULT, 0);
    }

    /**
     * Allows parsing leniently with {@link ParseConfig to control some aspects of the parsing}
     *
     * @param text        The text to parse
     * @param parseConfig The configuration to use for parsing
     * @return The date-time parsed
     */
    public static DateTime parseLenient(String text, ParseConfig parseConfig)
    {
        return ITUParser.parseLenient(text, parseConfig, 0);
    }

    /**
     * @param text        The text to parse
     * @param parseConfig The configuration to use for parsing
     * @param position    The position to start parsing from. The index (and the errorIndex, if an error occurs) is updated after the parsing process has completed
     * @return The date-time parsed
     */
    public static DateTime parseLenient(String text, ParseConfig parseConfig, ParsePosition position)
    {
        return ITUParser.parseLenient(text, parseConfig, position);
    }

    /**
     * Check if the dateTime is valid according to the RFC-3339 specification
     *
     * @param text The input to validate
     * @return True if valid, otherwise false
     */
    public static boolean isValid(String text)
    {
        try
        {
            parseDateTime(text);
            return true;
        }
        catch (DateTimeException exc)
        {
            return false;
        }
    }

    /**
     * Format the input as an RFC-3339 formatted date-time in the UTC timezone
     *
     * @param offsetDateTime The date-time to format
     * @param fractionDigits The number of fraction digits in the second field
     * @return The formatted string
     */
    public static String formatUtc(OffsetDateTime offsetDateTime, int fractionDigits)
    {
        return ITUFormatter.formatUtc(offsetDateTime, fractionDigits);
    }

    /**
     * Format the input as an ISO format string, limited to the granularity of the specified field, in the UTC timezone.
     *
     * @param offsetDateTime The date-time to format
     * @param lastIncluded   The last included field
     * @return The formatted string
     */
    public static String formatUtc(OffsetDateTime offsetDateTime, Field lastIncluded)
    {
        return ITUFormatter.formatUtc(offsetDateTime, lastIncluded);
    }

    /**
     * Format the input as an RFC-3339 formatted date-time in the timezone of the input.
     *
     * @param offsetDateTime The date-time to format
     * @return The formatted string
     */
    public static String format(OffsetDateTime offsetDateTime)
    {
        return ITUFormatter.format(offsetDateTime, offsetDateTime.getOffset(), 0);
    }

    /**
     * Format the input as an RFC-3339 formatted date-time in the timezone of the input, with the specified number of fraction digits.
     *
     * @param offsetDateTime The date-time to format
     * @param fractionDigits The number of fraction digits in the second field
     * @return The formatted string
     */
    public static String format(OffsetDateTime offsetDateTime, int fractionDigits)
    {
        return ITUFormatter.format(offsetDateTime, offsetDateTime.getOffset(), fractionDigits);
    }

    /**
     * Format the input as an RFC-3339 formatted date-time in the UTC timezone with second resolution.
     *
     * @param offsetDateTime The date-time to format.
     * @return The formatted string with second resolution.
     */
    public static String formatUtc(OffsetDateTime offsetDateTime)
    {
        return ITUFormatter.formatUtc(offsetDateTime, 0);
    }

    /**
     * Format the input as an RFC-3339 formatted date-time in the UTC timezone with millisecond resolution.
     *
     * @param offsetDateTime The date-time to format.
     * @return The formatted string with millisecond resolution.
     */
    public static String formatUtcMilli(final OffsetDateTime offsetDateTime)
    {
        return ITUFormatter.formatUtc(offsetDateTime, 3);
    }

    /**
     * Format the input as an RFC-3339 formatted date-time in the UTC timezone with microsecond resolution.
     *
     * @param offsetDateTime The date-time to format
     * @return The formatted string with microsecond resolution
     */
    public static String formatUtcMicro(final OffsetDateTime offsetDateTime)
    {
        return ITUFormatter.formatUtc(offsetDateTime, 6);
    }

    /**
     * Format the input as an RFC-3339 formatted date-time in the UTC timezone with nanosecond resolution
     *
     * @param offsetDateTime The date-time to format
     * @return The formatted string with nanosecond resolution
     */
    public static String formatUtcNano(final OffsetDateTime offsetDateTime)
    {
        return ITUFormatter.formatUtc(offsetDateTime, 9);
    }

    /**
     * Parse the input, and use callbacks for the type of date/date-time it contains. This allows you to handle different granularity inputs with ease!
     *
     * @param text             The text to parse as a date/date-time
     * @param temporalConsumer The consumer of the found date/date-time
     */
    public static void parse(final String text, final TemporalConsumer temporalConsumer)
    {
        final DateTime dateTime = ITUParser.parseLenient(text, ParseConfig.DEFAULT, 0);
        if (dateTime.includesGranularity(Field.MINUTE))
        {
            if (dateTime.getOffset().isPresent())
            {
                temporalConsumer.handle(dateTime.toOffsetDatetime());
            }
            else
            {
                temporalConsumer.handle(dateTime.toLocalDatetime());
            }
        }
        else if (dateTime.includesGranularity(Field.DAY))
        {
            temporalConsumer.handle(dateTime.toLocalDate());
        }
        else if (dateTime.includesGranularity(Field.MONTH))
        {
            temporalConsumer.handle(dateTime.toYearMonth());
        }
        else
        {
            temporalConsumer.handle(Year.of(dateTime.getYear()));
        }
    }

    /**
     * Parse the input, and use callbacks for the type of date/date-time it contains. This allows you to handle different granularity inputs with ease!
     *
     * @param text            The text to parse as a date/date-time
     * @param temporalHandler The handler of the found date/date-time
     * @param <T>             The type of Temporal returned
     * @return The temporal matching the type handled
     */
    public static <T> T parse(String text, TemporalHandler<T> temporalHandler)
    {
        final DateTime dateTime = ITUParser.parseLenient(text, ParseConfig.DEFAULT, 0);
        if (dateTime.includesGranularity(Field.MINUTE))
        {
            if (dateTime.getOffset().isPresent())
            {
                return temporalHandler.handle(dateTime.toOffsetDatetime());
            }
            else
            {
                return temporalHandler.handle(dateTime.toLocalDatetime());
            }
        }
        else if (dateTime.includesGranularity(Field.DAY))
        {
            return temporalHandler.handle(dateTime.toLocalDate());
        }
        else if (dateTime.includesGranularity(Field.MONTH))
        {
            return temporalHandler.handle(dateTime.toYearMonth());
        }
        else
        {
            return temporalHandler.handle(Year.of(dateTime.getYear()));
        }
    }

    /**
     * Check if the input is valid for one of the specified types
     *
     * @param text  The input to check
     * @param types The types that are considered valid
     * @return True if valid, otherwise false
     */
    public static boolean isValid(final String text, TemporalType... types)
    {
        try
        {
            return ITU.parse(text, new TemporalHandler<Boolean>()
            {
                @Override
                public Boolean handle(final LocalDate localDate)
                {
                    return isAllowed(TemporalType.LOCAL_DATE, types);
                }

                @Override
                public Boolean handle(final OffsetDateTime offsetDateTime)
                {
                    return isAllowed(TemporalType.OFFSET_DATE_TIME, types);
                }

                @Override
                public Boolean handle(final LocalDateTime localDateTime)
                {
                    return isAllowed(TemporalType.LOCAL_DATE_TIME, types);
                }

                @Override
                public Boolean handle(final YearMonth yearMonth)
                {
                    return isAllowed(TemporalType.YEAR_MONTH, types);
                }

                @Override
                public Boolean handle(final Year year)
                {
                    return isAllowed(TemporalType.YEAR, types);
                }
            });
        }
        catch (DateTimeException exc)
        {
            return false;
        }
    }

    private static boolean isAllowed(TemporalType needle, TemporalType... allowed)
    {
        for (TemporalType t : allowed)
        {
            if (t.equals(needle))
            {
                return true;
            }
        }
        return false;
    }
}
