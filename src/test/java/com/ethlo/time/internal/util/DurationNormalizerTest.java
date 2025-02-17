package com.ethlo.time.internal.util;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2025 Morten Haraldsen @ethlo
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ethlo.time.Duration;

class DurationNormalizerTest
{
    @Test
    void testFractionsOnlyNano()
    {
        Assertions.assertThat(new Duration(0, 5).normalized()).isEqualTo("PT0.000000005S");
    }

    @Test
    void testFractionsOnlyNanosNegative()
    {
        Assertions.assertThat(new Duration(-1, 999_999_995).normalized()).isEqualTo("-PT0.000000005S");
    }

    @Test
    void testFractionsOnlyMillis()
    {
        Assertions.assertThat(new Duration(0, 1_000_000).normalized()).isEqualTo("PT0.001S");
    }

    @Test
    void testFractionsOnlyMillisNegative()
    {
        Assertions.assertThat(new Duration(-1, 999_000_000).normalized()).isEqualTo("-PT0.001S");
    }
}
