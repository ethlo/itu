package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2022 Morten Haraldsen (ethlo)
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class DefaultLeapSecondHandler implements LeapSecondHandler
{
    public static final String LEAP_SECOND_PATH_CSV = "leap_second_dates.csv";
    private final SortedSet<YearMonth> leapSecondMonths;
    private final YearMonth lastLeapKnown;

    public DefaultLeapSecondHandler()
    {
        leapSecondMonths = new TreeSet<>();

        try (final InputStream in = DefaultLeapSecondHandler.class.getClassLoader().getResourceAsStream(LEAP_SECOND_PATH_CSV);
             final BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in, LEAP_SECOND_PATH_CSV + " was not found on the classpath"), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (!line.isEmpty())
                {
                    leapSecondMonths.add(YearMonth.parse(line));
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        lastLeapKnown = leapSecondMonths.last();
    }

    @Override
    public boolean isValidLeapSecondDate(YearMonth needle)
    {
        return leapSecondMonths.contains(needle);
    }

    @Override
    public YearMonth getLastKnownLeapSecond()
    {
        return lastLeapKnown;
    }
}
