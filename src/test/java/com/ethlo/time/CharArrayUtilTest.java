package com.ethlo.time;

import java.util.Arrays;

import org.junit.Test;
import static org.fest.assertions.api.Assertions.assertThat;

public class CharArrayUtilTest
{
    @Test
    public void testConvertWithoutTable()
    {
        final char[] expected = new char[]{'0','1','2','3','4','5','6'};
        final int value = 123456;
        final char[] buf = new char[16];
        LimitedCharArrayIntegerUtil.toString(value, buf, 0, 7);
        assertThat(Arrays.copyOf(buf, 7)).isEqualTo(expected);
    }
    
    @Test
    public void testConvertWithTable()
    {
        final char[] expected = new char[]{'0', '0', '1','2','3','4'};
        final int value = 1234;
        final char[] buf = new char[16];
        LimitedCharArrayIntegerUtil.toString(value, buf, 0, 6);
        assertThat(Arrays.copyOf(buf, 6)).isEqualTo(expected);
    }
}
