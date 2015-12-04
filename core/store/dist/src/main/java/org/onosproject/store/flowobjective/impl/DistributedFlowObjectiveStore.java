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
package org.onosproject.store.flowobjective.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.behaviour.DefaultNextGroup;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ObjectiveEvent;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of created next groups.
 */
@Component(immediate = true, enabled = true)
@Service
public class DistributedFlowObjectiveStore
        extends AbstractStore<ObjectiveEvent, FlowObjectiveStoreDelegate>
        implements FlowObjectiveStore {

    private final Logger log = getLogger(getClass());

    private ConsistentMap<Integer, byte[]> nextGroups;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private AtomicCounter nextIds;

    @Activate
    public void activate() {
        nextGroups = storageService.<Integer, byte[]>consistentMapBuilder()
                .withName("flowobjective-groups")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(byte[].class)
                                .register(Versioned.class)
                                .build()))
                .build();

        nextIds = storageService.atomicCounterBuilder()
                .withName("next-objective-counter")
                .build();

        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void putNextGroup(Integer nextId, NextGroup group) {
        nextGroups.put(nextId, group.data());
        notifyDelegate(new ObjectiveEvent(ObjectiveEvent.Type.ADD, nextId));
    }

    @Override
    public NextGroup getNextGroup(Integer nextId) {
        Versioned<byte[]> versionGroup = nextGroups.get(nextId);
        if (versionGroup != null) {
            return new DefaultNextGroup(versionGroup.value());
        }
        return null;
    }

    @Override
    public NextGroup removeNextGroup(Integer nextId) {
        Versioned<byte[]> versionGroup = nextGroups.remove(nextId);
        if (versionGroup != null) {
            notifyDelegate(new ObjectiveEvent(ObjectiveEvent.Type.REMOVE, nextId));
            return new DefaultNextGroup(versionGroup.value());
        }
        return null;
    }

    @Override
    public int allocateNextId() {
        return (int) nextIds.incrementAndGet();
    }
}
