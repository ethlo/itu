package com.ethlo.time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * The recommendation for date-time exchange in modern APIs is to use RFC-3339, available at https://tools.ietf.org/html/rfc3339
 * This class supports both validation, parsing and formatting of such date-times.
 * 
 * @author Ethlo, Morten Haraldsen
 */
public class StdJdkInternetDateTimeUtil implements InternetDateTimeUtil
{
    private final FractionType fractions;
    private List<String> validFormats;
    private SimpleDateFormat format;
    private DateTimeFormatter rfc3339formatter = new DateTimeFormatterBuilder()
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
        .optionalStart()
        .appendLiteral('.')
        .appendFraction(ChronoField.NANO_OF_SECOND, 3, 9, false)
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HHMM", "Z")
        .optionalEnd()
        .toFormatter();

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
        .appendFraction(ChronoField.NANO_OF_SECOND, 3, 9, false)
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HHMM", "Z")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HH:MM", "Z")
        .optionalEnd()
        .toFormatter();

    
    private final static String[] validFormatPatterns =
    {
        "yyyy-MM-dd'T'HH:mm:ssZZZZZ", 
        "yyyy-MM-dd'T'HH:mm:ss.*ZZZZZ",
        "yyyy-MM-dd'T'HH:mm:ssX", 
        "yyyy-MM-dd'T'HH:mm:ss.*X",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.*'Z'"
    };

    
    public StdJdkInternetDateTimeUtil()
    {
        this(FractionType.MILLISECONDS);
    }
    
    public StdJdkInternetDateTimeUtil(FractionType fractions)
    {
        this.fractions = fractions;
        
        this.validFormats = Arrays.asList(validFormatPatterns)
            .stream()
            .map(f->{return f.replace("*", repeat('S', fractions.getDigits()));})
            .collect(Collectors.toList());
        
        this.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss." + repeat('S', fractions.getDigits()) + "XX");
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
        return getUtcFormat().format(date);
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

    private DateFormat getUtcFormat()
    {
        final SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss." + repeat('S', fractions.getDigits()) + "+0000");
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utc;
    }

    @Override
    public String format(OffsetDateTime date, String timezone)
    {
        return date.format(rfc3339formatter.withZone(ZoneId.of(timezone)));
    }

    @Override
    public String formatUtc(OffsetDateTime date)
    {
        return date.format(rfc3339formatter.withZone(ZoneOffset.UTC));
    }
}