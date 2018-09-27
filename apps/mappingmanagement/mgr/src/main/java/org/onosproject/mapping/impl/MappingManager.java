/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.mapping.MappingAdminService;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingEvent;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingListener;
import org.onosproject.mapping.MappingProvider;
import org.onosproject.mapping.MappingProviderRegistry;
import org.onosproject.mapping.MappingProviderService;
import org.onosproject.mapping.MappingService;
import org.onosproject.mapping.MappingStore;
import org.onosproject.mapping.MappingStore.Type;
import org.onosproject.mapping.MappingStoreDelegate;
import org.onosproject.mapping.MappingValue;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of mapping management service.
 */
@Component(immediate = true,
           service = { MappingService.class, MappingAdminService.class, MappingProviderRegistry.class })
public class MappingManager
        extends AbstractListenerProviderRegistry<MappingEvent, MappingListener,
                                                 MappingProvider, MappingProviderService>
        implements MappingService, MappingAdminService, MappingProviderRegistry {

    private final Logger log = getLogger(getClass());

    private static final String MAPPING_OP_TOPIC = "mapping-ops-ids";
    private final MappingStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MappingStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(MappingEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(MappingEvent.class);
        log.info("Stopped");
    }

    @Override
    public int getMappingCount(Type type) {
        return store.getMappingCount(type);
    }

    @Override
    public Iterable<MappingEntry> getAllMappingEntries(Type type) {
        return store.getAllMappingEntries(type);
    }

    @Override
    public void storeMappingEntry(Type type, MappingEntry entry) {
        store.storeMapping(type, entry);
    }

    @Override
    public Iterable<MappingEntry> getMappingEntries(Type type, DeviceId deviceId) {
        return store.getMappingEntries(type, deviceId);
    }

    @Override
    public Iterable<MappingEntry> getMappingEntriesByAppId(Type type, ApplicationId appId) {

        Set<MappingEntry> mappingEntries = Sets.newHashSet();
        for (Device d : deviceService.getDevices()) {
            for (MappingEntry mappingEntry : store.getMappingEntries(type, d.id())) {
                if (mappingEntry.appId() == appId.id()) {
                    mappingEntries.add(mappingEntry);
                }
            }
        }
        return mappingEntries;
    }

    @Override
    public void removeMappingEntries(Type type, MappingEntry... mappingEntries) {
        for (MappingEntry entry : mappingEntries) {
            store.removeMapping(type, entry);
        }
    }

    @Override
    public void removeMappingEntriesByAppId(Type type, ApplicationId appId) {
        removeMappingEntries(type, Iterables.toArray(
                    getMappingEntriesByAppId(type, appId), MappingEntry.class));
    }

    @Override
    public void purgeMappings(Type type, DeviceId deviceId) {
        store.purgeMappingEntry(type, deviceId);
    }

    @Override
    protected MappingProviderService createProviderService(MappingProvider provider) {
        return new InternalMappingProviderService(provider);
    }

    /**
     * Obtains a mapping value associated a mapping key.
     *
     * @param mappingKey a given mapping key
     * @return a mapping value
     */
    private MappingValue getMappingValueByMappingKey(MappingKey mappingKey) {
        for (MappingEntry entry : store.getAllMappingEntries(Type.MAP_DATABASE)) {
            if (entry.key().equals(mappingKey)) {
                return entry.value();
            }
        }
        return null;
    }

    /**
     * Store delegate.
     */
    private class InternalStoreDelegate implements MappingStoreDelegate {

        @Override
        public void notify(MappingEvent event) {
            post(event);
        }
    }

    /**
     * Internal mapping provider service.
     */
    private class InternalMappingProviderService
            extends AbstractProviderService<MappingProvider> implements MappingProviderService {

        /**
         * Initializes internal mapping provider service.
         *
         * @param provider mapping provider
         */
        protected InternalMappingProviderService(MappingProvider provider) {
            super(provider);
        }

        @Override
        public void mappingAdded(MappingEntry mappingEntry, Type type) {
            storeMappingEntry(type, mappingEntry);
        }

        @Override
        public MappingValue mappingQueried(MappingKey mappingKey) {
            return getMappingValueByMappingKey(mappingKey);
        }

        @Override
        public List<MappingValue> mappingQueried(List<MappingKey> mappingKeys) {
            List<MappingValue> values = Lists.newArrayList();
            mappingKeys.forEach(key -> {
                MappingValue value = getMappingValueByMappingKey(key);
                if (value != null) {
                    values.add(value);
                }
            });
            return ImmutableList.copyOf(values);
        }
    }
}
