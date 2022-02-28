package com.ethlo.time;

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
