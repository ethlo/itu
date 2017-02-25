package com.ethlo.time;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * The recommendation for date-time exchange in modern APIs is to use RFC-3339, available at https://tools.ietf.org/html/rfc3339
 * This class supports both validation, parsing and formatting of such date-times.
 * 
 * @author Ethlo, Morten Haraldsen
 */
public interface InternetDateTimeUtil
{
    /**
     * Format the provided date with the defined time-zone
     * @param date The date to format
     * @param timezone The time zone to format the date-time according to
     * @return The formatted date-time string
     */
    String format(OffsetDateTime date, String timezone);

    /**
     * Format the {@link Date} as a UTC formatted date-time string
     * @param date The date to format
     * @return the formatted string
     */
    String formatUtc(OffsetDateTime date);

    /**
     * Parse the date-time and return it as a {@link Date} in UTC time-zone.
     * @param dateTimeStr The date-time string to parse
     * @return The instant defined by the date-time in UTC time-zone 
     */
    OffsetDateTime parse(String s);

    String formatUtc(Date date);

    String format(Date date, String timezone);
}