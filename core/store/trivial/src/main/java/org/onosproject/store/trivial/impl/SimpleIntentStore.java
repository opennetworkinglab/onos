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
package org.onosproject.store.trivial.impl;

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
import static org.onosproject.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service
public class SimpleIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());

    private final Map<Key, IntentData> current = Maps.newConcurrentMap();
    private final Map<Key, IntentData> pending = Maps.newConcurrentMap();

    private IntentData copyData(IntentData original) {
        if (original == null) {
            return null;
        }
        IntentData result =
                new IntentData(original.intent(), original.state(), original.version());

        if (original.installables() != null) {
            result.setInstallables(original.installables());
        }
        return result;
    }

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
        IntentData data = current.get(intentKey);
        if (data != null) {
            return data.installables();
        }
        return null;
    }


    /**
     * Determines whether an intent data update is allowed. The update must
     * either have a higher version than the current data, or the state
     * transition between two updates of the same version must be sane.
     *
     * @param currentData existing intent data in the store
     * @param newData new intent data update proposal
     * @return true if we can apply the update, otherwise false
     */
    private boolean isUpdateAcceptable(IntentData currentData, IntentData newData) {

        if (currentData == null) {
            return true;
        } else if (currentData.version().compareTo(newData.version()) < 0) {
            return true;
        } else if (currentData.version().compareTo(newData.version()) > 0) {
            return false;
        }

        // current and new data versions are the same
        IntentState currentState = currentData.state();
        IntentState newState = newData.state();

        switch (newState) {
            case INSTALLING:
                if (currentState == INSTALLING) {
                    return false;
                }
            // FALLTHROUGH
            case INSTALLED:
                if (currentState == INSTALLED) {
                    return false;
                } else if (currentState == WITHDRAWING || currentState == WITHDRAWN) {
                    log.warn("Invalid state transition from {} to {} for intent {}",
                             currentState, newState, newData.key());
                    return false;
                }
                return true;

            case WITHDRAWING:
                if (currentState == WITHDRAWING) {
                    return false;
                }
            // FALLTHOUGH
            case WITHDRAWN:
                if (currentState == WITHDRAWN) {
                    return false;
                } else if (currentState == INSTALLING || currentState == INSTALLED) {
                    log.warn("Invalid state transition from {} to {} for intent {}",
                             currentState, newState, newData.key());
                    return false;
                }
                return true;


            case FAILED:
                if (currentState == FAILED) {
                    return false;
                }
                return true;


            case COMPILING:
            case RECOMPILING:
            case INSTALL_REQ:
            case WITHDRAW_REQ:
            default:
                log.warn("Invalid state {} for intent {}", newState, newData.key());
                return false;
        }
    }

    @Override
    public void write(IntentData newData) {
        synchronized (this) {
            // TODO this could be refactored/cleaned up
            IntentData currentData = current.get(newData.key());
            IntentData pendingData = pending.get(newData.key());

            if (isUpdateAcceptable(currentData, newData)) {
                if (pendingData.state() == PURGE_REQ) {
                    current.remove(newData.key(), newData);
                } else {
                    current.put(newData.key(), copyData(newData));
                }

                if (pendingData != null
                        // pendingData version is less than or equal to newData's
                        // Note: a new update for this key could be pending (it's version will be greater)
                        && pendingData.version().compareTo(newData.version()) <= 0) {
                    pending.remove(newData.key());
                }

                notifyDelegateIfNotNull(IntentEvent.getEvent(newData));
            }
        }
    }

    private void notifyDelegateIfNotNull(IntentEvent event) {
        if (event != null) {
            notifyDelegate(event);
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
        return copyData(current.get(key));
    }

    @Override
    public void addPending(IntentData data) {
        if (data.version() == null) { // recompiled intents will already have a version
            data.setVersion(new SystemClockTimestamp());
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
                        .process(data);
                notifyDelegateIfNotNull(IntentEvent.getEvent(data));
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
}
