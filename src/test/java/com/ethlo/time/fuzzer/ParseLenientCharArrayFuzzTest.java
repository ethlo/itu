package com.ethlo.time.fuzzer;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2026 Morten Haraldsen @ethlo
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

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.ethlo.time.CharArrayDifferential;
import com.ethlo.time.ParseConfig;

/**
 * Differential fuzzing: whatever the input, the zero-allocation char[] path must agree with the String path.
 * <p>
 * NOTE: A run that is stopped with SIGTERM (e.g. wrapped in {@code timeout}) leaves a {@code crash-*} file with
 * the input that was in flight at that moment, without any report. It is not a finding until a replay under
 * {@code mvn test} fails too; the differential check itself has never had one.
 */
public class ParseLenientCharArrayFuzzTest
{
    @FuzzTest(maxDuration = "30m")
    void parse(FuzzedDataProvider data)
    {
        final boolean failOnTrailingJunk = data.consumeBoolean();
        final String text = data.consumeRemainingAsString();
        CharArrayDifferential.assertSameAsStringPath(text, ParseConfig.DEFAULT.withFailOnTrailingJunk(failOnTrailingJunk));
    }
}
