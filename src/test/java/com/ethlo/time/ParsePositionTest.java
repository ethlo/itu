package com.ethlo.time;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParsePosition;

import org.junit.jupiter.api.Test;

/**
 * Argument validation of the ParsePosition overloads. What they parse, and where they leave the position, is in
 * {@code date-time-corpus.json} (the entries with an {@code offset}).
 */
public class ParsePositionTest
{
    @Test
    void testParseOutOfBoundsPosition()
    {
        final ParsePosition pos = new ParsePosition(40);
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseDateTime("123", pos));
    }

    @Test
    void testParseOutOfBoundsPositionNegative()
    {
        final ParsePosition pos = new ParsePosition(-3);
        assertThrows(IndexOutOfBoundsException.class, () -> ITU.parseDateTime("123", pos));
    }
}
