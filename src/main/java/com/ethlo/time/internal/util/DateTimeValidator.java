package com.ethlo.time.internal.util;

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

import static com.ethlo.time.internal.util.LeapSecondHandler.LEAP_SECOND_SECONDS;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

import com.ethlo.time.Field;
import com.ethlo.time.LeapSecondException;

/**
 * The field-range and leap-second checks shared by {@link com.ethlo.time.DateTime} and the zero-allocation
 * buffer parser, so that both produce identical exceptions for identical values.
 */
public final class DateTimeValidator
{
    private static final LeapSecondHandler leapSecondHandler = new DefaultLeapSecondHandler();

    private DateTimeValidator()
    {
    }

    /**
     * Validates the parsed fields. Cheap arithmetic fast path; only when a field is out of range do we defer to
     * java.time, so that the error messages stay identical to what {@code OffsetDateTime.of(..)} would have produced.
     */
    public static void validate(final Field field, final int year, final int month, final int day, final int hour, final int minute, final int second, final int nano)
    {
        if (field.ordinal() >= Field.DAY.ordinal() && !isValidDate(year, month, day))
        {
            //noinspection ResultOfMethodCallIgnored
            LocalDate.of(year, month, day);
        }

        // NOTE: Validated from the most significant field down, and delegated to ChronoField so the messages
        // match what java.time would have produced had the value made it as far as OffsetDateTime.of(..)
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59 || nano < 0 || nano > 999_999_999)
        {
            ChronoField.HOUR_OF_DAY.checkValidValue(hour);
            ChronoField.MINUTE_OF_HOUR.checkValidValue(minute);
            ChronoField.SECOND_OF_MINUTE.checkValidValue(second);
            ChronoField.NANO_OF_SECOND.checkValidValue(nano);
        }
    }

    /**
     * Throws {@link LeapSecondException} if the value is second 60 of a minute where a leap second could
     * legitimately occur (or after the last known leap second), since java.time cannot represent it.
     *
     * @param hasOffset          Whether the value carries a timezone offset; UTC is assumed when it does not
     * @param offsetTotalSeconds The offset in seconds, ignored when {@code hasOffset} is false
     */
    public static void leapSecondCheck(final int year, final int month, final int day, final int hour, final int minute, final int second, final int nanos, final boolean hasOffset, final int offsetTotalSeconds)
    {
        if (second == LEAP_SECOND_SECONDS)
        {
            // Do not fall over trying to parse leap seconds
            final YearMonth needle = YearMonth.of(year, month);
            final boolean isValidLeapYearMonth = leapSecondHandler.isValidLeapSecondDate(needle);
            if (isValidLeapYearMonth || needle.isAfter(leapSecondHandler.getLastKnownLeapSecond()))
            {
                final int offsetSeconds = hasOffset ? offsetTotalSeconds : 0;
                final int utcHour = hour - offsetSeconds / 3_600;
                final int utcMinute = minute - (offsetSeconds % 3_600) / 60;
                if (((month == Month.DECEMBER.getValue() && day == 31) || (month == Month.JUNE.getValue() && day == 30))
                        && utcHour == 23
                        && utcMinute == 59)
                {
                    // Consider it a leap second
                    final OffsetDateTime nearest = OffsetDateTime.of(year, month, day, hour, minute, 59, nanos, ZoneOffset.ofTotalSeconds(offsetSeconds)).plusSeconds(1);
                    throw new LeapSecondException(nearest, second, isValidLeapYearMonth);
                }
            }
        }
    }

    public static boolean isValidDate(final int year, final int month, final int day)
    {
        if (month < 1 || month > 12 || day < 1 || year < -999_999_999 || year > 999_999_999)
        {
            return false;
        }
        switch (month)
        {
            case 2:
                return day <= (((year & 3) == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28);
            case 4:
            case 6:
            case 9:
            case 11:
                return day <= 30;
            default:
                return day <= 31;
        }
    }
}
