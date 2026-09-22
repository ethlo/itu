package com.ethlo.time.internal.util;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2024 Morten Haraldsen (ethlo)
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

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * The digit writers are checked against {@code String.valueOf}: every two- and four-digit value, and the
 * fraction writer for every digit count over the edges and a random sample of nanosecond values.
 */
class DigitWriterTest
{
    @Test
    void write2AndWrite4CoverTheirRanges()
    {
        final char[] buf = new char[4];
        final byte[] bytes = new byte[4];
        for (int v = 0; v < 100; v++)
        {
            LimitedCharArrayIntegerUtil.write2(buf, 1, v);
            LimitedCharArrayIntegerUtil.write2(bytes, 1, v);
            assertThat(new String(buf, 1, 2)).isEqualTo(String.format("%02d", v));
            assertThat(new String(bytes, 1, 2, StandardCharsets.ISO_8859_1)).isEqualTo(String.format("%02d", v));
        }
        for (int v = 0; v < 10_000; v++)
        {
            LimitedCharArrayIntegerUtil.write4(buf, 0, v);
            LimitedCharArrayIntegerUtil.write4(bytes, 0, v);
            assertThat(new String(buf, 0, 4)).isEqualTo(String.format("%04d", v));
            assertThat(new String(bytes, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo(String.format("%04d", v));
        }
    }

    @Test
    void writeFractionTruncatesToTheDigitCount()
    {
        final char[] buf = new char[10];
        final byte[] bytes = new byte[10];
        final Random random = new Random(7);
        final int[] fixed = {0, 1, 9, 10, 99, 100, 999, 1_000, 999_999, 1_000_000, 100_000_000, 123_456_789, 987_654_321, 999_999_999};
        for (int i = 0; i < 20_000 + fixed.length; i++)
        {
            final int nano = i < fixed.length ? fixed[i] : random.nextInt(1_000_000_000);
            final String nine = String.valueOf(1_000_000_000 + nano).substring(1);
            for (int digits = 1; digits <= 9; digits++)
            {
                LimitedCharArrayIntegerUtil.writeFraction(buf, 1, nano, digits);
                LimitedCharArrayIntegerUtil.writeFraction(bytes, 1, nano, digits);
                assertThat(new String(buf, 1, digits)).as("%d nanos, %d digits", nano, digits).isEqualTo(nine.substring(0, digits));
                assertThat(new String(bytes, 1, digits, StandardCharsets.ISO_8859_1)).as("%d nanos, %d digits, bytes", nano, digits).isEqualTo(nine.substring(0, digits));
            }
        }
    }
}
