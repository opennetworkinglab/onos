/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.trivial;

import com.google.common.collect.ImmutableSet;
import org.onosproject.app.ApplicationIdStore;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.osgi.service.component.annotations.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple implementation of the application ID registry using in-memory
 * structures.
 */
@Component(immediate = true, service = ApplicationIdStore.class)
public class SimpleApplicationIdStore implements ApplicationIdStore {

    private static final AtomicInteger ID_DISPENSER = new AtomicInteger(1);

    private final Map<Short, DefaultApplicationId> appIds = new ConcurrentHashMap<>();
    private final Map<String, DefaultApplicationId> appIdsByName = new ConcurrentHashMap<>();

    @Override
    public Set<ApplicationId> getAppIds() {
        return ImmutableSet.copyOf(appIds.values());
    }

    @Override
    public ApplicationId getAppId(Short id) {
        return appIds.get(id);
    }

    @Override
    public ApplicationId getAppId(String name) {
        return appIdsByName.get(name);
    }

    @Override
    public ApplicationId registerApplication(String name) {
        DefaultApplicationId appId = appIdsByName.get(name);
        if (appId == null) {
            short id = (short) ID_DISPENSER.getAndIncrement();
            appId = new DefaultApplicationId(id, name);
            appIds.put(id, appId);
            appIdsByName.put(name, appId);
        }
        return appId;
    }

}
