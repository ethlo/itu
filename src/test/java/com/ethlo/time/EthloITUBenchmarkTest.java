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

public class EthloITUBenchmarkTest extends BenchmarkTest
{
    @Override
    protected Rfc3339 getParser()
    {
        return new EthloITU();
    }

    @Override
    protected Rfc3339Formatter getFormatter()
    {
        return new EthloITU();
    }

    @Override
    protected Chronograph getChronograph()
    {
        return null;
    }

    @Override
    protected long getRuns()
    {
        return 100_000_000;
    }
}
