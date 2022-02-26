package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2022 Morten Haraldsen (ethlo)
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

import java.time.OffsetDateTime;

public interface Rfc3339Parser
{
    /**
     * Parse the date-time and return it as a {@link OffsetDateTime}.
     *
     * @param dateTimeStr The date-time string to parse
     * @return The {@link OffsetDateTime} as parsed from the input
     */
    OffsetDateTime parseDateTime(String dateTimeStr);

    /**
     * Check whether the string is a valid date-time according to RFC-3339
     *
     * @param dateTimeStr The date-time to validate
     * @return True if valid date-time or null, false otherwise
     */
    boolean isValid(String dateTimeStr);
}
