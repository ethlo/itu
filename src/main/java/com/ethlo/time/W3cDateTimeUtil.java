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

import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

/** This class deals with the formats mentioned in W3C - NOTE-datetime: https://www.w3.org/TR/NOTE-datetime
 * 
 * <ul>
 * <li>Year:<br> 
 *     YYYY (eg 1997)
 * </li>
 * <li>
 *     Year and month:<br>
 *     YYYY-MM (eg 1997-07)</li>
 *  <li>
 *     Complete date:<br>
 *     YYYY-MM-DD (eg 1997-07-16)
 * </li>
 * <li>
 *     Complete date plus hours and minutes:<br>
 *     YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
 * </li>
 * <li>
 *     Complete date plus hours, minutes and seconds:<br>
 *     YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
 * </li>
 * <li>
 *     Complete date plus hours, minutes, seconds and a decimal fraction of a second<br>
 *     YYYY-MM-DDThh:mm:ss.STZD (eg 1997-07-16T19:20:30.45+01:00)
 * </li>
 * </ul>   
 * <p>
 * where:
 *</p>
 * <ul>
 *  <li>YYYY = four-digit year</li>
 *  <li>MM = two-digit month (01=January, etc.)</li>
 *  <li>DD   = two-digit day of month (01 through 31)</li>
 *  <li>hh   = two digits of hour (00 through 23) (am/pm NOT allowed)</li>
 *  <li>mm   = two digits of minute (00 through 59)</li>
 *  <li>ss   = two digits of second (00 through 59)</li>
 *  <li>S   = one or more digits representing a decimal fraction of a second</li>
 *  <li>TZD  = time zone designator (Z or +hh:mm or -hh:mm)</li>
 * </ul>
 *
*/
public interface W3cDateTimeUtil
{
    /**
     * Format the date/date-time in UTC format
     * @param date The date to format
     * @param lastIncluded The last included field
     * @param fractionDigits The number of fraction digits, if applicable
     * @return the formatted date/date-time
     */
    String formatUtc(OffsetDateTime date, Field lastIncluded, int fractionDigits);

    /**
     * Parse the format with an expected type, i.e sub-class of {@link Temporal}
     * @param s The date/date-time to parse 
     * @param type The type you expect
     * @param <T> The expected type
     * @return The parsed date/date-time 
     */
    <T extends Temporal> T parseLenient(String s, Class<T> type);

    /**
     * Parse the format and return it as a fitting sub-class of {@link Temporal}
     * @param s The date/date-time to parse
     * @return The parsed date/date-time
     */
    Temporal parseLenient(String s);
}
