package com.ethlo.time;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Date;

import org.junit.Test;

import com.ethlo.time.InternetDateTimeUtil.FractionType;

public class InternetDateTimeUtilTest
{
    @Test(expected=IllegalArgumentException.class)
    public void testFormat1()
    {
        final String s = "2017-02-21T15:27:39.0000000";
        new InternetDateTimeUtil().parse(s);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testFormat2()
    {
        final String s = "2017-02-21T15:27:39.000+30:00";
        new InternetDateTimeUtil().parse(s);
    }
    
    @Test
    public void testFormat3()
    {
        final String s = "2017-02-21T10:00:00.000+1200";
        final Date date = new InternetDateTimeUtil().parse(s);
        assertThat(new InternetDateTimeUtil().formatUtc(date)).isEqualTo("2017-02-20T22:00:00.000+0000");
    }
    
    @Test
    public void testFormat4()
    {
        final String s = "2017-02-21T15:00:00.123Z";
        final Date date = new InternetDateTimeUtil().parse(s);
        assertThat(new InternetDateTimeUtil().formatUtc(date)).isEqualTo("2017-02-21T15:00:00.123+0000");
        assertThat(new InternetDateTimeUtil().format(date, "CET")).isEqualTo("2017-02-21T16:00:00.123+0100");
        assertThat(new InternetDateTimeUtil().format(date, "EST")).isEqualTo("2017-02-21T10:00:00.123-0500");
    }
    
    @Test
    public void testFormat5()
    {
        final String s = "2017-02-21T15:27:39.123+23:00";
        final Date date = new InternetDateTimeUtil().parse(s);
        assertThat(new InternetDateTimeUtil().formatUtc(date)).isEqualTo("2017-02-20T16:27:39.123+0000");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testParseMicrosecondsWithMillisecondConfig()
    {
        new InternetDateTimeUtil(FractionType.MILLISECONDS).parse("2017-02-21T15:27:39.0000000Z");
    }
    
    @Test
    public void testParseEmptyString()
    {
        final String s = "";
        final Date date = new InternetDateTimeUtil().parse(s);
        assertThat(date).isNull();;
    }
    
    @Test
    public void testParseNull()
    {
        final String s = null;
        final Date date = new InternetDateTimeUtil().parse(s);
        assertThat(date).isNull();
    }
    
    @Test
    public void testRfcExample()
    {
        // 1994-11-05T08:15:30-05:00 corresponds to November 5, 1994, 8:15:30 am, US Eastern Standard Time/
        // 1994-11-05T13:15:30Z corresponds to the same instant.
        final String a = "1994-11-05T08:15:30-05:00";
        final String b = "1994-11-05T13:15:30Z";
        final Date dA = new InternetDateTimeUtil().parse(a);
        final Date dB = new InternetDateTimeUtil().parse(b);
        assertThat(dA.getTime()).isEqualTo(dB.getTime());
    }
}
