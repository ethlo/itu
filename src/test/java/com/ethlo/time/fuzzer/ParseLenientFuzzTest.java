package com.ethlo.time.fuzzer;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2024 Morten Haraldsen (ethlo)
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

/*
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.ethlo.time.DateTime;
import com.ethlo.time.ITU;

public class ParseLenientFuzzTest
{
    @com.code_intelligence.jazzer.junit.FuzzTest(maxDuration = "2m")
    void parse(FuzzedDataProvider data)
    {
        DateTime d = null;
        try
        {
            d = ITU.parseLenient(data.consumeRemainingAsString());
        }
        catch (DateTimeException ignored)
        {

        }

        if (d != null)
        {
            d.toInstant();
            System.out.println(d);
            d.toString();
        }
    }
}
*/