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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class CharArrayUtilTest
{
    @Test
    public void testConvertWithoutTable()
    {
        final char[] expected = new char[]{'0', '1', '2', '3', '4', '5', '6'};
        final int value = 123456;
        final char[] buf = new char[16];
        LimitedCharArrayIntegerUtil.toString(value, buf, 0, 7);
        assertThat(Arrays.copyOf(buf, 7)).isEqualTo(expected);
    }

    @Test
    public void testConvertWithTable()
    {
        final char[] expected = new char[]{'0', '0', '1', '2', '3', '4'};
        final int value = 1234;
        final char[] buf = new char[16];
        LimitedCharArrayIntegerUtil.toString(value, buf, 0, 6);
        assertThat(Arrays.copyOf(buf, 6)).isEqualTo(expected);
    }
}
