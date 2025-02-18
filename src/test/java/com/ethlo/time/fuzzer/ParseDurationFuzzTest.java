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
import java.time.format.DateTimeParseException;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.ethlo.time.Duration;
import com.ethlo.time.ITU;

public class ParseDurationFuzzTest
{
    @FuzzTest(maxDuration = "30m")
    void parse(FuzzedDataProvider data)
    {
        final String input = data.consumeString(70);
        Duration d = null;
        try
        {
            d = ITU.parseDuration(input);
        }
        catch (DateTimeException exc)
        {
            //System.out.println(input + "=" + exc.getMessage());
        }

        if (d != null)
        {
            if (testJavaDuration(input))
            {
                System.out.println("Input: " + input);
                System.out.println("Parsed: " + d);
            }
        }
    }

    private boolean testJavaDuration(String input)
    {
        try
        {
            java.time.Duration.parse(input);
        }
        catch (DateTimeParseException exc)
        {
            // Not Java parsable
            if (input.length() > 3 && !input.contains("W"))
            {
                System.err.println("Java not happy with " + input + ": " + exc.getMessage());
                return true;
            }
        }
        return false;
    }
}