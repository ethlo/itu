package com.ethlo.time;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(CorrectnessTest.class)
public class FieldTest
{
    @Test
    public void testGetKnownFields()
    {
        final Field year = Field.valueOf(Year.class);
        final Field month = Field.valueOf(YearMonth.class);
        final Field day = Field.valueOf(LocalDate.class);
        final Field second = Field.valueOf(OffsetDateTime.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnknown()
    {
        Field.valueOf(Temporal.class);
    }
}
