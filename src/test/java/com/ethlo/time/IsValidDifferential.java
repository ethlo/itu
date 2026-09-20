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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DateTimeException;

/**
 * The contract of {@link ITU#isValid(String)}, stated as a check: for any text it must answer exactly what
 * {@link ITU#parseDateTime(String)} would do - true when it returns, false when it throws.
 * <p>
 * Shared by the corpus test and the fuzz test so that the two cannot drift in what they consider "the same".
 */
public final class IsValidDifferential
{
    private IsValidDifferential()
    {
    }

    public static void assertSameAsParseDateTime(final String text)
    {
        boolean parses;
        try
        {
            ITU.parseDateTime(text);
            parses = true;
        }
        catch (DateTimeException exc)
        {
            parses = false;
        }
        assertThat(ITU.isValid(text)).as("isValid('%s') vs parseDateTime", text).isEqualTo(parses);
    }
}
