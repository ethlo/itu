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

import static com.ethlo.time.internal.EthloITU.DATE_SEPARATOR;
import static com.ethlo.time.internal.EthloITU.SEPARATOR_UPPER;
import static com.ethlo.time.internal.EthloITU.TIME_SEPARATOR;
import static com.ethlo.time.internal.EthloITU.finish;
import static com.ethlo.time.internal.LeapSecondHandler.LEAP_SECOND_SECONDS;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Objects;
import java.util.Optional;

import com.ethlo.time.internal.DateTimeMath;
import com.ethlo.time.internal.DefaultLeapSecondHandler;
import com.ethlo.time.internal.LeapSecondHandler;
import com.ethlo.time.internal.LimitedCharArrayIntegerUtil;

/**
 * Container class for parsed date/date-time data. The {@link #getMostGranularField()} contains the highest granularity field found, like MONTH, MINUTE, SECOND.
 */
public class DateTime implements TemporalAccessor
{
    private static final LeapSecondHandler leapSecondHandler = new DefaultLeapSecondHandler();
    private final Field field;
    private final int year;
    private final int month;
    private final int day;
    private final int hour;
    private final int minute;
    private final int second;
    private final int nano;
    private final TimezoneOffset offset;
    private final int fractionDigits;

    private final int charLength;

    public DateTime(final Field field, final int year, final int month, final int day, final int hour, final int minute, final int second, final int nano, final TimezoneOffset offset, final int fractionDigits)
    {
        this(field, year, month, day, hour, minute, second, nano, offset, fractionDigits, -1);
    }

    public DateTime(final Field field, final int year, final int month, final int day, final int hour, final int minute, final int second, final int nano, final TimezoneOffset offset, final int fractionDigits, int charLength)
    {
        this.field = field;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.nano = nano;
        this.offset = offset;
        this.fractionDigits = fractionDigits;
        leapSecondCheck(year, month, day, hour, minute, second, nano, offset);
        validated();
        this.charLength = charLength;
    }

    /**
     * Create a new instance with second granularity from the input parameters
     *
     * @param year   year
     * @param month  month
     * @param day    day
     * @param hour   hour
     * @param minute minute
     * @param second second
     * @param offset timezone offset
     * @return A DateTime with second granularity
     */
    public static DateTime of(int year, int month, int day, int hour, int minute, int second, TimezoneOffset offset)
    {
        return new DateTime(Field.SECOND, year, month, day, hour, minute, second, 0, offset, 0);
    }

    /**
     * Create a new instance with nanosecond granularity from the input parameters
     *
     * @param year           year
     * @param month          month
     * @param day            day
     * @param hour           hour
     * @param minute         minute
     * @param second         second
     * @param nanos          nanos
     * @param offset         timezone offset
     * @param fractionDigits The granularity of the fractional seconds field
     * @return A DateTime with nanosecond granularity
     */
    public static DateTime of(int year, int month, int day, int hour, int minute, int second, int nanos, TimezoneOffset offset, final int fractionDigits)
    {
        return new DateTime(Field.NANO, year, month, day, hour, minute, second, nanos, offset, fractionDigits);
    }

    /**
     * Create a new instance with year granularity from the input parameters
     *
     * @param year The year
     * @return a new instance with year granularity from the input parameters
     */
    public static DateTime ofYear(int year)
    {
        return new DateTime(Field.YEAR, year, 0, 0, 0, 0, 0, 0, null, 0);
    }

    /**
     * Create a new instance with year-month granularity from the input parameters
     *
     * @param year  The year
     * @param month The month
     * @return a new instance with year-month granularity from the input parameters
     */
    public static DateTime ofYearMonth(int year, int month)
    {
        return new DateTime(Field.MONTH, year, month, 0, 0, 0, 0, 0, null, 0);
    }

    /**
     * Create a new instance with day granularity from the input parameters
     *
     * @param year  The year
     * @param month The month
     * @param day   The day
     * @return a new instance with day granularity from the input parameters
     */
    public static DateTime ofDate(int year, int month, int day)
    {
        return new DateTime(Field.DAY, year, month, day, 0, 0, 0, 0, null, 0);
    }

    /**
     * Create a new instance with minute granularity from the input parameters
     *
     * @param year   The year
     * @param month  The month
     * @param day    The day
     * @param hour   The hour
     * @param minute The minute
     * @param offset The timezone offset
     * @return a new instance with minute granularity from the input parameters
     */
    public static DateTime of(int year, int month, int day, int hour, int minute, TimezoneOffset offset)
    {
        return new DateTime(Field.MINUTE, year, month, day, hour, minute, 0, 0, offset, 0);
    }

    /**
     * Create a new instance with data from the specified date-time.
     *
     * @param dateTime The date-time to copy data from
     * @return A new instance
     */
    public static DateTime of(OffsetDateTime dateTime)
    {
        return DateTime.of(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getNano(), TimezoneOffset.of(dateTime.getOffset()), 9);
    }

    private static void leapSecondCheck(int year, int month, int day, int hour, int minute, int second, int nanos, TimezoneOffset offset)
    {
        if (second == LEAP_SECOND_SECONDS)
        {
            // Do not fall over trying to parse leap seconds
            final YearMonth needle = YearMonth.of(year, month);
            final boolean isValidLeapYearMonth = leapSecondHandler.isValidLeapSecondDate(needle);
            if (isValidLeapYearMonth || needle.isAfter(leapSecondHandler.getLastKnownLeapSecond()))
            {
                final int utcHour = hour - offset.getTotalSeconds() / 3_600;
                final int utcMinute = minute - (offset.getTotalSeconds() % 3_600) / 60;
                if (((month == Month.DECEMBER.getValue() && day == 31) || (month == Month.JUNE.getValue() && day == 30))
                        && utcHour == 23
                        && utcMinute == 59)
                {
                    // Consider it a leap second
                    final OffsetDateTime nearest = OffsetDateTime.of(year, month, day, hour, minute, 59, nanos, offset.toZoneOffset()).plusSeconds(1);
                    throw new LeapSecondException(nearest, second, isValidLeapYearMonth);
                }
            }
        }
    }

    public static DateTime ofNanos(int year, int month, int day, int hour, int minute, int second, int nanos, TimezoneOffset timezoneOffset, int fractionDigits, int charLength)
    {
        return new DateTime(fractionDigits > 0 ? Field.NANO : Field.SECOND, year, month, day, hour, minute, second, nanos, timezoneOffset, fractionDigits, charLength);
    }

    /**
     * Returns if the specified field is part of this date/date-time
     *
     * @param field The field to check for
     * @return True if included, otherwise false
     */
    public boolean includesGranularity(Field field)
    {
        return field.ordinal() <= this.field.ordinal();
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
     * Returns the time offset, if available
     *
     * @return the time offset, if available
     */
    public Optional<TimezoneOffset> getOffset()
    {
        return Optional.ofNullable(offset);
    }

    /**
     * Creates a {@link Year} discarding any higher granularity fields
     *
     * @return the {@link Year}
     */
    public Year toYear()
    {
        return Year.of(year);
    }

    /**
     * Creates a {@link YearMonth} discarding any higher granularity fields
     *
     * @return the {@link YearMonth}
     */
    public YearMonth toYearMonth()
    {
        assertMinGranularity(Field.MONTH);
        return YearMonth.of(year, month);
    }

    /**
     * Creates a {@link LocalDateTime} discarding any timezone information
     *
     * @return the {@link LocalDateTime}
     */
    public LocalDateTime toLocalDatetime()
    {
        assertMinGranularity(Field.MINUTE);
        return LocalDateTime.of(year, month, day, hour, minute, second, nano);
    }

    /**
     * Creates an {@link OffsetDateTime}
     *
     * @return the {@link OffsetDateTime}
     */
    public OffsetDateTime toOffsetDatetime()
    {
        assertMinGranularity(Field.MINUTE);
        if (offset != null)
        {
            return OffsetDateTime.of(year, month, day, hour, minute, second, nano, offset.toZoneOffset());
        }
        final String chars = toString();
        throw new DateTimeParseException("No timezone information: " + chars, chars, chars.length());
    }

    /**
     * Creates a {@link LocalDate}, discarding any higher granularity fields
     *
     * @return the {@link LocalDate}
     */
    public LocalDate toLocalDate()
    {
        assertMinGranularity(Field.DAY);
        return LocalDate.of(year, month, day);
    }

    /**
     * Returns the most granular field found during parsing
     *
     * @return The field found
     */
    public Field getMostGranularField()
    {
        return field;
    }

    private void assertMinGranularity(Field field)
    {
        if (!includesGranularity(field))
        {
            throw new DateTimeFormatException("No " + field.name() + " field found");
        }
    }

    /**
     * Formats this date-time as an ISO formatted string with the last included field as specified.
     *
     * @param lastIncluded The last specified field to include
     * @return The formatted date/date-time string
     */
    public String toString(final Field lastIncluded)
    {
        return toString(this, lastIncluded, 0);
    }

    /**
     * Formats this date-time as an RFC-3339 compatible string with the specified number of fractions in the second.
     *
     * @param fractionDigits The number of fractions to include
     * @return The formatted date/date-time string
     */
    public String toString(final int fractionDigits)
    {
        return toString(this, Field.NANO, fractionDigits);
    }

    private String toString(final DateTime date, final Field lastIncluded, final int fractionDigits)
    {
        if (lastIncluded.ordinal() > date.getMostGranularField().ordinal())
        {
            throw new DateTimeFormatException("Requested granularity was " + lastIncluded.name() + ", but contains only granularity " + date.getMostGranularField().name());
        }
        final TimezoneOffset tz = date.getOffset().orElse(null);
        final char[] buffer = new char[35];

        // YEAR
        LimitedCharArrayIntegerUtil.toString(date.getYear(), buffer, 0, 4);
        if (lastIncluded == Field.YEAR)
        {
            return finish(buffer, Field.YEAR.getRequiredLength(), null);
        }

        // MONTH
        if (lastIncluded.ordinal() >= Field.MONTH.ordinal())
        {
            buffer[4] = DATE_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getMonth(), buffer, 5, 2);
        }
        if (lastIncluded == Field.MONTH)
        {
            return finish(buffer, Field.MONTH.getRequiredLength(), null);
        }

        // DAY
        if (lastIncluded.ordinal() >= Field.DAY.ordinal())
        {
            buffer[7] = DATE_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getDayOfMonth(), buffer, 8, 2);
        }
        if (lastIncluded == Field.DAY)
        {
            return finish(buffer, Field.DAY.getRequiredLength(), null);
        }

        // HOUR
        if (lastIncluded.ordinal() >= Field.HOUR.ordinal())
        {
            buffer[10] = SEPARATOR_UPPER;
            LimitedCharArrayIntegerUtil.toString(date.getHour(), buffer, 11, 2);
        }
        if (lastIncluded == Field.HOUR)
        {
            return finish(buffer, Field.HOUR.getRequiredLength(), tz);
        }

        // MINUTE
        if (lastIncluded.ordinal() >= Field.MINUTE.ordinal())
        {
            buffer[13] = TIME_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getMinute(), buffer, 14, 2);
        }
        if (lastIncluded == Field.MINUTE)
        {
            return finish(buffer, Field.MINUTE.getRequiredLength(), tz);
        }

        // SECOND
        if (lastIncluded.ordinal() >= Field.SECOND.ordinal())
        {
            buffer[16] = TIME_SEPARATOR;
            LimitedCharArrayIntegerUtil.toString(date.getSecond(), buffer, 17, 2);
        }
        if (lastIncluded == Field.SECOND)
        {
            return finish(buffer, Field.SECOND.getRequiredLength(), tz);
        }

        // Fractions
        if (lastIncluded.ordinal() >= Field.NANO.ordinal())
        {
            buffer[19] = '.';
            LimitedCharArrayIntegerUtil.toString(date.getNano(), buffer, 20, fractionDigits);
        }
        return finish(buffer, 20 + fractionDigits, tz);
    }

    /**
     * Return the number of significant fraction digits in the second.
     *
     * @return The number of significant fraction digits
     */
    public int getFractionDigits()
    {
        return fractionDigits;
    }

    /**
     * Formats this date-time as a date/date-time with the same fields as was parsed
     *
     * @return The formatted date/date-time string
     */
    @Override
    public String toString()
    {
        return fractionDigits > 0 ? toString(fractionDigits) : toString(field);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        DateTime dateTime = (DateTime) o;
        return year == dateTime.year
                && month == dateTime.month
                && day == dateTime.day
                && hour == dateTime.hour
                && minute == dateTime.minute
                && second == dateTime.second
                && nano == dateTime.nano
                && fractionDigits == dateTime.fractionDigits
                && field == dateTime.field
                && Objects.equals(offset, dateTime.offset);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(field.ordinal(), year, month, day, hour, minute, second, nano, offset, fractionDigits);
    }

    @Override
    public boolean isSupported(final TemporalField field)
    {
        return Field.of(field).ordinal() <= this.field.ordinal();
    }

    @Override
    public long getLong(final TemporalField temporalField)
    {
        if (temporalField.equals(ChronoField.YEAR))
        {
            return year;
        }
        else if (temporalField.equals(ChronoField.MONTH_OF_YEAR))
        {
            return month;
        }
        else if (temporalField.equals(ChronoField.DAY_OF_MONTH))
        {
            return day;
        }
        else if (temporalField.equals(ChronoField.HOUR_OF_DAY))
        {
            return hour;
        }
        else if (temporalField.equals(ChronoField.MINUTE_OF_HOUR))
        {
            return minute;
        }
        else if (temporalField.equals(ChronoField.SECOND_OF_MINUTE))
        {
            return second;
        }
        else if (temporalField.equals(ChronoField.NANO_OF_SECOND))
        {
            return nano;
        }
        else if (temporalField.equals(ChronoField.INSTANT_SECONDS))
        {
            if (offset != null)
            {
                return toEpochSeconds();
            }
        }

        throw new UnsupportedTemporalTypeException("Unsupported field: " + temporalField);
    }

    /**
     * <p>This method will attempt to create an Instant from whatever granularity is available in the parsed year/date/date-time.</p>
     * <p>Missing fields will be replaced by their lowest allowed value: 1 for month and day, 0 for any missing time component.</p>
     * <p>NOTE: If there is no time-zone defined, UTC will be assumed</p>
     *
     * @return An instant representing the point in time.
     */
    public Instant toInstant()
    {
        return Instant.ofEpochSecond(toEpochSeconds(), nano);
    }

    private long toEpochSeconds()
    {
        final long secsSinceMidnight = hour * 3600L + minute * 60L + second;
        final long daysInSeconds = DateTimeMath.daysFromCivil(year, month != 0 ? month : 1, day != 0 ? day : 1) * 86_400;
        final long tsOffset = offset != null ? offset.getTotalSeconds() : 0;
        return (daysInSeconds + secsSinceMidnight) - tsOffset;
    }

    private void validated()
    {
        if (field.ordinal() > Field.DAY.ordinal())
        {
            //noinspection ResultOfMethodCallIgnored
            LocalDate.of(year, month, day);
        }

        if (second > 59)
        {
            throw new DateTimeException("Invalid value for SecondOfMinute (valid values 0 - 59): " + second);
        }
    }

    public int getParseLength()
    {
        return charLength;
    }
}
