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

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import com.ethlo.time.internal.ITUFormatter;
import com.ethlo.time.internal.ITUParser;

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

    public static DateTime parseLenient(String text, ParseConfig parseConfig)
    {
        return ITUParser.parseLenient(text, parseConfig, 0);
    }

    public static DateTime parseLenient(String text, ParseConfig parseConfig, ParsePosition position)
    {
        try
        {
            int offset = position.getIndex();
            final DateTime result = ITUParser.parseLenient(text, parseConfig, position.getIndex());
            position.setIndex(offset + result.getParseLength());
            return result;
        }
        catch (DateTimeParseException exc)
        {
            position.setErrorIndex(exc.getErrorIndex());
            position.setIndex(position.getErrorIndex());
            throw exc;
        }
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
