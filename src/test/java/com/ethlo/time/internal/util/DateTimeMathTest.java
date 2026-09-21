package com.ethlo.time.internal.util;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2026 Morten Haraldsen @ethlo
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * The day-count arithmetic is checked over its whole domain, not by samples: the leap rule has four periods
 * (4, 100, 400 years, and the month lengths inside a year), and a constant that is off by one only shows at
 * the edges of one of them. Every day of 0000-01-01 to 9999-12-31 is 3 652 425 cases, which runs in well under
 * a second.
 */
class DateTimeMathTest
{
    private static final long EPOCH_DAY_0000 = LocalDate.of(0, 1, 1).toEpochDay();

    private static final int DAYS = (int) (LocalDate.of(9999, 12, 31).toEpochDay() - EPOCH_DAY_0000) + 1;

    @Test
    void civilFromDaysMatchesJavaTimeForEveryDay()
    {
        for (int day = 0; day < DAYS; day++)
        {
            final int packed = DateTimeMath.civilFromDaysSince0000(day);
            final LocalDate expected = LocalDate.ofEpochDay(EPOCH_DAY_0000 + day);
            // One comparison per day: a mismatch names the day, and the loop stays cheap
            final int expectedPacked = expected.getYear() << 9 | expected.getMonthValue() << 5 | expected.getDayOfMonth();
            if (packed != expectedPacked)
            {
                assertEquals(expected.toString(), unpack(packed), "day " + day + " since 0000-01-01");
            }
        }
    }

    @Test
    void daysFromCivilMatchesJavaTimeForEveryDay()
    {
        for (int day = 0; day < DAYS; day++)
        {
            final LocalDate date = LocalDate.ofEpochDay(EPOCH_DAY_0000 + day);
            final long actual = DateTimeMath.daysFromCivil(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            if (actual != date.toEpochDay())
            {
                assertEquals(date.toEpochDay(), actual, date.toString());
            }
        }
    }

    @Test
    void timeOfDayMatchesDivisionForEverySecond()
    {
        for (int secondOfDay = 0; secondOfDay < 86_400; secondOfDay++)
        {
            final int hour = DateTimeMath.hourOfDay(secondOfDay);
            final int minute = DateTimeMath.minuteOfHour(secondOfDay);
            final int second = DateTimeMath.secondOfMinute(secondOfDay);
            if (hour != secondOfDay / 3_600 || minute != secondOfDay / 60 % 60 || second != secondOfDay % 60)
            {
                assertEquals(String.format("%02d:%02d:%02d", secondOfDay / 3_600, secondOfDay / 60 % 60, secondOfDay % 60), String.format("%02d:%02d:%02d", hour, minute, second), "second " + secondOfDay);
            }
        }
    }

    @Test
    void daysSince0000EpochIsJavaTimes()
    {
        assertEquals(-EPOCH_DAY_0000, DateTimeMath.DAYS_0000_TO_1970);
    }

    private static String unpack(final int packed)
    {
        return String.format("%04d-%02d-%02d", DateTimeMath.packedYear(packed), DateTimeMath.packedMonth(packed), DateTimeMath.packedDay(packed));
    }
}
