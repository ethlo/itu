package com.ethlo.time;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
public class InternetDateTimeUtil
{
    private final FractionType fractions;
    private List<String> validFormats;
    private SimpleDateFormat format;
    private List<SimpleDateFormat> formats;
    
    public enum FractionType
    {
        NONE(0),
        MILLISECONDS(3),
        MICROSECONDS(6),
        NANOSECONDS(9);
        
        private FractionType(int digits)
        {
            this.digits = digits;
        }
        
        public int getDigits()
        {
            return digits;
        }
        
        private int digits;
    }
    
    private final static String[] validFormatPatterns =
    {
        "yyyy-MM-dd'T'HH:mm:ssZZZZZ", 
        "yyyy-MM-dd'T'HH:mm:ss.*ZZZZZ",
        "yyyy-MM-dd'T'HH:mm:ssX", 
        "yyyy-MM-dd'T'HH:mm:ss.*X",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.*'Z'"
    };

    
    public InternetDateTimeUtil()
    {
        this(FractionType.MILLISECONDS);
    }
    
    public InternetDateTimeUtil(FractionType fractions)
    {
        this.fractions = fractions;
        
        this.validFormats = Arrays.asList(validFormatPatterns)
            .stream()
            .map(f->{return f.replace("*", repeat('S', fractions.getDigits()));})
            .collect(Collectors.toList());
        
        this.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss." + repeat('S', fractions.getDigits()) + "XX");
        
        this.formats = createFormats();
    }
    
    /**
     * Format the provided date with the defined time-zone
     * @param date The date to format
     * @param timezone The time zone to format the date-time according to
     * @return The formatted date-time string
     */
    public String format(Date date, String timezone)
    {
        format.setTimeZone(TimeZone.getTimeZone(timezone));
        return format.format(date);
    }
    
    /**
     * Format the {@link Date} as a UTC formatted date-time string
     * @param date The date to format
     * @return the formatted string
     */
    public String formatUtc(Date date)
    {
        return getUtcFormat().format(date);
    }
    
    /**
     * Parse the date-time and return it as a {@link Date} in UTC time-zone.
     * @param dateTimeStr The date-time string to parse
     * @return The instant defined by the date-time in UTC time-zone 
     */
    public Date parse(final String s)
    {
        if (s == null || s.isEmpty())
        {
            return null;
        }

        // Java doesn't properly handle the 'Z' literal so we replace it manually with UTC time
        String dateTimeStr = s;
        if (dateTimeStr.endsWith("Z"))
        {
            dateTimeStr = s.replace("Z", "+0000");
        }

        for (SimpleDateFormat format : formats)
        {
            try
            {
                return format.parse(dateTimeStr);
            }
            catch (ParseException e)
            {
                // Parsing failed for this pattern, try next
            }
        }

        // All patterns failed
        throw new IllegalArgumentException("Invalid format of " + s + " according to RFC-3339. "
              + "Valid formats include " + validFormats);
    }

    private List<SimpleDateFormat> createFormats()
    {
        final List<SimpleDateFormat> retVal = new ArrayList<>(this.validFormats.size());
        for (String f : validFormats)
        {
            retVal.add(new SimpleDateFormat(f));
        }
        return retVal;
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
}