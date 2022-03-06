package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2018 Morten Haraldsen (ethlo)
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

import java.time.DateTimeException;
import java.time.OffsetDateTime;

public class ITU
{
    private static final EthloITU delegate = new EthloITU();

    private ITU()
    {
    }

    public static OffsetDateTime parseDateTime(String s)
    {
        return delegate.parseDateTime(s);
    }

    public static DateTime parseLenient(String text)
    {
        return delegate.parse(text);
    }

    public static boolean isValid(String dateTime)
    {
        try
        {
            parseDateTime(dateTime);
            return true;
        }
        catch (DateTimeException exc)
        {
            return false;
        }
    }

    public static String formatUtc(OffsetDateTime date, int fractionDigits)
    {
        return delegate.formatUtc(date, fractionDigits);
    }

    public static String formatUtc(OffsetDateTime date, Field lastIncluded)
    {
        return delegate.formatUtc(date, lastIncluded);
    }

    public static String formatUtc(OffsetDateTime date)
    {
        return delegate.formatUtc(date);
    }

    public static String formatUtcMilli(OffsetDateTime date)
    {
        return delegate.formatUtcMilli(date);
    }

    public static String formatUtcMicro(OffsetDateTime date)
    {
        return delegate.formatUtcMicro(date);
    }

    public static String formatUtcNano(OffsetDateTime date)
    {
        return delegate.formatUtcNano(date);
    }
}
