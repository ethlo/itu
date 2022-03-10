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

import java.time.DateTimeException;
import java.time.OffsetDateTime;

/**
 * This exception is used to signal that there was a potentially valid leap-second in the parsed input.
 */
public class LeapSecondException extends DateTimeException
{
    private final int secondsInMinute;
    private final boolean isVerifiedValidLeapYearMonth;
    private final OffsetDateTime nearestDateTime;

    public LeapSecondException(OffsetDateTime nearestDateTime, int secondsInMinute, final boolean isVerifiedValidLeapYearMonth)
    {
        super("Leap second detected in input");
        this.nearestDateTime = nearestDateTime;
        this.secondsInMinute = secondsInMinute;
        this.isVerifiedValidLeapYearMonth = isVerifiedValidLeapYearMonth;
    }

    /**
     * The number of seconds, typically <code>60</code>.
     *
     * @return The number of seconds in this parsed date-time
     */
    public int getSecondsInMinute()
    {
        return secondsInMinute;
    }

    /**
     * Get the nearest date-time that is a roll-over to the next minute, (and potentially lower granularity fields) and 0 seconds.
     *
     * @return The date-time
     */
    public OffsetDateTime getNearestDateTime()
    {
        return nearestDateTime;
    }

    /**
     * Whether this is a date-time with a well-known leap-second
     *
     * @return True if known, otherwise false
     */
    public boolean isVerifiedValidLeapYearMonth()
    {
        return isVerifiedValidLeapYearMonth;
    }
}
