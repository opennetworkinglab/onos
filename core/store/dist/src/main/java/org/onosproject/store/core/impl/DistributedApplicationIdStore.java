/*
 * Copyright 2014 Open Networking Laboratory
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

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.putIfAbsent;

import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.MapEvent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationIdStore;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.store.hz.AbstractHazelcastStore;
import org.onosproject.store.hz.SMap;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onlab.util.KryoNamespace;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of the application ID registry using in-memory
 * structures.
 */
@Component(immediate = true)
@Service
public class DistributedApplicationIdStore
        extends AbstractHazelcastStore<AppIdEvent, AppIdStoreDelegate>
        implements ApplicationIdStore {

    protected IAtomicLong lastAppId;
    protected SMap<String, DefaultApplicationId> appIdsByName;

    protected Map<Short, DefaultApplicationId> appIds = new ConcurrentHashMap<>();

    private String listenerId;


    @Override
    @Activate
    public void activate() {
        super.activate();

        this.serializer = new KryoSerializer() {
            @Override
            protected void setupKryoPool() {
                serializerPool = KryoNamespace.newBuilder()
                        .register(KryoNamespaces.API)
                        .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                        .build();
            }
        };

        lastAppId = theInstance.getAtomicLong("applicationId");

        appIdsByName = new SMap<>(theInstance.<byte[], byte[]>getMap("appIdsByName"), this.serializer);
        listenerId = appIdsByName.addEntryListener((new RemoteAppIdEventHandler()), true);

        primeAppIds();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        appIdsByName.removeEntryListener(listenerId);
        log.info("Stopped");
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        return ImmutableSet.<ApplicationId>copyOf(appIds.values());
    }

    @Override
    public ApplicationId getAppId(Short id) {
        ApplicationId appId = appIds.get(id);
        if (appId == null) {
            primeAppIds();
        }
        return appId;
    }

    private void primeAppIds() {
        for (DefaultApplicationId appId : appIdsByName.values()) {
            appIds.put(appId.id(), appId);
        }
    }

    @Override
    public ApplicationId registerApplication(String name) {
        DefaultApplicationId appId = appIdsByName.get(name);
        if (appId == null) {
            short id = (short) lastAppId.getAndIncrement();
            appId = putIfAbsent(appIdsByName, name,
                                new DefaultApplicationId(id, name));
        }
        return appId;
    }

    private class RemoteAppIdEventHandler implements EntryListener<String, DefaultApplicationId> {
        @Override
        public void entryAdded(EntryEvent<String, DefaultApplicationId> event) {
            DefaultApplicationId appId = event.getValue();
            appIds.put(appId.id(), appId);
        }

        @Override
        public void entryRemoved(EntryEvent<String, DefaultApplicationId> event) {
        }

        @Override
        public void entryUpdated(EntryEvent<String, DefaultApplicationId> event) {
            entryAdded(event);
        }

        @Override
        public void entryEvicted(EntryEvent<String, DefaultApplicationId> event) {
        }

        @Override
        public void mapEvicted(MapEvent event) {
        }

        @Override
        public void mapCleared(MapEvent event) {
        }
    }
}
