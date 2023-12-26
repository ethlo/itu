package com.ethlo.time.jdk;

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

import com.ethlo.time.internal.AbstractRfc3339;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Java 8 JDK classes. The safe and normally "efficient enough" choice.
 *
 * @author ethlo - Morten Haraldsen
 */
public class JdkRfc3339 extends AbstractRfc3339
{
    private final DateTimeFormatter rfc3339baseFormatter = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.CLOCK_HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter();

    private final DateTimeFormatter rfc3339formatParser = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .optionalStart()
            .appendLiteral('T')
            .optionalEnd()
            .optionalStart()
            .appendLiteral('t')
            .optionalEnd()
            .optionalStart()
            .appendLiteral(' ')
            .optionalEnd()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendLiteral('.')
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false)
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HH:MM", "Z")
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HH:MM", "z")
            .optionalEnd()
            .toFormatter();

    private DateTimeFormatter getFormatter(int fractionDigits)
    {
        if (fractionDigits == 0)
        {
            return new DateTimeFormatterBuilder()
                    .append(rfc3339baseFormatter)
                    .appendOffset("+HH:MM", "Z")
                    .toFormatter()
                    .withZone(ZoneOffset.UTC);
        }

        return new DateTimeFormatterBuilder()
                .append(rfc3339baseFormatter)
                .optionalStart()
                .appendLiteral('.')
                .appendFraction(ChronoField.NANO_OF_SECOND, fractionDigits, fractionDigits, false)
                .optionalEnd()
                .appendOffset("+HH:MM", "Z")
                .toFormatter()
                .withZone(ZoneOffset.UTC);
    }

    @Override
    public OffsetDateTime parseDateTime(final String s)
    {
        return OffsetDateTime.from(rfc3339formatParser.parse(s));
    }

    @Override
    public String formatUtc(OffsetDateTime date)
    {
        return formatUtc(date, 0);
    }

    @Override
    public String formatUtcMilli(OffsetDateTime date)
    {
        return formatUtc(date, 3);
    }

    @Override
    public String formatUtcMicro(OffsetDateTime date)
    {
        return formatUtc(date, 6);
    }

    @Override
    public String formatUtcNano(OffsetDateTime date)
    {
        return formatUtc(date, 9);
    }

    @Override
    public String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        assertMaxFractionDigits(fractionDigits);
        return getFormatter(fractionDigits).format(date);
    }
}
