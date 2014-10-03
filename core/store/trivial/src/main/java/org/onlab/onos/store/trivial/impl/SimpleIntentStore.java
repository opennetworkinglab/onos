package org.onlab.onos.store.trivial.impl;

import static org.onlab.onos.net.intent.IntentState.COMPILED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.intent.InstallableIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentStore;
import org.onlab.onos.net.intent.IntentStoreDelegate;
import org.onlab.onos.store.AbstractStore;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

@Component(immediate = true)
@Service
public class SimpleIntentStore
    extends AbstractStore<IntentEvent, IntentStoreDelegate>
    implements IntentStore {

    private final Logger log = getLogger(getClass());
    private final Map<IntentId, Intent> intents = new HashMap<>();
    private final Map<IntentId, IntentState> states = new HashMap<>();
    private final Map<IntentId, List<InstallableIntent>> installable = new HashMap<>();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public IntentEvent createIntent(Intent intent) {
        intents.put(intent.getId(), intent);
        return this.setState(intent, IntentState.SUBMITTED);
    }

    @Override
    public IntentEvent removeIntent(IntentId intentId) {
        Intent intent = intents.remove(intentId);
        installable.remove(intentId);
        IntentEvent event = this.setState(intent, IntentState.WITHDRAWN);
        states.remove(intentId);
        return event;
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

    // TODO return dispatch event here... replace with state transition methods
    @Override
    public IntentEvent setState(Intent intent, IntentState newState) {
        IntentId id = intent.getId();
        IntentState oldState = states.get(id);
        states.put(id, newState);
        return new IntentEvent(intent, newState, oldState, System.currentTimeMillis());
    }

    @Override
    public IntentEvent addInstallableIntents(IntentId intentId, List<InstallableIntent> result) {
        installable.put(intentId, result);
        return this.setState(intents.get(intentId), COMPILED);
    }

    @Override
    public List<InstallableIntent> getInstallableIntents(IntentId intentId) {
        return installable.get(intentId);
    }

    @Override
    public void removeInstalledIntents(IntentId intentId) {
        installable.remove(intentId);
    }

}
