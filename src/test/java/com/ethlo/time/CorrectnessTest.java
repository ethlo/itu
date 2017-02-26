package com.ethlo.time;

import static org.fest.assertions.api.Assertions.assertThat;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;

public abstract class CorrectnessTest extends AbstractTest<InternetDateTimeUtil>
{
    private final String[] validFormats =
    { 
        "2017-02-21T15:27:39Z", "2017-02-21T15:27:39.123Z", 
        "2017-02-21T15:27:39.123456Z", "2017-02-21T15:27:39.123456789Z", 
        "2017-02-21T15:27:39+00:00", "2017-02-21T15:27:39.123+00:00", 
        "2017-02-21T15:27:39.123456+00:00", "2017-02-21T15:27:39.123456789+00:00",
        "2017-02-21T15:27:39.1+00:00", "2017-02-21T15:27:39.12+00:00",
        "2017-02-21T15:27:39.123+00:00", "2017-02-21T15:27:39.1234+00:00",
        "2017-02-21T15:27:39.112345+00:00", "2017-02-21T15:27:39.123456+00:00",
        "2017-02-21T15:27:39.1234567+00:00", "2017-02-21T15:27:39.12345678+00:00"
    };
    
    private final String[] invalidFormats = {
            "2017-02-21T15:27:39", "2017-02-21T15:27:39.123", 
            "2017-02-21T15:27:39.123456", "2017-02-21T15:27:39.123456789",
            "2017-02-21T15:27:39+0000", "2017-02-21T15:27:39.123+0000", 
            "201702-21T15:27:39.123456+0000", "20170221T15:27:39.123456789+0000"};
    
    @Test(expected=DateTimeException.class)
    public void testFormat1()
    {
        final String s = "2017-02-21T15:27:39.0000000";
        instance.parse(s);
    }
    
    @Test(expected=DateTimeException.class)
    public void testFormat2()
    {
        final String s = "2017-02-21T15:27:39.000+30:00";
        instance.parse(s);
    }
    
    @Test
    public void testFormat3()
    {
        final String s = "2017-02-21T10:00:00.000+12:00";
        final OffsetDateTime date = instance.parse(s);
        assertThat(instance.formatUtcMilli(date)).isEqualTo("2017-02-20T22:00:00.000Z");
    }
    
    @Ignore
    @Test
    public void testFormat4()
    {
        final String s = "2017-02-21T15:00:00.123Z";
        final OffsetDateTime date = instance.parse(s);
        assertThat(instance.formatUtc(date)).isEqualTo("2017-02-21T15:00:00.123Z");
        assertThat(instance.format(date, "CET")).isEqualTo("2017-02-21T16:00:00.123+01:00");
        assertThat(instance.format(date, "EST")).isEqualTo("2017-02-21T10:00:00.123-05:00");
    }
    
    @Test
    public void testFormatUtc()
    {
        final String s = "2017-02-21T15:09:03.123456789Z";
        final OffsetDateTime date = instance.parse(s);
        final String expected = "2017-02-21T15:09:03Z";
        final String actual = instance.formatUtc(date);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void testFormatUtcMilli()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = instance.parse(s);
        assertThat(instance.formatUtcMilli(date)).isEqualTo("2017-02-21T15:00:00.123Z");
    }
    
    @Test
    public void testFormatUtcMicro()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = instance.parse(s);
        assertThat(instance.formatUtcMicro(date)).isEqualTo("2017-02-21T15:00:00.123456Z");
    }
    
    @Test
    public void testFormatUtcNano()
    {
        final String s = "2017-02-21T15:00:00.987654321Z";
        final OffsetDateTime date = instance.parse(s);
        assertThat(instance.formatUtcNano(date)).isEqualTo(s);
    }

    @Test(expected=DateTimeException.class)
    public void testFormat4TrailingNoise()
    {
        final String s = "2017-02-21T15:00:00.123ZGGG";
        instance.parse(s);
    }
    
    @Test
    public void testFormat5()
    {
        final String s = "2017-02-21T15:27:39.123+13:00";
        final OffsetDateTime date = instance.parse(s);
        assertThat(instance.formatUtcMilli(date)).isEqualTo("2017-02-21T02:27:39.123Z");
    }
        
    @Test
    public void testParseEmptyString()
    {
        final String s = "";
        final OffsetDateTime date = instance.parse(s);
        assertThat(date).isNull();
    }
    
    @Test
    public void testParseNull()
    {
        final String s = null;
        final OffsetDateTime date = instance.parse(s);
        assertThat(date).isNull();
    }
    
    @Test
    public void testRfcExample()
    {
        // 1994-11-05T08:15:30-05:00 corresponds to November 5, 1994, 8:15:30 am, US Eastern Standard Time/
        // 1994-11-05T13:15:30Z corresponds to the same instant.
        final String a = "1994-11-05T08:15:30-05:00";
        final String b = "1994-11-05T13:15:30Z";
        final OffsetDateTime dA = instance.parse(a);
        final OffsetDateTime dB = instance.parse(b);
        assertThat(instance.formatUtc(dA)).isEqualTo(instance.formatUtc(dB));
    }
    
    @Test
    public void testValid()
    {
        for (String f : this.validFormats)
        {
            assertThat(instance.isValid(f)).isTrue();
        }
    }
    
    @Test
    public void testInvalid()
    {
        for (String f : this.invalidFormats)
        {
            if (instance.isValid(f))
            {
                throw new DateTimeException(f + " is deemed valid");
            }
        }
    }
    
    @Test
    public void testMilitaryOffset()
    {
        final String s = "2017-02-21T15:27:39+0000";
        assertThat(instance.isValid(s)).isFalse();
    }
    
    @Test
    public void testFormatWithNamedTimeZone()
    {
        // TODO: Add assertions
        instance.format(new Date(), "EST");
    }
    
    @Override
    protected long getRuns()
    {
        return 1;
    }
}
