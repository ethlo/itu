package com.ethlo.time.internal;

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

import java.util.Arrays;

public final class LimitedCharArrayIntegerUtil
{
    public static final char DIGIT_9 = '9';
    public static final char ZERO = '0';
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final int TABLE_WIDTH = 4;
    private static final int RADIX = 10;
    private static final int MAX_INT_WIDTH = 10;
    private static final int TABLE_SIZE = (int) Math.pow(RADIX, TABLE_WIDTH);
    private static final char[] INT_CONVERSION_CACHE = new char[(TABLE_SIZE * TABLE_WIDTH) + MAX_INT_WIDTH];

    static
    {
        int offset = 0;
        for (int i = 0; i < TABLE_SIZE; i++)
        {
            createBufferEntry(INT_CONVERSION_CACHE, offset, TABLE_WIDTH, i);
            offset += TABLE_WIDTH;
        }
    }

    private LimitedCharArrayIntegerUtil()
    {
    }

    public static int parsePositiveInt(final String strNum, int startInclusive, int endExclusive)
    {
        int result = 0;
        try
        {
            for (int i = startInclusive; i < endExclusive; i++)
            {
                final char c = strNum.charAt(i);
                if (c < ZERO || c > DIGIT_9)
                {
                    ErrorUtil.raiseUnexpectedCharacter(strNum, i);
                }
                result = (result * 10) + (c - ZERO);
            }
        }
        catch (StringIndexOutOfBoundsException exc)
        {
            ErrorUtil.raiseUnexpectedEndOfText(strNum, startInclusive);
        }

        return result;
    }

    public static void toString(final int value, final char[] buf, final int offset, final int charLength)
    {
        if (value < TABLE_SIZE)
        {
            final int length = Math.min(TABLE_WIDTH, charLength);
            final int padPrefixLen = charLength - length;
            final int start = charLength > TABLE_WIDTH ? TABLE_WIDTH : TABLE_WIDTH - charLength;
            final int targetOffset = offset + padPrefixLen;
            final int srcPos = (value * TABLE_WIDTH) + (charLength < TABLE_WIDTH ? start : 0);
            copy(INT_CONVERSION_CACHE, srcPos, buf, targetOffset, length);
            if (padPrefixLen > 0)
            {
                zeroFill(buf, offset, padPrefixLen);
            }
        }
        else
        {
            createBufferEntry(buf, offset, charLength, value);
        }
    }

    private static void createBufferEntry(char[] buf, int offset, int charLength, int value)
    {
        int charPos = offset + MAX_INT_WIDTH;
        value = -value;
        int div;
        int rem;
        while (value <= -10)
        {
            div = value / 10;
            rem = -(value - 10 * div);
            buf[charPos--] = DIGITS[rem];
            value = div;
        }
        buf[charPos] = DIGITS[-value];

        int l = ((MAX_INT_WIDTH + offset) - charPos) + 1;
        while (l < charLength)
        {
            buf[--charPos] = ZERO;
            l++;
        }
        final int srcPos = charPos;
        copy(buf, srcPos, offset, charLength);
    }

    private static void zeroFill(char[] buf, int offset, int padPrefixLen)
    {
        Arrays.fill(buf, offset, offset + padPrefixLen, ZERO);
    }

    private static void copy(char[] buf, int srcPos, int offset, int length)
    {
        copy(buf, srcPos, buf, offset, length);
    }

    private static void copy(char[] buf, int srcPos, char[] target, int offset, int length)
    {
        System.arraycopy(buf, srcPos, target, offset, length);
    }
}
