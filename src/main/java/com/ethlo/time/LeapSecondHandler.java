package com.ethlo.time;

import java.time.YearMonth;

public interface LeapSecondHandler
{
    int LEAP_SECOND_SECONDS = 60;

    boolean isValidLeapSecondDate(YearMonth needle);

    YearMonth getLastKnownLeapSecond();
}
