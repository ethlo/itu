package com.ethlo.time;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class Java7InternetDateTimeUtil implements InternetDateTimeUtil
{
    private final DateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    
    @Override
    public String formatUtc(OffsetDateTime date)
    {
        return formatUtc(new Date(date.toInstant().toEpochMilli()));
    }

    @Override
    public OffsetDateTime parse(String dateTimeStr)
    {
        try
        {
            return OffsetDateTime.ofInstant(parser.parse(dateTimeStr).toInstant(), ZoneOffset.UTC);
        }
        catch (ParseException exc)
        {
            throw new DateTimeException(exc.getMessage(), exc);
        }
    }

    @Override
    public String formatUtc(Date date)
    {
        return parser.format(date);
    }

    @Override
    public String formatUtcMilli(Date date)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String format(Date date, String timezone)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String format(Date date, String timezone, int fractionDigits)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValid(String dateTimeStr)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String formatUtcMilli(OffsetDateTime date)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String formatUtcMicro(OffsetDateTime date)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String formatUtcNano(OffsetDateTime date)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean allowUnknownLocalOffsetConvention()
    {
        // TODO Auto-generated method stub
        return false;
    }

}
