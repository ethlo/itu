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

import com.ethlo.time.CorrectnessTest;
import com.ethlo.time.EthloITU;
import com.ethlo.time.Rfc3339;
import com.ethlo.time.Rfc3339Formatter;

public class EthloITUCorrectnessTest extends CorrectnessTest
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
}
