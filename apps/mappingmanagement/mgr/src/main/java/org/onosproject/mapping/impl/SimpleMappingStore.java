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

import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Modified;
import org.onosproject.mapping.MappingEvent;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingId;
import org.onosproject.mapping.MappingStore;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.store.AbstractStore;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of mappings using trivial in-memory implementation.
 */
@Component(immediate = true)
@Service
public class SimpleMappingStore
        extends AbstractStore<MappingEvent, MappingStoreDelegate>
        implements MappingStore {

    private final Logger log = getLogger(getClass());

    private final ConcurrentMap<DeviceId, ConcurrentMap<MappingId, List<MappingEntry>>>
            mapDbStore = Maps.newConcurrentMap();

    private final ConcurrentMap<DeviceId, ConcurrentMap<MappingId, List<MappingEntry>>>
            mapCacheStore = Maps.newConcurrentMap();

    private final AtomicInteger localBatchIdGen = new AtomicInteger();

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
        return 0;
    }

    @Override
    public MappingEntry getMappingEntry(Type type, Mapping mapping) {
        return null;
    }

    @Override
    public Iterable<MappingEntry> getMappingEntries(Type type, DeviceId deviceId) {
        return null;
    }

    @Override
    public void deleteMapping(Type type, Mapping mapping) {

    }

    @Override
    public MappingEvent addOrUpdateMappingEntry(Type type, MappingEntry entry) {
        return null;
    }

    @Override
    public MappingEvent removeMappingEntry(Type type, MappingEntry entry) {
        return null;
    }

    @Override
    public MappingEvent pendingMappingEntry(Type type, MappingEntry entry) {
        return null;
    }

    @Override
    public void purgeMappingEntries(Type type) {

    }
}


