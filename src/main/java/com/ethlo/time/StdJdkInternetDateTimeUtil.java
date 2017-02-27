package com.ethlo.time;

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * Java 8 JDK classes. The safe and normally "efficient enough" choice.
 * 
 * @author ethlo - Morten Haraldsen
 */
public class StdJdkInternetDateTimeUtil extends AbstractInternetDateTimeUtil
{
    private SimpleDateFormat[] formats = new SimpleDateFormat[9];
    
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
        .optionalStart()
        .appendLiteral('T')
        .optionalEnd()
        .optionalStart()
        .appendLiteral('t')
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

    public StdJdkInternetDateTimeUtil()
    {
        super(false);
        for (int i = 1; i < 9; i++)
        {
            this.formats[i] = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss." + repeat('S', i) + "XXX");
        }
    }
    
    @Override
    public String formatUtc(Date date)
    {
        return formatUtc(OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC), 3);
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
        assertMaxFractionDigits(fractionDigits);
        return getFormatter(fractionDigits).format(date);
    }

    @Override
    public String formatUtcMilli(Date date)
    {
        return formatUtcMilli(OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC));
    }

    @Override
    public String format(Date date, String timezone)
    {
        return format(date, timezone, 3);       
    }
    
    @Override
    public String format(Date date, String timezone, int fractionDigits)
    {
        final SimpleDateFormat formatter = (SimpleDateFormat)formats[fractionDigits].clone();
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        return formatter.format(date);
    }
}