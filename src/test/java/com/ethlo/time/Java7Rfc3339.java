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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class Java7Rfc3339 implements Rfc3339
{
    private final DateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private final DateFormat fractionParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Override
    public String formatUtc(OffsetDateTime date)
    {
        return formatUtc(new Date(date.toInstant().toEpochMilli()));
    }

    @Override
    public OffsetDateTime parseDateTime(String dateTimeStr)
    {
        try
        {
            return OffsetDateTime.ofInstant(fractionParser.parse(dateTimeStr).toInstant(), ZoneOffset.UTC);
        }
        catch (ParseException fractionExc)
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
}
