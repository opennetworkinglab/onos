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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.mapping.DefaultMappingEntry;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingEntry.MappingEntryState;
import org.onosproject.mapping.MappingEvent;
import org.onosproject.mapping.MappingId;
import org.onosproject.mapping.MappingStore;
import org.onosproject.mapping.MappingStoreDelegate;
import org.onosproject.mapping.StoredMappingEntry;
import org.onosproject.net.DeviceId;
import org.onosproject.store.AbstractStore;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.onosproject.mapping.MappingEntry.MappingEntryState.PENDING_ADD;
import static org.onosproject.mapping.MappingEntry.MappingEntryState.PENDING_REMOVE;
import static org.onosproject.mapping.MappingEvent.Type.MAPPING_ADDED;
import static org.onosproject.mapping.MappingEvent.Type.MAPPING_REMOVED;
import static org.onosproject.mapping.MappingEvent.Type.MAPPING_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of mappings using trivial in-memory implementation.
 */
@Component(immediate = true, service = MappingStore.class)
public class SimpleMappingStore
        extends AbstractStore<MappingEvent, MappingStoreDelegate>
        implements MappingStore {

    private final Logger log = getLogger(getClass());

    private static final String UNRECOGNIZED_STORE_MSG = "Unrecognized store type {}";

    private final ConcurrentMap<DeviceId, ConcurrentMap<MappingId,
                  List<StoredMappingEntry>>> mapDbStore = Maps.newConcurrentMap();

    private final ConcurrentMap<DeviceId, ConcurrentMap<MappingId,
                  List<StoredMappingEntry>>> mapCacheStore = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        mapDbStore.clear();
        mapCacheStore.clear();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {

    }

    @Override
    public int getMappingCount(Type type) {
        int sum = 0;

        for (ConcurrentMap<MappingId, List<StoredMappingEntry>> mapDb :
                getMappingStore(type).values()) {
            for (List<StoredMappingEntry> mes : mapDb.values()) {
                sum += mes.size();
            }
        }

        return sum;
    }

    @Override
    public Iterable<MappingEntry> getAllMappingEntries(Type type) {

        List<MappingEntry> entries = Lists.newArrayList();

        for (ConcurrentMap<MappingId, List<StoredMappingEntry>> mapDb :
                getMappingStore(type).values()) {
            for (List<StoredMappingEntry> mes : mapDb.values()) {
                entries.addAll(mes);
            }
        }

        return entries;
    }

    /**
     * Obtains the mapping store for specified device and store type.
     *
     * @param type     mapping store type
     * @param deviceId device identifier
     * @return Map representing Mapping Store of given device and store type
     */
    private ConcurrentMap<MappingId, List<StoredMappingEntry>>
                                     getMappingStoreByDeviceId(Type type,
                                                               DeviceId deviceId) {
        switch (type) {
            case MAP_DATABASE:
                return mapDbStore.computeIfAbsent(deviceId,
                                                    k -> Maps.newConcurrentMap());
            case MAP_CACHE:
                return mapCacheStore.computeIfAbsent(deviceId,
                                                    k -> Maps.newConcurrentMap());
            default:
                log.error(UNRECOGNIZED_STORE_MSG, type);
                return null;
        }
    }

    /**
     * Obtains the mapping store for specified store type.
     *
     * @param type mapping store type
     * @return mapping store
     */
    private ConcurrentMap<DeviceId, ConcurrentMap<MappingId, List<StoredMappingEntry>>>
                                    getMappingStore(Type type) {
        switch (type) {
            case MAP_DATABASE:
                return mapDbStore;
            case MAP_CACHE:
                return mapCacheStore;
            default:
                log.error(UNRECOGNIZED_STORE_MSG, type);
                return null;
        }
    }

    /**
     * Obtains mapping entries for specified device, store type and mapping ID.
     *
     * @param type      mapping store type
     * @param deviceId  device identifier
     * @param mappingId mapping identifier
     * @return a collection of mapping entries
     */
    private List<StoredMappingEntry> getMappingEntriesInternal(Type type,
                                                               DeviceId deviceId,
                                                               MappingId mappingId) {
        final ConcurrentMap<MappingId, List<StoredMappingEntry>>
                            store = getMappingStoreByDeviceId(type, deviceId);
        List<StoredMappingEntry> r = store.get(mappingId);
        if (r == null) {
            final List<StoredMappingEntry> concurrentlyAdded;
            r = new CopyOnWriteArrayList<>();
            concurrentlyAdded = store.putIfAbsent(mappingId, r);
            if (concurrentlyAdded != null) {
                return concurrentlyAdded;
            }
        }
        return r;
    }

    /**
     * Obtains a mapping entry for specified device, store type and mapping.
     *
     * @param type      mapping store type
     * @param deviceId  device identifier
     * @param mapping   mapping identifier
     * @return a mapping entry
     */
    private MappingEntry getMappingEntryInternal(Type type, DeviceId deviceId,
                                                 Mapping mapping) {
        List<StoredMappingEntry> mes =
                        getMappingEntriesInternal(type, deviceId, mapping.id());
        for (StoredMappingEntry me : mes) {
            if (me.equals(mapping)) {
                return me;
            }
        }
        return null;
    }

    @Override
    public MappingEntry getMappingEntry(Type type, Mapping mapping) {
        return getMappingEntryInternal(type, mapping.deviceId(), mapping);
    }

    @Override
    public Iterable<MappingEntry> getMappingEntries(Type type, DeviceId deviceId) {
        // FIXME: a better way is using Java 8 API to return immutable iterables
        return FluentIterable.from(getMappingStoreByDeviceId(type, deviceId).values())
                .transformAndConcat(Collections::unmodifiableList);
    }

    @Override
    public void storeMapping(Type type, MappingEntry mapping) {

        List<StoredMappingEntry> entries =
                getMappingEntriesInternal(type, mapping.deviceId(), mapping.id());

        synchronized (entries) {
            if (!entries.contains(mapping)) {
                StoredMappingEntry entry =
                        new DefaultMappingEntry(mapping, mapping.state());
                entries.add(entry);
            }
        }
    }

    @Override
    public void pendingDeleteMapping(Type type, Mapping mapping) {

        List<StoredMappingEntry> entries =
                getMappingEntriesInternal(type, mapping.deviceId(), mapping.id());

        synchronized (entries) {
            for (StoredMappingEntry entry : entries) {
                if (entry.equals(mapping)) {
                    synchronized (entry) {
                        entry.setState(PENDING_REMOVE);
                    }
                }
            }
        }
    }

    @Override
    public MappingEvent addOrUpdateMappingEntry(Type type, MappingEntry entry) {

        List<StoredMappingEntry> entries =
                    getMappingEntriesInternal(type, entry.deviceId(), entry.id());
        synchronized (entries) {
            for (StoredMappingEntry stored : entries) {
                if (stored.equals(entry)) {
                    if (stored.state() == PENDING_ADD) {
                        stored.setState(MappingEntryState.ADDED);
                        return new MappingEvent(MAPPING_ADDED, entry);
                    }
                    return new MappingEvent(MAPPING_UPDATED, entry);
                }
            }
        }

        log.error("Mapping was not found in store {} to update", entry);

        return null;
    }

    @Override
    public MappingEvent removeMapping(Type type, Mapping mapping) {

        List<StoredMappingEntry> entries =
                getMappingEntriesInternal(type, mapping.deviceId(), mapping.id());
        synchronized (entries) {
            if (entries.remove(mapping)) {
                return new MappingEvent(MAPPING_REMOVED, mapping);
            }
        }
        return null;
    }

    @Override
    public MappingEvent pendingMappingEntry(Type type, MappingEntry entry) {
        List<StoredMappingEntry> entries =
                getMappingEntriesInternal(type, entry.deviceId(), entry.id());
        synchronized (entries) {
            for (StoredMappingEntry stored : entries) {
                if (stored.equals(entry) && (stored.state() != PENDING_ADD)) {
                    synchronized (stored) {
                        stored.setState(PENDING_ADD);
                        return new MappingEvent(MAPPING_UPDATED, entry);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void purgeMappingEntry(Type type, DeviceId deviceId) {
        getMappingStore(type).remove(deviceId);
    }

    @Override
    public void purgeMappingEntries(Type type) {
        getMappingStore(type).clear();
    }
}


