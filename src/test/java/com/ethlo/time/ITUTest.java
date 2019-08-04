package com.ethlo.time;

import static org.fest.assertions.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Date;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(CorrectnessTest.class)
public class ITUTest
{
    private static final OffsetDateTime VALID_DATETIME = OffsetDateTime.parse("2017-05-01T16:23:12Z");

    @Test
    public void parseDateTime()
    {
        assertThat(ITU.parseDateTime(VALID_DATETIME.toString())).isNotNull();
    }

    @Test
    public void formatUtc()
    {
        assertThat(ITU.formatUtc(new Date())).isNotNull();
    }

    @Test
    public void testFormatUtc()
    {
        assertThat(ITU.formatUtc(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void isValid()
    {
        assertThat(ITU.isValid("2017-asddsd")).isFalse();
    }

    @Test
    public void formatUtcMilli()
    {
        assertThat(ITU.formatUtcMilli(new Date())).isNotNull();
    }

    @Test
    public void formatUtcMicro()
    {
        assertThat(ITU.formatUtcMicro(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void formatUtcNano()
    {
        assertThat(ITU.formatUtcNano(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void testFormatUtcMilli()
    {
        assertThat(ITU.formatUtcMilli(VALID_DATETIME)).isNotNull();
    }

    @Test
    public void testFormat()
    {
        assertThat(ITU.format(new Date(), "GMT")).isNotNull();
    }

    @Test
    public void parseLenient()
    {
        assertThat(ITU.parseLenient("2017-01-31")).isNotNull();
    }

    @Test
    public void parseLenient2()
    {
        assertThat(ITU.parseLenient("2017-01-31", YearMonth.class)).isNotNull();
    }
    
    @Test
    public void toEpochMillis()
    {
        assertThat(ITU.toEpochMillis(VALID_DATETIME));
    }
}