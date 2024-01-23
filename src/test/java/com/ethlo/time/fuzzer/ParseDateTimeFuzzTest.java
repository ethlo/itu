package com.ethlo.time.fuzzer;

import java.time.DateTimeException;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.ethlo.time.ITU;

public class ParseDateTimeFuzzTest
{
    @com.code_intelligence.jazzer.junit.FuzzTest(maxDuration = "2m")
    void parse(FuzzedDataProvider data)
    {
        try
        {
            ITU.parseDateTime(data.consumeRemainingAsAsciiString());
        }
        catch (DateTimeException ignored)
        {

        }
    }
}