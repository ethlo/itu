package com.ethlo.time;

import static org.fest.assertions.api.Assertions.assertThat;

import java.time.DateTimeException;
import java.time.OffsetDateTime;

import org.junit.Ignore;
import org.junit.Test;

public abstract class CorrectnessTest extends AbstractTest<InternetDateTimeUtil>
{
    private final String[] validFormats =
    { 
      "2017-02-21T15:27:39.123", "2017-02-21T15:27:39.123456", "2017-02-21T15:27:39.123456789", 
      "2017-02-21T15:27:39.123", "2017-02-21T15:27:39.123456", "2017-02-21T15:27:39.123456789"};
    
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
        final String s = "2017-02-21T10:00:00.000+1200";
        final OffsetDateTime date = instance.parse(s);
        assertThat(instance.formatUtc(date)).isEqualTo("2017-02-20T22:00:00.000Z");
    }
    
    @Ignore
    @Test
    public void testFormat4()
    {
        final String s = "2017-02-21T15:00:00.123Z";
        final OffsetDateTime date = instance.parse(s);
        assertThat(instance.formatUtc(date)).isEqualTo("2017-02-21T15:00:00.123Z");
        assertThat(instance.format(date, "CET")).isEqualTo("2017-02-21T16:00:00.123+0100");
        assertThat(instance.format(date, "EST")).isEqualTo("2017-02-21T10:00:00.123-0500");
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
        assertThat(instance.formatUtc(date)).isEqualTo("2017-02-21T02:27:39.123Z");
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
    
    @Override
    protected long getRuns()
    {
        return 1;
    }
}
