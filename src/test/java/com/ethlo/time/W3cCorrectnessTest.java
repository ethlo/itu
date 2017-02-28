package com.ethlo.time;

import static org.fest.assertions.api.Assertions.assertThat;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(CorrectnessTest.class)
public class W3cCorrectnessTest extends AbstractTest<InternetDateTimeUtil>
{
    private W3cDateTimeUtil w3cDateUtil;

    @Test
    public void testParseEmptyString()
    {
        final String s = "";
        final OffsetDateTime date = instance.parse(s);
        assertThat(date).isNull();
    }
    
    @Test
    public void testParseYearString()
    {
        final String s = "2012";
        final Year date = w3cDateUtil.parseLenient(s, Year.class);
        assertThat(date.getValue()).isEqualTo(2012);
    }
    
    @Test
    public void testParseYearMonthString()
    {
        final String s = "2012-10";
        final YearMonth date = w3cDateUtil.parseLenient(s, YearMonth.class);
        assertThat(date.getYear()).isEqualTo(2012);
        assertThat(date.getMonthValue()).isEqualTo(10);
    }
    
    @Test
    public void testParseDateString()
    {
        final String s = "2012-03-29";
        final LocalDate date = w3cDateUtil.parseLenient(s, LocalDate.class);
        assertThat(instance.formatUtc(OffsetDateTime.of(date, LocalTime.MIN, ZoneOffset.UTC))).isEqualTo("2012-03-29T00:00:00Z");
    }
    
    @Test(expected=DateTimeException.class)
    public void testParseBestEffort1DigitMinute()
    {
        final String s = "2012-03-29T23:1";
        final Temporal date = w3cDateUtil.parseLenient(s);
        assertThat(date.get(ChronoField.YEAR)).isEqualTo(2012);
        assertThat(date.get(ChronoField.MONTH_OF_YEAR)).isEqualTo(3);
        assertThat(date.get(ChronoField.DAY_OF_MONTH)).isEqualTo(29);
        assertThat(date.get(ChronoField.HOUR_OF_DAY)).isEqualTo(23);
        assertThat(date.get(ChronoField.MINUTE_OF_HOUR)).isEqualTo(19);
    }
    
    @Test
    public void testParseNull()
    {
        final String s = null;
        final OffsetDateTime date = instance.parse(s);
        assertThat(date).isNull();
    }

    @Override
    protected InternetDateTimeUtil getInstance()
    {
        final FastInternetDateTimeUtil retVal = new FastInternetDateTimeUtil();
        this.w3cDateUtil = retVal; 
        return retVal;
    }

    @Override
    protected long getRuns()
    {
        return 1;
    }
}
