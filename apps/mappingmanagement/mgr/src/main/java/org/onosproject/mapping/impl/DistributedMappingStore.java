/*
 * Copyright 2017-present Open Networking Laboratory
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
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.mapping.DefaultMapping;
import org.onosproject.mapping.DefaultMappingEntry;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingEvent;
import org.onosproject.mapping.MappingId;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingStore;
import org.onosproject.mapping.MappingStoreDelegate;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a distributed store for managing mapping information.
 */
@Component(immediate = true)
@Service
public class DistributedMappingStore
        extends AbstractStore<MappingEvent, MappingStoreDelegate>
        implements MappingStore {

    private final Logger log = getLogger(getClass());

    private ConsistentMap<MappingId, Mapping> database;
    private ConsistentMap<MappingId, Mapping> cache;

    private Map<MappingId, Mapping> databaseMap;
    private Map<MappingId, Mapping> cacheMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private final MapEventListener<MappingId, Mapping> listener = new InternalListener();

    @Activate
    public void activate() {

        Serializer serializer = Serializer.using(KryoNamespaces.API,
                                                    Mapping.class,
                                                    DefaultMapping.class,
                                                    MappingId.class,
                                                    MappingEvent.Type.class,
                                                    MappingKey.class,
                                                    MappingValue.class,
                                                    MappingAddress.class,
                                                    MappingAddress.Type.class,
                                                    MappingAction.class,
                                                    MappingAction.Type.class,
                                                    MappingTreatment.class,
                                                    MappingInstruction.class,
                                                    MappingInstruction.Type.class);

        database = storageService.<MappingId, Mapping>consistentMapBuilder()
                        .withName("onos-mapping-database")
                        .withSerializer(serializer)
                        .build();

        cache = storageService.<MappingId, Mapping>consistentMapBuilder()
                        .withName("onos-mapping-cache")
                        .withSerializer(serializer)
                        .build();

        database.addListener(listener);
        cache.addListener(listener);
        databaseMap = database.asJavaMap();
        cacheMap = cache.asJavaMap();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        database.removeListener(listener);
        cache.removeListener(listener);
        log.info("Stopped");
    }

    /**
     * Obtains map representation of mapping store.
     *
     * @param type mapping store type
     * @return map representation of mapping store
     */
    private Map<MappingId, Mapping> getStoreMap(Type type) {
        switch (type) {
            case MAP_DATABASE:
                return databaseMap;
            case MAP_CACHE:
                return cacheMap;
            default:
                log.warn("Unrecognized map store type {}", type);
                return Maps.newConcurrentMap();
        }
    }

    /**
     * Obtains mapping store.
     *
     * @param type mapping store type
     * @return mapping store
     */
    private ConsistentMap<MappingId, Mapping> getStore(Type type) {
        switch (type) {
            case MAP_DATABASE:
                return database;
            case MAP_CACHE:
                return cache;
            default:
                throw new IllegalArgumentException("Wrong mapping store " + type);
        }
    }

    @Override
    public int getMappingCount(Type type) {
        AtomicInteger sum = new AtomicInteger(0);
        deviceService.getDevices().forEach(device ->
                sum.addAndGet(Iterables.size(getMappingEntries(type, device.id()))));
        return sum.get();
    }

    @Override
    public Iterable<MappingEntry> getAllMappingEntries(Type type) {

        Map<MappingId, Mapping> storeMap = getStoreMap(type);
        return ImmutableList.copyOf(storeMap.values().stream()
                                            .map(DefaultMappingEntry::new)
                                            .collect(Collectors.toList()));
    }

    @Override
    public MappingEntry getMappingEntry(Type type, Mapping mapping) {

        return new DefaultMappingEntry(getStoreMap(type).get(mapping.id()));
    }

    @Override
    public Iterable<MappingEntry> getMappingEntries(Type type, DeviceId deviceId) {

        Map<MappingId, Mapping> storeMap = getStoreMap(type);
        return ImmutableList.copyOf(storeMap.values().stream()
                                             .filter(m -> m.deviceId() == deviceId)
                                             .map(DefaultMappingEntry::new)
                                             .collect(Collectors.toList()));
    }

    @Override
    public void storeMapping(Type type, MappingEntry mapping) {

        getStore(type).put(mapping.id(), mapping);
    }

    @Override
    public MappingEvent removeMapping(Type type, Mapping mapping) {

        getStore(type).remove(mapping.id());
        return null;
    }

    @Override
    public void pendingDeleteMapping(Type type, Mapping mapping) {
        // TODO: this will be implemented when management plane is ready
        log.error("This method will be available when management plane is ready");
    }

    @Override
    public MappingEvent addOrUpdateMappingEntry(Type type, MappingEntry entry) {
        // TODO: this will be implemented when management plane is ready
        log.error("This method will be available when management plane is ready");
        return null;
    }

    @Override
    public MappingEvent pendingMappingEntry(Type type, MappingEntry entry) {
        // TODO: this will be implemented when management plane is ready
        log.error("This method will be available when management plane is ready");
        return null;
    }

    @Override
    public void purgeMappingEntries(Type type) {
        getStore(type).clear();
    }

    /**
     * Event listener to notify delegates about mapping events.
     */
    private class InternalListener implements MapEventListener<MappingId, Mapping> {

        @Override
        public void event(MapEvent<MappingId, Mapping> event) {
            final MappingEvent.Type type;
            final Mapping mapping;

            switch (event.type()) {
                case INSERT:
                    type = MappingEvent.Type.MAPPING_ADDED;
                    mapping = event.newValue().value();
                    break;
                case UPDATE:
                    type = MappingEvent.Type.MAPPING_UPDATED;
                    mapping = event.newValue().value();
                    break;
                case REMOVE:
                    type = MappingEvent.Type.MAPPING_REMOVED;
                    mapping = event.oldValue().value();
                    break;
                default:
                    throw new IllegalArgumentException("Wrong event type " + event.type());
            }
            notifyDelegate(new MappingEvent(type, mapping));
        }
    }
}
