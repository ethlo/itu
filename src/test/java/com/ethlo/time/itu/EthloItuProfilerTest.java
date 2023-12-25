package com.ethlo.time.itu;

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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.ethlo.time.internal.EthloITU;

public class EthloItuProfilerTest
{
    private static final EthloITU ethloItu = EthloITU.getInstance();

    @Test
    void profileParsing()
    {
        final int runs = 1_000_000_000;
        for (int i = 0; i < runs; i++)
        {
            ethloItu.parse("2017-12-21T12:20:45.987654321Z");
        }
    }
}
