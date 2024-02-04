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
import static com.ethlo.time.token.DateTimeTokens.digits;
import static com.ethlo.time.token.DateTimeTokens.fractions;
import static com.ethlo.time.token.DateTimeTokens.separators;

public class DateTimeParsers
{
    private static final ConfigurableDateTimeParser DATE = (ConfigurableDateTimeParser) DateTimeParsers.of(
            digits(YEAR, 4),
            separators('-'),
            digits(MONTH, 2),
            separators('-'),
            digits(DAY, 2)
    );

    public static DateTimeParser of(DateTimeToken... tokens)
    {
        return ConfigurableDateTimeParser.of(tokens);
    }

    private static final DateTimeParser LOCAL_TIME = of(
            digits(HOUR, 2),
            separators(':'),
            digits(MINUTE, 2),
            separators(':'),
            digits(SECOND, 2),
            fractions()
    );

    public static DateTimeParser localDate()
    {
        return DATE;
    }

    public static DateTimeParser localTime()
    {
        return LOCAL_TIME;
    }
}
