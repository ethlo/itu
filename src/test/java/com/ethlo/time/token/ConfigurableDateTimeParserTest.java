package com.ethlo.time.token;

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

import static com.ethlo.time.Field.DAY;
import static com.ethlo.time.Field.HOUR;
import static com.ethlo.time.Field.MINUTE;
import static com.ethlo.time.Field.MONTH;
import static com.ethlo.time.Field.SECOND;
import static com.ethlo.time.Field.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParsePosition;

import org.junit.jupiter.api.Test;

import com.ethlo.time.DateTime;
import com.ethlo.time.ITU;

public class ConfigurableDateTimeParserTest
{
    @Test
    void parseCustomFormat()
    {
        final ParsePosition pos = new ParsePosition(0);
        final String input = "31-12-2000 235937";
        final DateTimeParser parser = new ConfigurableDateTimeParser(
                new TwoDigitToken(DAY),
                new SeparatorToken('-'),
                new TwoDigitToken(MONTH),
                new SeparatorToken('-'),
                new FourDigitToken(YEAR),
                new SeparatorToken(' '),
                new TwoDigitToken(HOUR),
                new TwoDigitToken(MINUTE),
                new TwoDigitToken(SECOND)
        );
        final DateTime result = parser.parse(input, pos);
        assertThat(result).isEqualTo(DateTime.of(2000, 12, 31, 23, 59, 37, null));
    }

    @Test
    void parseRfc3339Format()
    {
        final String input = "2023-01-01T23:38:34.987654321+06:00";
        final DateTime fixed = ITU.parseLenient(input);
        final ParsePosition pos = new ParsePosition(0);
        final DateTime custom = DateTimeParsers.rfc3339().parse(input, pos);
        assertThat(custom).isEqualTo(fixed);
        assertThat(fixed.toString()).isEqualTo(input);
        assertThat(custom.toString()).isEqualTo(input);
    }
}
