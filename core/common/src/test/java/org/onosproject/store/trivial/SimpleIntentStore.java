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
package org.onosproject.store.trivial;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.IntentState.PURGE_REQ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple single-instance implementation of the intent store.
 */
@Component(immediate = true)
@Service
public class SimpleIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());

    private final Map<Key, IntentData> current = Maps.newConcurrentMap();
    private final Map<Key, IntentData> pending = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public long getIntentCount() {
        return current.size();
    }

    @Override
    public Iterable<Intent> getIntents() {
        return current.values().stream()
                .map(IntentData::intent)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IntentData> getIntentData(boolean localOnly, long olderThan) {
        if (localOnly || olderThan > 0) {
            long older = System.nanoTime() - olderThan * 1_000_000; //convert ms to ns
            final SystemClockTimestamp time = new SystemClockTimestamp(older);
            return current.values().stream()
                    .filter(data -> data.version().isOlderThan(time) &&
                            (!localOnly || isMaster(data.key())))
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList(current.values());
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        IntentData data = current.get(intentKey);
        return (data != null) ? data.state() : null;
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        IntentData data = current.get(intentKey);
        if (data != null) {
            return data.installables();
        }
        return null;
    }

    @Override
    public void write(IntentData newData) {
        checkNotNull(newData);

        synchronized (this) {
            // TODO this could be refactored/cleaned up
            IntentData currentData = current.get(newData.key());
            IntentData pendingData = pending.get(newData.key());

            if (IntentData.isUpdateAcceptable(currentData, newData)) {
                if (pendingData != null) {
                    if (pendingData.state() == PURGE_REQ) {
                        current.remove(newData.key(), newData);
                    } else {
                        current.put(newData.key(), IntentData.copy(newData));
                    }

                    if (pendingData.version().compareTo(newData.version()) <= 0) {
                        // pendingData version is less than or equal to newData's
                        // Note: a new update for this key could be pending (it's version will be greater)
                        pending.remove(newData.key());
                    }
                }
                IntentEvent.getEvent(newData).ifPresent(this::notifyDelegate);
            }
        }
    }

    @Override
    public void batchWrite(Iterable<IntentData> updates) {
        for (IntentData data : updates) {
            write(data);
        }
    }

    @Override
    public Intent getIntent(Key key) {
        IntentData data = current.get(key);
        return (data != null) ? data.intent() : null;
    }

    @Override
    public IntentData getIntentData(Key key) {
        IntentData currentData = current.get(key);
        if (currentData == null) {
            return null;
        }
        return IntentData.copy(currentData);
    }

    @Override
    public void addPending(IntentData data) {
        if (data.version() == null) { // recompiled intents will already have a version
            data = new IntentData(data.intent(), data.state(), new SystemClockTimestamp());
        }
        synchronized (this) {
            IntentData existingData = pending.get(data.key());
            if (existingData == null ||
                    // existing version is strictly less than data's version
                    // Note: if they are equal, we already have the update
                    // TODO maybe we should still make this <= to be safe?
                    existingData.version().compareTo(data.version()) < 0) {
                pending.put(data.key(), data);
                checkNotNull(delegate, "Store delegate is not set")
                        .process(IntentData.copy(data));
                IntentEvent.getEvent(data).ifPresent(this::notifyDelegate);
            } else {
                log.debug("IntentData {} is older than existing: {}",
                          data, existingData);
            }
            //TODO consider also checking the current map at this point
        }
    }

    @Override
    public boolean isMaster(Key intentKey) {
        return true;
    }

    @Override
    public Iterable<Intent> getPending() {
        return pending.values().stream()
                .map(IntentData::intent)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IntentData> getPendingData() {
        return Lists.newArrayList(pending.values());
    }

    @Override
    public IntentData getPendingData(Key intentKey) {
        return pending.get(intentKey);
    }

    @Override
    public Iterable<IntentData> getPendingData(boolean localOnly, long olderThan) {
        long older = System.nanoTime() - olderThan * 1_000_000; //convert ms to ns
        final SystemClockTimestamp time = new SystemClockTimestamp(older);
        return pending.values().stream()
                .filter(data -> data.version().isOlderThan(time) &&
                        (!localOnly || isMaster(data.key())))
                .collect(Collectors.toList());
    }
}
