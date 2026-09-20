package com.ethlo.time.internal.fixed;

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

import static com.ethlo.time.internal.fixed.ITUParser.DATE_SEPARATOR;
import static com.ethlo.time.internal.fixed.ITUParser.FRACTION_SEPARATOR;
import static com.ethlo.time.internal.fixed.ITUParser.MAX_FRACTION_DIGITS;
import static com.ethlo.time.internal.fixed.ITUParser.MINUS;
import static com.ethlo.time.internal.fixed.ITUParser.PLUS;
import static com.ethlo.time.internal.fixed.ITUParser.SEPARATOR_LOWER;
import static com.ethlo.time.internal.fixed.ITUParser.SEPARATOR_SPACE;
import static com.ethlo.time.internal.fixed.ITUParser.SEPARATOR_UPPER;
import static com.ethlo.time.internal.fixed.ITUParser.TIME_SEPARATOR;
import static com.ethlo.time.internal.fixed.ITUParser.ZULU_LOWER;
import static com.ethlo.time.internal.fixed.ITUParser.ZULU_UPPER;
import static com.ethlo.time.internal.util.LimitedCharArrayIntegerUtil.ZERO;

import com.ethlo.time.internal.util.DateTimeValidator;

/**
 * The yes/no of {@link ITUParser#parseDateTime(String, int)} without the parse: {@code isValid(text)} is true exactly
 * when {@code parseDateTime(text, 0)} returns, and false whenever it would throw a {@code DateTimeException}.
 * <p>
 * Parsing and catching costs 800+ ns per invalid input (a formatted message and a stack trace), and a validator is
 * called on invalid input by design. This walk returns false instead, so both answers cost about the same as the
 * parse itself. It accepts precisely what the strict parser accepts with {@code ParseConfig.DEFAULT}: the
 * fixed-width prefix, an optional 1-9 digit fraction, a mandatory offset, nothing after it, and the field ranges of
 * {@link DateTimeValidator} and {@link com.ethlo.time.TimezoneOffset#ofHoursMinutes}; second 60 is never valid because
 * it is either a leap second (which java.time cannot represent) or out of range. The corpus and fuzz tests hold it to
 * the parser, input by input.
 */
public final class ITUValidator
{
    /**
     * The shortest valid input, {@code YYYY-MM-DDTHH:MM:SSZ}
     */
    private static final int MIN_LENGTH = 20;
    private static final int MAX_OFFSET_HOURS = 18;
    private static final int MAX_OFFSET_MINUTES = MAX_OFFSET_HOURS * 60;

    private ITUValidator()
    {
    }

    public static boolean isValid(final String text)
    {
        final int length = text.length();
        if (length < MIN_LENGTH)
        {
            return false;
        }

        // The fixed-width prefix: digits and separators at known positions, then the field ranges
        final int year = digits4(text, 0);
        final int month = digits2(text, 5);
        final int day = digits2(text, 8);
        final int hour = digits2(text, 11);
        final int minute = digits2(text, 14);
        final int second = digits2(text, 17);
        if (year < 0 || month < 0 || day < 0 || hour < 0 || minute < 0 || second < 0)
        {
            return false;
        }
        if (text.charAt(4) != DATE_SEPARATOR || text.charAt(7) != DATE_SEPARATOR || text.charAt(13) != TIME_SEPARATOR || text.charAt(16) != TIME_SEPARATOR)
        {
            return false;
        }
        final char dateTimeSeparator = text.charAt(10);
        if (dateTimeSeparator != SEPARATOR_UPPER && dateTimeSeparator != SEPARATOR_LOWER && dateTimeSeparator != SEPARATOR_SPACE)
        {
            return false;
        }
        if (!DateTimeValidator.isValidDate(year, month, day) || hour > 23 || minute > 59 || second > 59)
        {
            return false;
        }

        // Optional fraction: 1-9 digits, and it cannot end the text since the offset is mandatory
        int idx = 19;
        if (text.charAt(idx) == FRACTION_SEPARATOR)
        {
            idx = 20;
            while (idx < length && (text.charAt(idx) ^ ZERO) <= 9)
            {
                idx++;
            }
            final int fractionDigits = idx - 20;
            if (fractionDigits < 1 || fractionDigits > MAX_FRACTION_DIGITS || idx == length)
            {
                return false;
            }
        }

        // Offset: 'Z', or a sign and HH:MM within +/-18:00, where "-00:00" (RFC 3339 unknown local offset) is rejected
        final char c = text.charAt(idx);
        if (c == ZULU_UPPER || c == ZULU_LOWER)
        {
            return idx + 1 == length;
        }
        if ((c != PLUS && c != MINUS) || idx + 6 != length || text.charAt(idx + 3) != TIME_SEPARATOR)
        {
            return false;
        }
        final int offsetHours = digits2(text, idx + 1);
        final int offsetMinutes = digits2(text, idx + 4);
        if (offsetHours < 0 || offsetMinutes < 0 || offsetMinutes > 59 || offsetHours * 60 + offsetMinutes > MAX_OFFSET_MINUTES)
        {
            return false;
        }
        return c == PLUS || (offsetHours | offsetMinutes) != 0;
    }

    /**
     * @return The value of the two digits at {@code start}, or -1 if either is not a digit. The caller guarantees
     * {@code start + 2 <= text.length()}. See {@code LimitedCharArrayIntegerUtil.parse2} for the {@code c ^ '0'} digit test
     */
    private static int digits2(final String text, final int start)
    {
        final int d0 = text.charAt(start) ^ ZERO;
        final int d1 = text.charAt(start + 1) ^ ZERO;
        if (d0 <= 9 && d1 <= 9)
        {
            return d0 * 10 + d1;
        }
        return -1;
    }

    private static int digits4(final String text, final int start)
    {
        final int d0 = text.charAt(start) ^ ZERO;
        final int d1 = text.charAt(start + 1) ^ ZERO;
        final int d2 = text.charAt(start + 2) ^ ZERO;
        final int d3 = text.charAt(start + 3) ^ ZERO;
        if (d0 <= 9 && d1 <= 9 && d2 <= 9 && d3 <= 9)
        {
            return d0 * 1000 + d1 * 100 + d2 * 10 + d3;
        }
        return -1;
    }
}
