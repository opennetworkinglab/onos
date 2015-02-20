/*
 * Copyright 2014 Open Networking Laboratory
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
import static org.slf4j.LoggerFactory.getLogger;

//TODO Note: this store will be removed

@Component(immediate = true)
@Service
public class SimpleIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());

    // current state maps FIXME.. make this a IntentData map
    private final Map<Key, IntentData> current = Maps.newConcurrentMap();
    private final Map<Key, IntentData> pending = Maps.newConcurrentMap(); //String is "key"

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
        return (data != null) ? data.installables() : null;
    }

    @Override
    public void write(IntentData newData) {
        //FIXME need to compare the versions
        current.put(newData.key(), newData);
        try {
            notifyDelegate(IntentEvent.getEvent(newData));
        } catch (IllegalArgumentException e) {
            //no-op
            log.trace("ignore this exception: {}", e);
        }
        IntentData old = pending.get(newData.key());
        if (old != null /* && FIXME version check */) {
            pending.remove(newData.key());
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
        //FIXME need to compare versions
        pending.put(data.key(), data);
        checkNotNull(delegate, "Store delegate is not set")
                .process(data);
        notifyDelegate(IntentEvent.getEvent(data));
    }

}
