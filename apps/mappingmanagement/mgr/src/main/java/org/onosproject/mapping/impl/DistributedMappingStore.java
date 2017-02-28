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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingEvent;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingStore;
import org.onosproject.mapping.MappingStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.store.AbstractStore;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

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

    @Activate
    public void activate(ComponentContext context) {
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        log.info("Stopped");
    }

    @Override
    public void setDelegate(MappingStoreDelegate delegate) {

    }

    @Override
    public void unsetDelegate(MappingStoreDelegate delegate) {

    }

    @Override
    public boolean hasDelegate() {
        return false;
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
    public void storeMapping(Type type, Mapping mapping) {

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
