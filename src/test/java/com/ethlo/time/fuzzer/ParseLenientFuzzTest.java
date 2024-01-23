package com.ethlo.time.fuzzer;

import java.time.DateTimeException;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.ethlo.time.DateTime;
import com.ethlo.time.ITU;

public class ParseLenientFuzzTest
{
    @com.code_intelligence.jazzer.junit.FuzzTest(maxDuration = "2m")
    void parse(FuzzedDataProvider data)
    {
        DateTime d = null;
        try
        {
            d = ITU.parseLenient(data.consumeRemainingAsString());
        }
        catch (DateTimeException ignored)
        {

        }

        if (d != null)
        {
            d.toInstant();
            System.out.println(d);
            d.toString();
        }
    }
}