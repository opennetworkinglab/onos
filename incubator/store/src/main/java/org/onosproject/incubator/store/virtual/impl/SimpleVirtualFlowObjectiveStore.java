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

package org.onosproject.incubator.store.virtual.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowObjectiveStore;
import org.onosproject.net.behaviour.DefaultNextGroup;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ObjectiveEvent;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Single instance implementation of store to manage
 * the inventory of created next groups for virtual network.
 */
@Component(immediate = true)
@Service
public class SimpleVirtualFlowObjectiveStore
        extends AbstractVirtualStore<ObjectiveEvent, FlowObjectiveStoreDelegate>
        implements VirtualNetworkFlowObjectiveStore {

    private final Logger log = getLogger(getClass());

    private ConcurrentMap<NetworkId, ConcurrentMap<Integer, byte[]>> nextGroupsMap;

    private AtomicCounter nextIds;

    // event queue to separate map-listener threads from event-handler threads (tpool)
    private BlockingQueue<VirtualObjectiveEvent> eventQ;
    private ExecutorService tpool;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Activate
    public void activate() {
        tpool = Executors.newFixedThreadPool(4, groupedThreads("onos/virtual/flobj-notifier", "%d", log));
        eventQ = new LinkedBlockingQueue<>();
        tpool.execute(new FlowObjectiveNotifier());

        initNextGroupsMap();

        nextIds = storageService.getAtomicCounter("next-objective-counter");
        log.info("Started");
    }

    public void deactivate() {
        log.info("Stopped");
    }

    protected void initNextGroupsMap() {
        nextGroupsMap = Maps.newConcurrentMap();
    }

    protected void updateNextGroupsMap(NetworkId networkId,
                                       ConcurrentMap<Integer, byte[]> nextGroups) {
    }

    protected ConcurrentMap<Integer, byte[]> getNextGroups(NetworkId networkId) {
        nextGroupsMap.computeIfAbsent(networkId, n -> Maps.newConcurrentMap());
        return nextGroupsMap.get(networkId);
    }

    @Override
    public void putNextGroup(NetworkId networkId, Integer nextId, NextGroup group) {
        ConcurrentMap<Integer, byte[]> nextGroups = getNextGroups(networkId);
        nextGroups.put(nextId, group.data());
        updateNextGroupsMap(networkId, nextGroups);

        eventQ.add(new VirtualObjectiveEvent(networkId, ObjectiveEvent.Type.ADD, nextId));
    }

    @Override
    public NextGroup getNextGroup(NetworkId networkId, Integer nextId) {
        ConcurrentMap<Integer, byte[]> nextGroups = getNextGroups(networkId);
        byte[] groupData = nextGroups.get(nextId);
        if (groupData != null) {
            return new DefaultNextGroup(groupData);
        }
        return null;
    }

    @Override
    public NextGroup removeNextGroup(NetworkId networkId, Integer nextId) {
        ConcurrentMap<Integer, byte[]> nextGroups = getNextGroups(networkId);
        byte[] nextGroup = nextGroups.remove(nextId);
        updateNextGroupsMap(networkId, nextGroups);

        eventQ.add(new VirtualObjectiveEvent(networkId, ObjectiveEvent.Type.REMOVE, nextId));

        return new DefaultNextGroup(nextGroup);
    }

    @Override
    public Map<Integer, NextGroup> getAllGroups(NetworkId networkId) {
        ConcurrentMap<Integer, byte[]> nextGroups = getNextGroups(networkId);

        Map<Integer, NextGroup> nextGroupMappings = new HashMap<>();
        for (int key : nextGroups.keySet()) {
            NextGroup nextGroup = getNextGroup(networkId, key);
            if (nextGroup != null) {
                nextGroupMappings.put(key, nextGroup);
            }
        }
        return nextGroupMappings;
    }

    @Override
    public int allocateNextId(NetworkId networkId) {
        return (int) nextIds.incrementAndGet();
    }

    private class FlowObjectiveNotifier implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    VirtualObjectiveEvent vEvent = eventQ.take();
                    notifyDelegate(vEvent.networkId(), vEvent);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class VirtualObjectiveEvent extends ObjectiveEvent {
        NetworkId networkId;

        public VirtualObjectiveEvent(NetworkId networkId, Type type,
                                     Integer objective) {
            super(type, objective);
            this.networkId = networkId;
        }

        NetworkId networkId() {
            return networkId;
        }
    }
}
