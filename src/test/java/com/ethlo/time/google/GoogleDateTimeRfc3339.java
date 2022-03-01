package com.ethlo.time.google;

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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.ethlo.time.Rfc3339Parser;
import com.google.api.client.util.DateTime;

public class GoogleDateTimeRfc3339 implements Rfc3339Parser
{
    @Override
    public OffsetDateTime parseDateTime(String dateTimeStr)
    {
        final DateTime.SecondsAndNanos secondsAndNanos = DateTime.parseRfc3339ToSecondsAndNanos(dateTimeStr);
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(secondsAndNanos.getSeconds(), secondsAndNanos.getNanos()), ZoneOffset.UTC);
    }

    @Override
    public boolean isValid(final String dateTimeStr)
    {
        try
        {
            parseDateTime(dateTimeStr);
            return true;
        }
        catch (NumberFormatException exc)
        {
            return false;
        }
    }
}
