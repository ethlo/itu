package com.ethlo.time;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ErrorOffsetTest
{
    @ParameterizedTest
    @ValueSource(strings = {"111",
            "1111-",
            "2012-2",
            "2012-11-",
            "2012-11-1",
            "2012-11-11x",
            "2012-11-11t12",
            "2012-11-11t12:",
            "2012-11-11t12:22",
            "2012-11-11t12:22:",
            "2012-11-11t12:22:1",
            "2012-11-11t12:22:11",
            "2012-11-11t12:22:11.",
            "2012-11-11t12:22:11y",
            "2012-11-11t12:22:11.1234567890",
            "2012-11-11t12:22:11.1234567890+",
            "2012-11-11t12:22:11.1234567890+8",
            "2012-11-11t12:22:11.1234567890+08:",
            "2012-11-11t12:22:11.1234567890+08:1",
            "2012-11-11t12:22:11.1234567890+08:11x"})
    public void testParseUseErrorPosition(String arg)
    {
        // Compare that parser error-position with the JDK parser
        final DateTimeParseException exc = assertThrows(DateTimeParseException.class, () -> ITU.parseDateTime(arg));
        final DateTimeParseException original = assertThrows(DateTimeParseException.class, () -> OffsetDateTime.parse(arg));
        assertThat(exc.getErrorIndex()).isEqualTo(original.getErrorIndex());
    }

}
