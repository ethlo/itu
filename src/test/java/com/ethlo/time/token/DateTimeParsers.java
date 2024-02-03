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

public class DateTimeParsers
{
    private static final ConfigurableDateTimeParser DATE = new ConfigurableDateTimeParser(
            new DigitsToken(YEAR, 4),
            new SeparatorToken('-'),
            new DigitsToken(MONTH, 2),
            new SeparatorToken('-'),
            new DigitsToken(DAY, 2)
    );

    private static final ConfigurableDateTimeParser MINUTES = DATE.combine(
            new SeparatorToken('T'),
            new DigitsToken(HOUR, 2),
            new SeparatorToken(':'),
            new DigitsToken(MINUTE, 2)
    );

    private static final ConfigurableDateTimeParser LOCAL_TIME = MINUTES.combine(
            new SeparatorToken(':'),
            new DigitsToken(SECOND, 2)
    );

    private static final ConfigurableDateTimeParser FRACTIONAL_SECONDS_LOCAL = LOCAL_TIME.combine(
            new SeparatorToken('.'),
            new FractionsToken()
    );

    private static final ConfigurableDateTimeParser FRACTIONAL_SECONDS_OFFSET = FRACTIONAL_SECONDS_LOCAL.combine(
            new TimeZoneOffsetToken()
    );

    public static DateTimeParser rfc3339()
    {
        return FRACTIONAL_SECONDS_OFFSET;
    }

    public static DateTimeParser minutes()
    {
        return MINUTES;
    }

    public static DateTimeParser seconds()
    {
        return LOCAL_TIME;
    }

    public static DateTimeParser fractionalSeconds()
    {
        return FRACTIONAL_SECONDS_LOCAL;
    }
}
