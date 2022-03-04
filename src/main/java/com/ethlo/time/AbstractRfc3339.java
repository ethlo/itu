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

public abstract class AbstractRfc3339 implements Rfc3339
{
    public static final int MAX_FRACTION_DIGITS = 9;

    protected void assertMaxFractionDigits(int fractionDigits)
    {
        if (fractionDigits > MAX_FRACTION_DIGITS)
        {
            throw new DateTimeException("Maximum supported number of fraction digits in second is "
                    + MAX_FRACTION_DIGITS + ", got " + fractionDigits);
        }
    }
}
