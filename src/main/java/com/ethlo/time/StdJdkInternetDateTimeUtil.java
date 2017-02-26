package com.ethlo.time;

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * The recommendation for date-time exchange in modern APIs is to use RFC-3339, available at https://tools.ietf.org/html/rfc3339
 * This class supports both validation, parsing and formatting of such date-times.
 * 
 * @author Ethlo, Morten Haraldsen
 */
public class StdJdkInternetDateTimeUtil implements InternetDateTimeUtil
{
    private SimpleDateFormat format;
    
    private DateTimeFormatter rfc3339baseFormatter = new DateTimeFormatterBuilder()
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

    private DateTimeFormatter rfc3339formatParser = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral('T')
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
        .toFormatter();

    public StdJdkInternetDateTimeUtil()
    {
        this.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss." + repeat('S', 3) + "XX");
    }
    
    @Override
    public String format(Date date, String timezone)
    {
        format.setTimeZone(TimeZone.getTimeZone(timezone));
        return format.format(date);
    }
    
    @Override
    public String formatUtc(Date date)
    {
        return formatUtc(OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC));
    }
    
    @Override
    public OffsetDateTime parse(final String s)
    {
        if (s == null || s.isEmpty())
        {
            return null;
        }
        
        return OffsetDateTime.from(rfc3339formatParser.parse(s));
    }
    
    private String repeat(char c, int repeats)
    {
        final char[] chars = new char[repeats];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    @Override
    public String format(OffsetDateTime date, String timezone)
    {
        return date.format(getFormatter(3).withZone(ZoneId.of(timezone)));
    }

    @Override
    public String formatUtc(OffsetDateTime date)
    {
        return formatUtc(date, 0);
    }
    
    @Override
    public boolean isValid(String dateTime)
    {
        try
        {
            parse(dateTime);
            return true;
        }
        catch (DateTimeException exc)
        {
            return false;
        }
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
        return getFormatter(fractionDigits).format(date);
    }
}