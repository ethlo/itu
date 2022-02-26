package com.ethlo.time;

import org.junit.jupiter.api.Disabled;

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
@Disabled("Not returning correct results")
public class Java7Rfc3339BenchmarkTest extends BenchmarkTest
{
    @Override
    protected Rfc3339 getParser()
    {
        return new Java7Rfc3339();
    }

    @Override
    protected Rfc3339Formatter getFormatter()
    {
        return new Java7Rfc3339();
    }
}
