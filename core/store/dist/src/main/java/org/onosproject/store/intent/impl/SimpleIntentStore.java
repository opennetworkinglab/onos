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
package org.onosproject.store.intent.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.intent.BatchWrite;
import org.onosproject.net.intent.BatchWrite.Operation;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.impl.SystemClockTimestamp;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

//TODO Note: this store will be removed once the GossipIntentStore is stable

@Component(immediate = true)
@Service
//FIXME remove this
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
    public IntentState getIntentState(Key intentKey) {
        IntentData data = current.get(intentKey);
        return (data != null) ? data.state() : null;
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        // TODO: implement this or delete class
        return null;
        /*
        for (IntentData data : current.values()) {
            if (Objects.equals(data.intent().id(), intentId)) {
                return data.installables();
            }
        }
        return null;
        */
    }

    /*
     * Execute writes in a batch.
     *
     * @param batch BatchWrite to execute
     * @return failed operations
     */
    @Override
    public List<Operation> batchWrite(BatchWrite batch) {
        throw new UnsupportedOperationException("deprecated");
        /*
        if (batch.isEmpty()) {
            return Collections.emptyList();
        }

        List<Operation> failed = Lists.newArrayList();
        for (Operation op : batch.operations()) {
            switch (op.type()) {
            case CREATE_INTENT:
                checkArgument(op.args().size() == 1,
                              "CREATE_INTENT takes 1 argument. %s", op);
                Intent intent = (Intent) op.args().get(0);
                // TODO: what if it failed?
//                createIntent(intent); FIXME
                break;

            case REMOVE_INTENT:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INTENT takes 1 argument. %s", op);
                IntentId intentId = (IntentId) op.args().get(0);
//                removeIntent(intentId); FIXME
                break;

            case REMOVE_INSTALLED:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INSTALLED takes 1 argument. %s", op);
                intentId = (IntentId) op.args().get(0);
                removeInstalledIntents(intentId);
                break;

            case SET_INSTALLABLE:
                checkArgument(op.args().size() == 2,
                              "SET_INSTALLABLE takes 2 arguments. %s", op);
                intentId = (IntentId) op.args().get(0);
                @SuppressWarnings("unchecked")
                List<Intent> installableIntents = (List<Intent>) op.args().get(1);
                setInstallableIntents(intentId, installableIntents);
                break;

            case SET_STATE:
                checkArgument(op.args().size() == 2,
                              "SET_STATE takes 2 arguments. %s", op);
                intent = (Intent) op.args().get(0);
                IntentState newState = (IntentState) op.args().get(1);
                setState(intent, newState);
                break;

            default:
                break;
            }
        }
        return failed;
        */
    }

    @Override
    public void write(IntentData newData) {
        synchronized (this) {
            // TODO this could be refactored/cleaned up
            IntentData currentData = current.get(newData.key());
            IntentData pendingData = pending.get(newData.key());
            if (currentData == null ||
                    // current version is less than or equal to newData's
                    // Note: current and newData's versions will be equal for state updates
                    currentData.version().compareTo(newData.version()) <= 0) {
                current.put(newData.key(), newData);

                if (pendingData != null
                        // pendingData version is less than or equal to newData's
                        // Note: a new update for this key could be pending (it's version will be greater)
                        && pendingData.version().compareTo(newData.version()) <= 0) {
                    pending.remove(newData.key());
                }

                try {
                    notifyDelegate(IntentEvent.getEvent(newData));
                } catch (IllegalArgumentException e) {
                    //no-op
                    log.trace("ignore this exception: {}", e);
                }
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
    public void addPending(IntentData data) {
        data.setVersion(new SystemClockTimestamp());
        synchronized (this) {
            IntentData existingData = pending.get(data.key());
            if (existingData == null ||
                    // existing version is strictly less than data's version
                    // Note: if they are equal, we already have the update
                    // TODO maybe we should still make this <= to be safe?
                    existingData.version().compareTo(data.version()) < 0) {
                pending.put(data.key(), data);
                checkNotNull(delegate, "Store delegate is not set")
                        .process(data);
                notifyDelegate(IntentEvent.getEvent(data));
            } else {
                log.debug("IntentData {} is older than existing: {}",
                          data, existingData);
            }
            //TODO consider also checking the current map at this point
        }
    }


    @Override
    public boolean isMaster(Intent intent) {
        return true;
    }
}
