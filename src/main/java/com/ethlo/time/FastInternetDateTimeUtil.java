package com.ethlo.time;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import com.ethlo.util.CharArrayIntegerUtil;
import com.ethlo.util.CharArrayUtil;

public class FastInternetDateTimeUtil implements InternetDateTimeUtil
{
    private static final char dateSep = '-';
    private static final char[] dateSepArr = new char[]{dateSep};
    private static final char timeSep = ':';
    private static final char[] timeSepArr = new char[]{timeSep};
    private static final char[] sepArr = new char[]{'T'};
    private static final char fractionSep = '.';
    private static final char[] fractionSepArr = new char[]{fractionSep};
    private static char[] zuluArr = new char[]{'Z'};
    
    public OffsetDateTime parse(String s)
    {
        if (s == null || s.isEmpty())
        {
            return null;
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
                Assert.isTrue(len == 3 || len == 6 | len == 9);
                fractions = CharArrayIntegerUtil.parsePositiveInt(chars, 10, 20, idx);
                if (len == 3) {fractions = fractions * 1_000_000;}
                if (len == 6) {fractions = fractions * 1_000;}
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
        return ZoneOffset.of(new String(chars, offset, chars.length - offset));
    }

    private void assertNoMoreChars(char[] chars, int lastUsed)
    {
        if (chars.length > lastUsed + 1)
        {
            throw new IllegalArgumentException("Trailing noise from offset " + lastUsed + 1);
        }
    }
    
    public String formatUtc(OffsetDateTime date)
    {
        final LocalDateTime utc = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        return new String(CharArrayUtil.merge(
            pad(CharArrayIntegerUtil.toString(utc.getYear()), 4),
            dateSepArr,
            pad(CharArrayIntegerUtil.toString(utc.getMonthValue()), 2),
            dateSepArr,
            pad(CharArrayIntegerUtil.toString(utc.getDayOfMonth()), 2),
            sepArr,
            pad(CharArrayIntegerUtil.toString(utc.getHour()), 2),
            timeSepArr,
            pad(CharArrayIntegerUtil.toString(utc.getMinute()), 2),
            timeSepArr,
            pad(CharArrayIntegerUtil.toString(utc.getSecond()), 2),
            fractionSepArr,
            pad(CharArrayIntegerUtil.toString(utc.getNano() / 1_000_000), 3),
            zuluArr
            ));
    }
    
    private char[] pad(char[] val, int length)
    {
        return CharArrayUtil.zeroPad(val, length);
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
        // TODO: Implement me
        throw new UnsupportedOperationException();
    }
}