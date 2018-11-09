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

package org.onosproject.incubator.net.virtual.store.impl;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntentStore;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.store.Timestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.IntentState.PURGE_REQ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple single-instance implementation of the intent store for virtual networks.
 */

@Component(immediate = true, service = VirtualNetworkIntentStore.class)
public class SimpleVirtualIntentStore
        extends AbstractVirtualStore<IntentEvent, IntentStoreDelegate>
        implements VirtualNetworkIntentStore {

    private final Logger log = getLogger(getClass());

    private final Map<NetworkId, Map<Key, IntentData>> currentByNetwork =
            Maps.newConcurrentMap();
    private final Map<NetworkId, Map<Key, IntentData>> pendingByNetwork =
            Maps.newConcurrentMap();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }


    @Override
    public long getIntentCount(NetworkId networkId) {
        return getCurrentMap(networkId).size();
    }

    @Override
    public Iterable<Intent> getIntents(NetworkId networkId) {
        return getCurrentMap(networkId).values().stream()
                .map(IntentData::intent)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IntentData> getIntentData(NetworkId networkId,
                                              boolean localOnly, long olderThan) {
        if (localOnly || olderThan > 0) {
            long older = System.nanoTime() - olderThan * 1_000_000; //convert ms to ns
            final SystemClockTimestamp time = new SystemClockTimestamp(older);
            return getCurrentMap(networkId).values().stream()
                    .filter(data -> data.version().isOlderThan(time) &&
                            (!localOnly || isMaster(networkId, data.key())))
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList(getCurrentMap(networkId).values());
    }

    @Override
    public IntentState getIntentState(NetworkId networkId, Key intentKey) {
        IntentData data = getCurrentMap(networkId).get(intentKey);
        return (data != null) ? data.state() : null;
    }

    @Override
    public List<Intent> getInstallableIntents(NetworkId networkId, Key intentKey) {
        IntentData data = getCurrentMap(networkId).get(intentKey);
        if (data != null) {
            return data.installables();
        }
        return null;
    }

    @Override
    public void write(NetworkId networkId, IntentData newData) {
        checkNotNull(newData);

        synchronized (this) {
            // TODO this could be refactored/cleaned up
            IntentData currentData = getCurrentMap(networkId).get(newData.key());
            IntentData pendingData = getPendingMap(networkId).get(newData.key());

            if (IntentData.isUpdateAcceptable(currentData, newData)) {
                if (pendingData != null) {
                    if (pendingData.state() == PURGE_REQ) {
                        getCurrentMap(networkId).remove(newData.key(), newData);
                    } else {
                        getCurrentMap(networkId).put(newData.key(), IntentData.copy(newData));
                    }

                    if (pendingData.version().compareTo(newData.version()) <= 0) {
                        // pendingData version is less than or equal to newData's
                        // Note: a new update for this key could be pending (it's version will be greater)
                        getPendingMap(networkId).remove(newData.key());
                    }
                }
                IntentEvent.getEvent(newData).ifPresent(e -> notifyDelegate(networkId, e));
            }
        }
    }

    @Override
    public void batchWrite(NetworkId networkId, Iterable<IntentData> updates) {
        for (IntentData data : updates) {
            write(networkId, data);
        }
    }

    @Override
    public Intent getIntent(NetworkId networkId, Key key) {
        IntentData data = getCurrentMap(networkId).get(key);
        return (data != null) ? data.intent() : null;
    }

    @Override
    public IntentData getIntentData(NetworkId networkId, Key key) {
        IntentData currentData = getCurrentMap(networkId).get(key);
        if (currentData == null) {
            return null;
        }
        return IntentData.copy(currentData);
    }

    @Override
    public void addPending(NetworkId networkId, IntentData data) {
        if (data.version() == null) { // recompiled intents will already have a version
            data = new IntentData(data.intent(), data.state(), new SystemClockTimestamp());
        }
        synchronized (this) {
            IntentData existingData = getPendingMap(networkId).get(data.key());
            if (existingData == null ||
                    // existing version is strictly less than data's version
                    // Note: if they are equal, we already have the update
                    // TODO maybe we should still make this <= to be safe?
                    existingData.version().compareTo(data.version()) < 0) {
                getPendingMap(networkId).put(data.key(), data);

                checkNotNull(delegateMap.get(networkId), "Store delegate is not set")
                        .process(IntentData.copy(data));
                IntentEvent.getEvent(data).ifPresent(e -> notifyDelegate(networkId, e));
            } else {
                log.debug("IntentData {} is older than existing: {}",
                          data, existingData);
            }
            //TODO consider also checking the current map at this point
        }
    }

    @Override
    public boolean isMaster(NetworkId networkId, Key intentKey) {
        return true;
    }

    @Override
    public Iterable<Intent> getPending(NetworkId networkId) {
        return getPendingMap(networkId).values().stream()
                .map(IntentData::intent)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IntentData> getPendingData(NetworkId networkId) {
        return Lists.newArrayList(getPendingMap(networkId).values());
    }

    @Override
    public IntentData getPendingData(NetworkId networkId, Key intentKey) {
        return getPendingMap(networkId).get(intentKey);
    }

    @Override
    public Iterable<IntentData> getPendingData(NetworkId networkId,
                                               boolean localOnly, long olderThan) {
        long older = System.nanoTime() - olderThan * 1_000_000; //convert ms to ns
        final SystemClockTimestamp time = new SystemClockTimestamp(older);
        return getPendingMap(networkId).values().stream()
                .filter(data -> data.version().isOlderThan(time) &&
                        (!localOnly || isMaster(networkId, data.key())))
                .collect(Collectors.toList());
    }

    /**
     * Returns the current intent map for a specific virtual network.
     *
     * @param networkId a virtual network identifier
     * @return the current map for the requested virtual network
     */
    private Map<Key, IntentData> getCurrentMap(NetworkId networkId) {
        currentByNetwork.computeIfAbsent(networkId,
                                   n -> Maps.newConcurrentMap());
        return currentByNetwork.get(networkId);
    }

    /**
     * Returns the pending intent map for a specific virtual network.
     *
     * @param networkId a virtual network identifier
     * @return the pending intent map for the requested virtual network
     */
    private Map<Key, IntentData> getPendingMap(NetworkId networkId) {
        pendingByNetwork.computeIfAbsent(networkId,
                                   n -> Maps.newConcurrentMap());
        return pendingByNetwork.get(networkId);
    }

    public class SystemClockTimestamp implements Timestamp {

        private final long nanoTimestamp;

        public SystemClockTimestamp() {
            nanoTimestamp = System.nanoTime();
        }

        public SystemClockTimestamp(long timestamp) {
            nanoTimestamp = timestamp;
        }

        @Override
        public int compareTo(Timestamp o) {
            checkArgument(o instanceof SystemClockTimestamp,
                          "Must be SystemClockTimestamp", o);
            SystemClockTimestamp that = (SystemClockTimestamp) o;

            return ComparisonChain.start()
                    .compare(this.nanoTimestamp, that.nanoTimestamp)
                    .result();
        }
    }
}
