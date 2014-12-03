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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.intent.BatchWrite;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.BatchWrite.Operation;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service
public class SimpleIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());
    private final Map<IntentId, Intent> intents = new ConcurrentHashMap<>();
    private final Map<IntentId, IntentState> states = new ConcurrentHashMap<>();
    private final Map<IntentId, List<Intent>> installable = new ConcurrentHashMap<>();


    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void createIntent(Intent intent) {
        if (intents.containsKey(intent.id())) {
            return;
        }
        intents.put(intent.id(), intent);
        this.setState(intent, IntentState.INSTALL_REQ);
    }

    @Override
    public void removeIntent(IntentId intentId) {
        checkState(getIntentState(intentId) == WITHDRAWN,
                   "Intent state for {} is not WITHDRAWN.", intentId);
        intents.remove(intentId);
        installable.remove(intentId);
        states.remove(intentId);
    }

    @Override
    public long getIntentCount() {
        return intents.size();
    }

    @Override
    public Iterable<Intent> getIntents() {
        return ImmutableSet.copyOf(intents.values());
    }

    @Override
    public Intent getIntent(IntentId intentId) {
        return intents.get(intentId);
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        return states.get(id);
    }

    @Override
    public void setState(Intent intent, IntentState state) {
        IntentId id = intent.id();
        states.put(id, state);
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

    @Override
    public void setInstallableIntents(IntentId intentId, List<Intent> result) {
        installable.put(intentId, result);
    }

    @Override
    public List<Intent> getInstallableIntents(IntentId intentId) {
        return installable.get(intentId);
    }

    @Override
    public void removeInstalledIntents(IntentId intentId) {
        installable.remove(intentId);
    }
    /**
     * Execute writes in a batch.
     *
     * @param batch BatchWrite to execute
     * @return failed operations
     */
    @Override
    public List<Operation> batchWrite(BatchWrite batch) {
        List<Operation> failed = Lists.newArrayList();
        for (Operation op : batch.operations()) {
            switch (op.type()) {
            case CREATE_INTENT:
                checkArgument(op.args().size() == 1,
                              "CREATE_INTENT takes 1 argument. %s", op);
                Intent intent = (Intent) op.args().get(0);
                // TODO: what if it failed?
                createIntent(intent);
                break;

            case REMOVE_INTENT:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INTENT takes 1 argument. %s", op);
                IntentId intentId = (IntentId) op.args().get(0);
                removeIntent(intentId);
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
}
