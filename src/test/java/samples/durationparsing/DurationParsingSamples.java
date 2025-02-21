package samples.durationparsing;

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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ethlo.time.Duration;
import com.ethlo.time.ITU;

public class DurationParsingSamples
{
    @Test
    void simple()
    {
        final Duration duration = ITU.parseDuration("P4W");
        assertThat(duration.getSeconds()).isEqualTo(2_419_200L);
    }

    @Test
    void fullNotNormalizedToNormalized()
    {
        final Duration duration = ITU.parseDuration("P4W10DT28H122M1.123456S");
        assertThat(duration.normalized()).isEqualTo("P5W4DT6H2M1.123456S");
    }
}
