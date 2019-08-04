package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.fest.assertions.api.Assertions.assertThat;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(CorrectnessTest.class)
public abstract class CorrectnessTest extends AbstractTest<Rfc3339>
{
    private final String[] validFormats =
    { 
        "2017-02-21T15:27:39Z", "2017-02-21T15:27:39.123Z", 
        "2017-02-21T15:27:39.123456Z", "2017-02-21T15:27:39.123456789Z", 
        "2017-02-21T15:27:39+00:00", "2017-02-21T15:27:39.123+00:00", 
        "2017-02-21T15:27:39.123456+00:00", "2017-02-21T15:27:39.123456789+00:00",
        "2017-02-21T15:27:39.1+00:00", "2017-02-21T15:27:39.12+00:00",
        "2017-02-21T15:27:39.123+00:00", "2017-02-21T15:27:39.1234+00:00",
        "2017-02-21T15:27:39.12345+00:00", "2017-02-21T15:27:39.123456+00:00",
        "2017-02-21T15:27:39.1234567+00:00", "2017-02-21T15:27:39.12345678+00:00",
        "2017-02-21T15:27:39.123456789+00:00"
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
        instance.parseDateTime(s);
    }
    
    @Test(expected=DateTimeException.class)
    public void testFormat2()
    {
        final String s = "2017-02-21T15:27:39.000+30:00";
        instance.parseDateTime(s);
    }
    
    @Test
    public void testFormat3()
    {
        final String s = "2017-02-21T10:00:00.000+12:00";
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(instance.formatUtcMilli(date)).isEqualTo("2017-02-20T22:00:00.000Z");
    }
    
    @Test(expected=DateTimeException.class)
    public void testInvalidNothingAfterFractionalSeconds()
    {
        final String s = "2017-02-21T10:00:00.12345";
        instance.parseDateTime(s);
    }
    
    @Test
    public void testFormat4()
    {
        final String s = "2017-02-21T15:00:00.123Z";
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(instance.formatUtcMilli(date)).isEqualTo("2017-02-21T15:00:00.123Z");
        assertThat(instance.format(Date.from(date.atZoneSameInstant(ZoneOffset.UTC).toInstant()), "CET", 3)).isEqualTo("2017-02-21T16:00:00.123+01:00");
        assertThat(instance.format(new Date(date.toInstant().toEpochMilli()), "EST", 3)).isEqualTo("2017-02-21T10:00:00.123-05:00");
    }
    
    @Test(expected=DateTimeException.class)
    public void testParseMoreThanNanoResolutionFails()
    {
        instance.parseDateTime("2017-02-21T15:00:00.1234567891Z");
    }
    
    @Test(expected=DateTimeException.class)
    public void testFormatMoreThanNanoResolutionFails()
    {
        final OffsetDateTime d = instance.parseDateTime("2017-02-21T15:00:00.123456789Z");
        final int fractionDigits = 10;
        instance.formatUtc(d, fractionDigits);
    }
    
    @Test
    public void testFormatUtc()
    {
        final String s = "2017-02-21T15:09:03.123456789Z";
        final OffsetDateTime date = instance.parseDateTime(s);
        final String expected = "2017-02-21T15:09:03Z";
        final String actual = instance.formatUtc(date);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testFormatUtcMilli()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(instance.formatUtcMilli(date)).isEqualTo("2017-02-21T15:00:00.123Z");
    }
    
    @Test
    public void testFormatUtcMilliWithDate()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(instance.formatUtcMilli(new Date(date.toInstant().toEpochMilli()))).isEqualTo("2017-02-21T15:00:00.123Z");
    }
    
    @Test
    public void testFormatUtcMicro()
    {
        final String s = "2017-02-21T15:00:00.123456789Z";
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(instance.formatUtcMicro(date)).isEqualTo("2017-02-21T15:00:00.123456Z");
    }
    
    @Test
    public void testFormatUtcNano()
    {
        final String s = "2017-02-21T15:00:00.987654321Z";
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(instance.formatUtcNano(date)).isEqualTo(s);
    }

    @Test(expected=DateTimeException.class)
    public void testFormat4TrailingNoise()
    {
        final String s = "2017-02-21T15:00:00.123ZGGG";
        instance.parseDateTime(s);
    }
    
    @Test
    public void testFormat5()
    {
        final String s = "2017-02-21T15:27:39.123+13:00";
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(instance.formatUtcMilli(date)).isEqualTo("2017-02-21T02:27:39.123Z");
    }
        
    @Test
    public void testParseEmptyString()
    {
        final String s = "";
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(date).isNull();
    }
    
    @Test
    public void testParseNull()
    {
        final String s = null;
        final OffsetDateTime date = instance.parseDateTime(s);
        assertThat(date).isNull();
    }
    
    @Test
    public void testRfcExample()
    {
        // 1994-11-05T08:15:30-05:00 corresponds to November 5, 1994, 8:15:30 am, US Eastern Standard Time/
        // 1994-11-05T13:15:30Z corresponds to the same instant.
        final String a = "1994-11-05T08:15:30-05:00";
        final String b = "1994-11-05T13:15:30Z";
        final OffsetDateTime dA = instance.parseDateTime(a);
        final OffsetDateTime dB = instance.parseDateTime(b);
        assertThat(instance.formatUtc(dA)).isEqualTo(instance.formatUtc(dB));
    }
    
    @Test(expected=DateTimeException.class)
    public void testBadSeparator()
    {
        final String a = "1994 11-05T08:15:30-05:00";
        instance.parseDateTime(a);
    }
    
    @Test(expected=DateTimeException.class)
    public void testParseNonDigit()
    {
        final String a = "199g-11-05T08:15:30-05:00";
        instance.parseDateTime(a);
    }


    @Test(expected=DateTimeException.class)
    public void testInvalidDateTimeSeparator()
    {
        final String a = "1994-11-05X08:15:30-05:00";
        instance.parseDateTime(a);
    }
    
    @Test
    public void testLowerCaseTseparator()
    {
        final String a = "1994-11-05t08:15:30z";
        instance.parseDateTime(a);
    }
    
    @Test
    public void testSpaceAsSeparator()
    {
        final String a = "1994-11-05 08:15:30z";
        instance.parseDateTime(a);
    }
    
    @Test
    public void testValid()
    {
        for (String f : this.validFormats)
        {
            assertThat(instance.isValid(f)).overridingErrorMessage("Expecting to be valid <%s>", f).isTrue();
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

    @Test(expected=DateTimeException.class)
    public void testParseUnknownLocalOffsetConvention()
    {
        final String s = "2017-02-21T15:27:39-00:00";
        instance.parseDateTime(s);
    }
    
    @Test
    public void testParseLowercaseZ()
    {
        final String s = "2017-02-21T15:27:39.000z";
        instance.parseDateTime(s);
    }

    @Test
    public void testFormatWithNamedTimeZoneDate()
    {
        final String s = "2017-02-21T15:27:39.321+00:00";
        final OffsetDateTime d = instance.parseDateTime(s);
        final String formatted = instance.format(new Date(d.toInstant().toEpochMilli()), "EST");
        assertThat(formatted).isEqualTo("2017-02-21T10:27:39.321-05:00");
    }
    
    @Test
    public void testFormatUtcDate()
    {
        final String s = "2017-02-21T15:27:39.321+00:00";
        final OffsetDateTime d = instance.parseDateTime(s);
        final String formatted = instance.formatUtc(new Date(d.toInstant().toEpochMilli()));
        assertThat(formatted).isEqualTo("2017-02-21T15:27:39.321Z");
    }
    
    @Test
    public void testFormatWithNamedTimeZone()
    {
        final String s = "2017-02-21T15:27:39.321+00:00";
        final OffsetDateTime d = instance.parseDateTime(s);
        final String formatted = instance.format(new Date(d.toInstant().toEpochMilli()), "EST", 3);
        assertThat(formatted).isEqualTo("2017-02-21T10:27:39.321-05:00");
    }
    
    @Override
    protected long getRuns()
    {
        return 1;
    }
}
