package com.ethlo.time;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TimezoneOffsetTest
{
    final TimezoneOffset a = TimezoneOffset.ofHoursMinutes(10, 30);
    final TimezoneOffset b = TimezoneOffset.ofHoursMinutes(10, 30);
    final TimezoneOffset c = TimezoneOffset.ofHoursMinutes(-8, -15);


    @Test
    void of()
    {
        assertThat(TimezoneOffset.of(ZoneOffset.ofHoursMinutes(10, 30))).isEqualTo(a);
    }

    @Test
    void getHours()
    {
        assertThat(a.getHours()).isEqualTo(10);
    }

    @Test
    void getMinutes()
    {
        assertThat(a.getMinutes()).isEqualTo(30);
    }

    @Test
    void getTotalSeconds()
    {
        assertThat(a.getTotalSeconds()).isEqualTo(37800);
    }

    @Test
    void asJavaTimeOffset()
    {
        assertThat(a.asJavaTimeOffset()).isEqualTo(ZoneOffset.ofHoursMinutes(10, 30));
    }

    @Test
    void testEquals()
    {
        assertThat(a).isNotEqualTo("");
        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void testToString()
    {
        assertThat(a.toString()).isEqualTo("TimezoneOffset{hours=10, minutes=30}");
    }

    @Test
    void testHashcode()
    {
        assertThat(a.hashCode()).isEqualTo(1301);
    }
}