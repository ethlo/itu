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

import java.beans.ConstructorProperties;

public class TestParam
{
    private final String input;
    private final boolean lenient;
    private final String error;
    private final int errorIndex;
    private final String expected;
    private final String note;

    @ConstructorProperties(value = {"input", "lenient", "error", "error_index", "expected", "note"})
    public TestParam(String input, boolean lenient, String error, Integer errorIndex, String expected, String note)
    {
        this.input = input;
        this.lenient = lenient;
        this.error = error;
        this.errorIndex = errorIndex != null ? errorIndex : -1;
        this.expected = expected;
        this.note = note;
    }

    public String getInput()
    {
        return input;
    }

    public boolean isLenient()
    {
        return lenient;
    }

    public String getError()
    {
        return error;
    }

    public int getErrorIndex()
    {
        return errorIndex;
    }

    @Override
    public String toString()
    {
        return "TestParam{" +
                "input='" + input + '\'' +
                ", lenient=" + lenient +
                ", expected=" + expected + '\'' +
                ", error='" + error + '\'' +
                ", errorOffset=" + errorIndex + '\'' +
                ", note=" + note +
                '}';
    }

    public String getNote()
    {
        return note;
    }

    public String getExpected()
    {
        return expected;
    }
}
