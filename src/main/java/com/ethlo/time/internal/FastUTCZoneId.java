package com.ethlo.time.internal;

/*-
 * #%L
 * Internet Time Utility
 * %%
 * Copyright (C) 2017 - 2022 Morten Haraldsen (ethlo)
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

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesProvider;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Set;

public class FastUTCZoneId
{
    private static final String customZoneId = "ITU-UTC";
    private static final ZoneId delegate;

    static
    {
        // The heart of the magic: the ZoneRulesProvider
        final ZoneRulesProvider customProvider = new ZoneRulesProvider()
        {
            @Override
            protected Set<String> provideZoneIds()
            {
                return Collections.singleton(customZoneId);
            }

            @Override
            protected NavigableMap<String, ZoneRules> provideVersions(String zoneId)
            {
                return Collections.emptyNavigableMap();
            }

            @Override
            protected ZoneRules provideRules(String zoneId, boolean forCaching)
            {
                return ZoneOffset.UTC.getRules();
            }
        };

        // Registering the ZoneRulesProvider is the key to ZoneId using it
        ZoneRulesProvider.registerProvider(customProvider);
        delegate = ZoneId.of(customZoneId);
    }

    public static ZoneId get()
    {
        return delegate;
    }
}
