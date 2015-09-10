/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.core.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Set;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationIdStore;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * ApplicationIdStore implementation on top of {@code AtomicCounter}
 * and {@code ConsistentMap} primitives.
 */
@Component(immediate = true, enabled = true)
@Service
public class ConsistentApplicationIdStore implements ApplicationIdStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private AtomicCounter appIdCounter;
    private ConsistentMap<String, ApplicationId> registeredIds;
    private Map<String, ApplicationId> nameToAppIdCache = Maps.newConcurrentMap();
    private Map<Short, ApplicationId> idToAppIdCache = Maps.newConcurrentMap();

    private static final Serializer SERIALIZER = Serializer.using(new KryoNamespace.Builder()
                                                                        .register(KryoNamespaces.API)
                                                                        .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                                                                        .build());

    @Activate
    public void activate() {
        appIdCounter = storageService.atomicCounterBuilder()
                                      .withName("onos-app-id-counter")
                                      .withPartitionsDisabled()
                                      .build();

        registeredIds = storageService.<String, ApplicationId>consistentMapBuilder()
                .withName("onos-app-ids")
                .withPartitionsDisabled()
                .withSerializer(SERIALIZER)
                .build();

        primeAppIds();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        // TODO: Rework this when we have notification support in ConsistentMap.
        primeAppIds();
        return ImmutableSet.copyOf(nameToAppIdCache.values());
    }

    @Override
    public ApplicationId getAppId(Short id) {
        if (!idToAppIdCache.containsKey(id)) {
            primeAppIds();
        }
        return idToAppIdCache.get(id);
    }

    @Override
    public ApplicationId getAppId(String name) {
        ApplicationId appId = nameToAppIdCache.computeIfAbsent(name, key -> {
            Versioned<ApplicationId> existingAppId = registeredIds.get(key);
            return existingAppId != null ? existingAppId.value() : null;
        });
        if (appId != null) {
            idToAppIdCache.putIfAbsent(appId.id(), appId);
        }
        return appId;
    }

    @Override
    public ApplicationId registerApplication(String name) {
        ApplicationId appId = nameToAppIdCache.computeIfAbsent(name, key -> {
            Versioned<ApplicationId> existingAppId = registeredIds.get(name);
            if (existingAppId == null) {
                int id = Tools.retryable(appIdCounter::incrementAndGet, StorageException.class, 1, 2000)
                              .get()
                              .intValue();
                DefaultApplicationId newAppId = new DefaultApplicationId(id, name);
                existingAppId = registeredIds.putIfAbsent(name, newAppId);
                if (existingAppId != null) {
                    return existingAppId.value();
                } else {
                    return newAppId;
                }
            } else {
                return existingAppId.value();
            }
        });
        idToAppIdCache.putIfAbsent(appId.id(), appId);
        return appId;
    }

    private void primeAppIds() {
        registeredIds.values()
                     .stream()
                     .map(Versioned::value)
                     .forEach(appId -> {
                         nameToAppIdCache.putIfAbsent(appId.name(), appId);
                         idToAppIdCache.putIfAbsent(appId.id(), appId);
                     });
    }
}
