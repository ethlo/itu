package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2026 Morten Haraldsen @ethlo
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
import static org.assertj.core.api.Assertions.fail;

import java.time.DateTimeException;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * The contract of the zero-allocation parser, stated as a check: for any text and window, the char[] path must
 * produce exactly what the String path produces for {@code new String(chars, offset, length)} - the same values,
 * the same consumed length, or the same exception type, message and error index.
 * <p>
 * Shared by the corpus test and the fuzz test so that the two cannot drift in what they consider "the same".
 */
public final class CharArrayDifferential
{
    /**
     * Characters placed on both sides of the window. They are chosen so that reading past either edge would change
     * the outcome: a digit before, and a valid continuation after.
     */
    private static final String JUNK_BEFORE = "9-1T:.+Z";
    private static final String JUNK_AFTER = "Z+01:00.123";

    private CharArrayDifferential()
    {
    }

    public static void assertSameAsStringPath(final String input, final ParseConfig config)
    {
        // The window is the whole array
        assertSameAsStringPath(input, input.toCharArray(), 0, input.length(), config);

        // The window is embedded in an array with a sentinel on each side
        final String padded = JUNK_BEFORE + input + JUNK_AFTER;
        assertSameAsStringPath(input, padded.toCharArray(), JUNK_BEFORE.length(), input.length(), config);
    }

    private static void assertSameAsStringPath(final String input, final char[] chars, final int offset, final int length, final ParseConfig config)
    {
        final char[] snapshot = chars.clone();
        final MutableDateTimeBuffer buffer = new MutableDateTimeBuffer();
        final Object stringOutcome = stringPath(input, config);
        final Object charArrayOutcome = charArrayPath(chars, offset, length, config, buffer);

        assertThat(chars).overridingErrorMessage("Input array must not be modified: %s", input).isEqualTo(snapshot);

        if (stringOutcome instanceof DateTime)
        {
            if (!(charArrayOutcome instanceof Integer))
            {
                fail("String path parsed '%s' as %s, but char[] path failed with %s", input, stringOutcome, charArrayOutcome);
            }
            final DateTime expected = (DateTime) stringOutcome;
            assertThat(buffer.getMostGranularField()).isEqualTo(expected.getMostGranularField());
            assertThat(buffer.getYear()).isEqualTo(expected.getYear());
            assertThat(buffer.getMonth()).isEqualTo(expected.getMonth());
            assertThat(buffer.getDayOfMonth()).isEqualTo(expected.getDayOfMonth());
            assertThat(buffer.getHour()).isEqualTo(expected.getHour());
            assertThat(buffer.getMinute()).isEqualTo(expected.getMinute());
            assertThat(buffer.getSecond()).isEqualTo(expected.getSecond());
            assertThat(buffer.getNano()).isEqualTo(expected.getNano());
            assertThat(buffer.getFractionDigits()).isEqualTo(expected.getFractionDigits());
            assertThat(buffer.hasOffset()).isEqualTo(expected.getOffset().isPresent());
            if (buffer.hasOffset())
            {
                assertThat(buffer.getOffsetTotalSeconds()).isEqualTo(expected.getOffset().get().getTotalSeconds());
            }
            assertThat(buffer.getParseLength()).isEqualTo(expected.getParseLength());
            assertThat((int) (Integer) charArrayOutcome).isEqualTo(expected.getParseLength());
            assertThat(buffer.toDateTime()).isEqualTo(expected);
            assertThat(buffer.toString()).isEqualTo(expected.toString());
            assertThat(buffer.toEpochSecond()).isEqualTo(expected.toInstant().getEpochSecond());
            assertThat(buffer.toEpochMilli()).isEqualTo(expected.toInstant().toEpochMilli());
        }
        else
        {
            if (!(charArrayOutcome instanceof Throwable))
            {
                fail("String path failed on '%s' with %s, but char[] path succeeded with %s", input, stringOutcome, buffer);
            }
            final Throwable expected = (Throwable) stringOutcome;
            final Throwable actual = (Throwable) charArrayOutcome;
            assertThat(actual).overridingErrorMessage("Different exception for '%s': String path %s, char[] path %s", input, expected, actual)
                    .isInstanceOf(expected.getClass())
                    .hasMessage(expected.getMessage());
            if (expected instanceof DateTimeParseException)
            {
                assertThat(((DateTimeParseException) actual).getErrorIndex()).isEqualTo(((DateTimeParseException) expected).getErrorIndex());
                assertThat(((DateTimeParseException) actual).getParsedString()).isEqualTo(input);
            }
            if (expected instanceof LeapSecondException)
            {
                assertThat(((LeapSecondException) actual).getNearestDateTime()).isEqualTo(((LeapSecondException) expected).getNearestDateTime());
                assertThat(((LeapSecondException) actual).getSecondsInMinute()).isEqualTo(((LeapSecondException) expected).getSecondsInMinute());
                assertThat(((LeapSecondException) actual).isVerifiedValidLeapYearMonth()).isEqualTo(((LeapSecondException) expected).isVerifiedValidLeapYearMonth());
            }
        }
    }

    private static Object stringPath(final String input, final ParseConfig config)
    {
        try
        {
            return ITU.parseLenient(input, config);
        }
        catch (DateTimeException exc)
        {
            return exc;
        }
    }

    private static Object charArrayPath(final char[] chars, final int offset, final int length, final ParseConfig config, final MutableDateTimeBuffer buffer)
    {
        try
        {
            return ITU.parseLenient(chars, offset, length, config, buffer);
        }
        catch (DateTimeException exc)
        {
            return exc;
        }
    }

    public static String describe(final char[] chars, final int offset, final int length)
    {
        return "'" + new String(chars, offset, length) + "' in " + Arrays.toString(chars);
    }
}
