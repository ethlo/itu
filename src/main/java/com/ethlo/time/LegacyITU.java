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

import java.util.Date;

public class LegacyITU
{
    private static final EthloITU delegate = new EthloITU();

    private LegacyITU()
    {
    }

    public static String formatUtc(Date date)
    {
        return delegate.formatUtc(date);
    }

    public static String format(Date date, String timezone)
    {
        return delegate.format(date, timezone);
    }

    public static String formatUtcMilli(Date date)
    {
        return delegate.formatUtcMilli(date);
    }

    public static String format(Date date, String timezone, int fractionDigits)
    {
        return delegate.format(date, timezone, fractionDigits);
    }
}
