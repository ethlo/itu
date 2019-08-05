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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

import org.junit.Before;

public abstract class AbstractTest<T>
{
    protected T instance;

    protected abstract T getInstance();
    
    protected abstract long getRuns();
    
    @Before
    public void setup()
    {
        this.instance = getInstance();
    }
    
    protected final void perform(Consumer<Void> func, String msg)
    {
        // Warm-up
        for (int i = 0; i < getRuns(); i++)
        {
            func.accept(null);
        }
        
        // Benchmark
        final long start = System.nanoTime();
        for (int i = 0; i < getRuns(); i++)
        {
            func.accept(null);
        }
        final long end = System.nanoTime();
        final double secs = (end - start) / 1_000_000_000D;
        System.out.println(msg + ": " + getElapsedFormatter().format(secs) + " sec elapsed. " + getPerformanceFormatter().format((getRuns() / secs))  + " iterations/sec. " + getRuns() + " total iterations");
    }
    
    protected DecimalFormat getPerformanceFormatter()
    {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        formatter.setMaximumFractionDigits(0);
        formatter.setDecimalFormatSymbols(symbols);
        return formatter;
    }
    
    protected DecimalFormat getElapsedFormatter()
    {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        formatter.setMaximumFractionDigits(2);
        formatter.setDecimalFormatSymbols(symbols);
        return formatter;
    }
}
