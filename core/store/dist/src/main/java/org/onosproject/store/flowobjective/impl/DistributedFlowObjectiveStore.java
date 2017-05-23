/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.store.service.AtomicIdGenerator;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages the inventory of created next groups.
 */
@Component(immediate = true)
@Service
public class DistributedFlowObjectiveStore
        extends AbstractStore<ObjectiveEvent, FlowObjectiveStoreDelegate>
        implements FlowObjectiveStore {

    private final Logger log = getLogger(getClass());

    private ConsistentMap<Integer, byte[]> nextGroups;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private AtomicIdGenerator nextIds;
    private MapEventListener<Integer, byte[]> mapListener = new NextGroupListener();
    // event queue to separate map-listener threads from event-handler threads (tpool)
    private BlockingQueue<ObjectiveEvent> eventQ;
    private ExecutorService tpool;

    @Activate
    public void activate() {
        tpool = Executors.newFixedThreadPool(4, groupedThreads("onos/flobj-notifier", "%d", log));
        eventQ = new LinkedBlockingQueue<ObjectiveEvent>();
        tpool.execute(new FlowObjectiveNotifier());
        nextGroups = storageService.<Integer, byte[]>consistentMapBuilder()
                .withName("onos-flowobjective-groups")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(byte[].class)
                                .register(Versioned.class)
                                .build("DistributedFlowObjectiveStore")))
                .build();
        nextGroups.addListener(mapListener);
        nextIds = storageService.getAtomicIdGenerator("next-objective-id-generator");
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        nextGroups.removeListener(mapListener);
        tpool.shutdown();
        log.info("Stopped");
    }

    @Override
    public void putNextGroup(Integer nextId, NextGroup group) {
        nextGroups.put(nextId, group.data());
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
            return new DefaultNextGroup(versionGroup.value());
        }
        return null;
    }

    @Override
    public Map<Integer, NextGroup> getAllGroups() {
        Map<Integer, NextGroup> nextGroupMappings = new HashMap<>();
        for (int key : nextGroups.keySet()) {
            NextGroup nextGroup = getNextGroup(key);
            if (nextGroup != null) {
                nextGroupMappings.put(key, nextGroup);
            }
        }
        return nextGroupMappings;
    }

    @Override
    public int allocateNextId() {
        return (int) nextIds.nextId();
    }

    private class FlowObjectiveNotifier implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    notifyDelegate(eventQ.take());
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class NextGroupListener implements MapEventListener<Integer, byte[]> {
        @Override
        public void event(MapEvent<Integer, byte[]> event) {
            switch (event.type()) {
            case INSERT:
                eventQ.add(new ObjectiveEvent(ObjectiveEvent.Type.ADD, event.key()));
                break;
            case REMOVE:
                eventQ.add(new ObjectiveEvent(ObjectiveEvent.Type.REMOVE, event.key()));
                break;
            case UPDATE:
                // TODO Introduce UPDATE ObjectiveEvent when the map is being updated
                break;
            default:
                break;
            }
        }
    }

}
