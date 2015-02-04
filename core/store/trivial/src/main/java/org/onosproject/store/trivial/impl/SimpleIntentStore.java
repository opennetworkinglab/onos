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

import com.google.common.collect.Lists;
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
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service
public class SimpleIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());

    // current state maps FIXME.. make this a IntentData map
    private final Map<String, IntentData> current = Maps.newConcurrentMap();
    private final Map<String, IntentData> pending = Maps.newConcurrentMap(); //String is "key"

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
    public Intent getIntent(IntentId intentId) {
        throw new UnsupportedOperationException("deprecated");
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        throw new UnsupportedOperationException("deprecated");
    }

    private void setState(Intent intent, IntentState state) {
        //FIXME
        IntentId id = intent.id();
//        states.put(id, state);
        IntentEvent.Type type = null;

        switch (state) {
        case INSTALL_REQ:
            type = IntentEvent.Type.INSTALL_REQ;
            break;
        case INSTALLED:
            type = IntentEvent.Type.INSTALLED;
            break;
        case FAILED:
            type = IntentEvent.Type.FAILED;
            break;
        case WITHDRAW_REQ:
            type = IntentEvent.Type.WITHDRAW_REQ;
            break;
        case WITHDRAWN:
            type = IntentEvent.Type.WITHDRAWN;
            break;
        default:
            break;
        }
        if (type != null) {
            notifyDelegate(new IntentEvent(type, intent));
        }
    }

    private void setInstallableIntents(IntentId intentId, List<Intent> result) {
        //FIXME
//        installable.put(intentId, result);
    }

    @Override
    public List<Intent> getInstallableIntents(IntentId intentId) {
        throw new UnsupportedOperationException("deprecated");
    }

    @Override
    public IntentData getIntentData(String key) {
        return current.get(key);
    }

    private void removeInstalledIntents(IntentId intentId) {
        //FIXME
//        installable.remove(intentId);
    }

    /**
     * Execute writes in a batch.
     *
     * @param batch BatchWrite to execute
     * @return failed operations
     */
    @Override
    public List<Operation> batchWrite(BatchWrite batch) {
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
    }

    @Override
    public void addPending(IntentData data) {
        //FIXME need to compare versions
        pending.put(data.key(), data);
        checkNotNull(delegate, "Store delegate is not set")
                .process(data);
    }
    // FIXME!!! pending.remove(intent.key()); // TODO check version


    @Override
    public boolean isMaster(Intent intent) {
        return true;
    }
}
