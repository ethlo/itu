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

import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractTest
{
    protected Rfc3339Parser parser;
    protected Rfc3339Formatter formatter;

    protected abstract Rfc3339Parser getParser();

    protected abstract Rfc3339Formatter getFormatter();

    protected abstract long getRuns();

    @BeforeEach
    public void setup()
    {
        this.parser = getParser();
        this.formatter = getFormatter();
    }

    protected final void unsupported(final Chronograph chronograph, String msg)
    {
        chronograph.timed(msg + " - unsupported!", () -> {
        });
    }

    protected final void perform(final Runnable func, final String msg)
    {
        final Chronograph c = Chronograph.create();
        c.timed(msg, () ->
        {
            for (int i = 0; i < getRuns(); i++)
            {
                func.run();
            }
        });

        final double ns = c.getTotalTime().toNanos();
        final double opsSec = (ns / getRuns());
        final double nsPerOp = getRuns() * 1_000_000_000 / ns;
        System.out.printf("%s - %.2f ns (%s ops/sec)%n", msg, opsSec, new DecimalFormat("###,###,###").format(nsPerOp));
    }
}
