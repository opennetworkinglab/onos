/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.onosproject.core.ApplicationId;
import org.onosproject.app.ApplicationIdStore;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * ApplicationIdStore implementation on top of {@code AtomicCounter}
 * and {@code ConsistentMap} primitives.
 */
@Component(immediate = true)
@Service
public class DistributedApplicationIdStore implements ApplicationIdStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private AtomicCounter appIdCounter;
    private ConsistentMap<String, ApplicationId> registeredIds;
    private Map<Short, ApplicationId> idToAppIdCache = Maps.newConcurrentMap();
    private MapEventListener<String, ApplicationId> mapEventListener = event -> {
        if (event.type() == MapEvent.Type.INSERT) {
            idToAppIdCache.put(event.newValue().value().id(), event.newValue().value());
        }
    };

    @Activate
    public void activate() {
        appIdCounter = storageService.getAtomicCounter("onos-app-id-counter");

        registeredIds = storageService.<String, ApplicationId>consistentMapBuilder()
                .withName("onos-app-ids")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .withRelaxedReadConsistency()
                .build();

        primeIdToAppIdCache();
        registeredIds.addListener(mapEventListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        registeredIds.removeListener(mapEventListener);
        log.info("Stopped");
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        return ImmutableSet.copyOf(registeredIds.asJavaMap().values());
    }

    @Override
    public ApplicationId getAppId(Short id) {
        if (!idToAppIdCache.containsKey(id)) {
            primeIdToAppIdCache();
        }
        return idToAppIdCache.get(id);
    }

    @Override
    public ApplicationId getAppId(String name) {
        return registeredIds.asJavaMap().get(name);
    }

    @Override
    public ApplicationId registerApplication(String name) {
        ApplicationId exisitingAppId = registeredIds.asJavaMap().get(name);
        if (exisitingAppId == null) {
            ApplicationId newAppId = new DefaultApplicationId((int) appIdCounter.incrementAndGet(), name);
            exisitingAppId = registeredIds.asJavaMap().putIfAbsent(name, newAppId);
            return exisitingAppId == null ? newAppId : exisitingAppId;
        } else {
            return exisitingAppId;
        }
    }

    private void primeIdToAppIdCache() {
        registeredIds.asJavaMap()
                     .values()
                     .forEach(appId -> {
                         idToAppIdCache.putIfAbsent(appId.id(), appId);
                     });
    }
}
