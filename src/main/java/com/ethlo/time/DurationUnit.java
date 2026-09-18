package com.ethlo.time;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2025 Morten Haraldsen @ethlo
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

/**
 * The units a {@link Duration} can be rendered with, largest first.
 * <p>
 * Used to cap the largest unit {@link Duration#normalized(DurationUnit)} is allowed to emit. Anything above
 * the cap stays folded into it, so <code>PT833H20M0.117392763S</code> and <code>P4W6DT17H20M0.117392763S</code>
 * are the same duration rendered with a cap of {@link #HOURS} and {@link #WEEKS} respectively.
 * <p>
 * Years and months are deliberately absent, for the reasons given in
 * {@link com.ethlo.time.internal.ItuDurationParser}: their length depends on the date they are applied to,
 * so they cannot express an exact duration.
 */
public enum DurationUnit
{
    /**
     * Weeks (<code>W</code>). The default, and the most compact rendering.
     * <p>
     * NOTE: <code>java.time.Duration.parse</code> does not accept the week designator, so output rendered with
     * this cap cannot be read back by the Java Time API once the duration reaches a full week. Use
     * {@link #DAYS} or smaller where that matters.
     */
    WEEKS,

    /**
     * Days (<code>D</code>). The largest unit that <code>java.time.Duration.parse</code> accepts.
     */
    DAYS,

    /**
     * Hours (<code>H</code>). Matches how <code>java.time.Duration.toString</code> decomposes a duration.
     */
    HOURS,

    /**
     * Minutes (<code>M</code> in the time section).
     */
    MINUTES,

    /**
     * Seconds (<code>S</code>), including any fractional part. Renders the duration as a single
     * seconds value, such as <code>PT3000000.117392763S</code>.
     */
    SECONDS
}
