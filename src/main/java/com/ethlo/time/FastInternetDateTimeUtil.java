package com.ethlo.time;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import com.ethlo.util.Assert;
import com.ethlo.util.CharArrayIntegerUtil;
import com.ethlo.util.CharArrayUtil;

public class FastInternetDateTimeUtil implements InternetDateTimeUtil
{
    private final StdJdkInternetDateTimeUtil delegate = new StdJdkInternetDateTimeUtil();
    
    private static final char dateSep = '-';
    private static final char timeSep = ':';
    private static final char sep = 'T';
    private static final char fractionSep = '.';

    private static final int[] widths = new int[]{100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10, 1};
    private static char zulu = 'Z';
    
    @Override
    public OffsetDateTime parse(String s)
    {
        if (s == null || s.isEmpty())
        {
            return null;
        }
        
        if (s.length() < 20)
        {
            throw new DateTimeException("Invalid date-time: " + s);
        }
        
        final char[] chars = s.toCharArray();
        
        // Date portion
        final int year = CharArrayIntegerUtil.parsePositiveInt(chars, 10, 0, 4);
        isTrue(chars, 4, dateSep);
        final int month = CharArrayIntegerUtil.parsePositiveInt(chars, 10, 5, 7);
        isTrue(chars, 7, dateSep);
        final int day = CharArrayIntegerUtil.parsePositiveInt(chars, 10, 8, 10);
        
        // Time starts
        Assert.isTrue(chars[10] == 'T');
        final int hour = CharArrayIntegerUtil.parsePositiveInt(chars, 10, 11, 13);
        isTrue(chars, 13, timeSep);
        final int minute = CharArrayIntegerUtil.parsePositiveInt(chars, 10, 14, 16);
        isTrue(chars, 16, timeSep);
        final int second = CharArrayIntegerUtil.parsePositiveInt(chars, 10, 17, 19);
        
        // From here the specification is more lenient
        final int remaining = chars.length - 19;
        
        ZoneOffset offset = null;
        int fractions = 0;
        
        if (remaining == 1 && chars[19] == 'Z' || chars[19] == 'z')
        {
            // Do nothing we are done
            offset = ZoneOffset.UTC;
            assertNoMoreChars(chars, 19);
        }
        else if (chars[19] == fractionSep)
        {
            // We have fractional seconds
            final int idx = CharArrayUtil.indexOfNonDigit(chars, 20);
            if (idx != -1)
            {
                // We have an end of fractions
                final int len = idx - 20;
                fractions = CharArrayIntegerUtil.parsePositiveInt(chars, 10, 20, idx);
                if (len == 1) {fractions = fractions * 100_000_000;}
                if (len == 2) {fractions = fractions * 10_000_000;}
                if (len == 3) {fractions = fractions * 1_000_000;}
                if (len == 4) {fractions = fractions * 100_000;}
                if (len == 5) {fractions = fractions * 10_000;}
                if (len == 6) {fractions = fractions * 1_000;}
                if (len == 7) {fractions = fractions * 100;}
                if (len == 8) {fractions = fractions * 10;}
                offset = parseTz(chars, idx);
            }
            else
            {
                offset = parseTz(chars, 20);
            }
        }
        else if (chars[19] == '+' || chars[19] == '-')
        {
            // No fractional sections
            offset = parseTz(chars, 19);
        }
        else
        {
            throw new UnsupportedOperationException();
        }
        
        return OffsetDateTime.of(year, month, day, hour, minute, second, fractions, offset); 
    }

    private void isTrue(char[] chars, int offset, char expected)
    {
        if (chars[offset] != expected)
        {
            throw new DateTimeException("Expected character " + expected + " at position " + offset);
        }
    }

    private ZoneOffset parseTz(char[] chars, int offset)
    {
        final int left = chars.length - offset;
        if (chars[offset] == 'Z' || chars[offset] == 'z')
        {
            assertNoMoreChars(chars, offset);
            return ZoneOffset.UTC;
        }
        
        if (left != 6)
        {
            throw new DateTimeException("Invalid offset: " + new String(chars, offset, left));
        }
        
        return ZoneOffset.of(new String(chars, offset, left));
    }

    private void assertNoMoreChars(char[] chars, int lastUsed)
    {
        if (chars.length > lastUsed + 1)
        {
            throw new DateTimeException("Unparsed data from offset " + lastUsed + 1);
        }
    }
    
    @Override
    public String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        final LocalDateTime utc = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        
        final char[] buf = new char[64];
        
        // Date
        CharArrayIntegerUtil.toString(utc.getYear(), buf, 0, 4);
        buf[4] = dateSep;
        CharArrayIntegerUtil.toString(utc.getMonthValue(), buf, 5, 2);
        buf[7] = dateSep;
        CharArrayIntegerUtil.toString(utc.getDayOfMonth(), buf, 8, 2);
        
        // T separator
        buf[10] = sep;
        
        // Time
        CharArrayIntegerUtil.toString(utc.getHour(), buf, 11, 2);
        buf[13] = timeSep;
        CharArrayIntegerUtil.toString(utc.getMinute(), buf, 14, 2);
        buf[16] = timeSep;
        CharArrayIntegerUtil.toString(utc.getSecond(), buf, 17, 2);
        
        // Second fractions
        final boolean hasFractionDigits = fractionDigits > 0;
        if (hasFractionDigits)
        {
            buf[19] = fractionSep;
            addFractions(buf, fractionDigits, utc.getNano());
        }
        
        // Add time-zone 'Z'
        buf[(hasFractionDigits ? 20 + fractionDigits : 19)] = zulu;
        final int length = hasFractionDigits ? 21 + fractionDigits : 20;
        
        return new String(buf, 0, length);
    }

    private void addFractions(char[] buf, int fractionDigits, int nano)
    {
        if (fractionDigits < 0)
        {
            fractionDigits = 0;
        }
        
        if (fractionDigits > 9)
        {
            fractionDigits = 9;
        }
        
        final double d = widths[fractionDigits - 1];
        CharArrayIntegerUtil.toString((int)(nano / d), buf, 20, fractionDigits);
    }

    @Override
    public String format(OffsetDateTime date, String timezone)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String formatUtc(Date date)
    {
        return formatUtc(OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC));
    }

    @Override
    public String format(Date date, String timezone)
    {
        return delegate.format(date, timezone);
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
    public String formatUtc(OffsetDateTime date)
    {
        return formatUtc(date, 0);
    }
}