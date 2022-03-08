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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;

public enum Field
{
    // 2000-12-31T16:11:34.123456
    YEAR(4), MONTH(7), DAY(10), HOUR(13), MINUTE(16), SECOND(19), NANO(20);

    private final int requiredLength;

    Field(int requiredLength)
    {
        this.requiredLength = requiredLength;
    }

    public static Field valueOf(Class<? extends Temporal> type)
    {
        if (Year.class.equals(type))
        {
            return YEAR;
        }
        else if (YearMonth.class.equals(type))
        {
            return MONTH;
        }
        else if (LocalDate.class.equals(type))
        {
            return DAY;
        }
        else if (OffsetDateTime.class.equals(type))
        {
            return SECOND;
        }

        throw new IllegalArgumentException("Type " + type.getSimpleName() + " is not supported");
    }

    public int getRequiredLength()
    {
        return requiredLength;
    }
}
