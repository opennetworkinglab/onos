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
package org.onlab.onos.store.intent.impl;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentStore;
import org.onlab.onos.net.intent.IntentStoreDelegate;
import org.onlab.onos.store.hz.AbstractHazelcastStore;
import org.onlab.onos.store.hz.SMap;
import org.onlab.onos.store.serializers.KryoNamespaces;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onlab.onos.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, enabled = false)
@Service
public class HazelcastIntentStore
        extends AbstractHazelcastStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    /** Valid parking state, which can transition to INSTALLED. */
    private static final Set<IntentState> PRE_INSTALLED = EnumSet.of(SUBMITTED, INSTALLED, FAILED);

    /** Valid parking state, which can transition to WITHDRAWN. */
    private static final Set<IntentState> PRE_WITHDRAWN = EnumSet.of(INSTALLED, FAILED);

    private final Logger log = getLogger(getClass());

    // Assumption: IntentId will not have synonyms
    private SMap<IntentId, Intent> intents;
    private SMap<IntentId, IntentState> states;

    // Map to store instance local intermediate state transition
    private transient Map<IntentId, IntentState> transientStates = new ConcurrentHashMap<>();

    private SMap<IntentId, List<Intent>> installable;

    // TODO make this configurable
    private boolean onlyLogTransitionError = true;

    @Override
    @Activate
    public void activate() {
        // FIXME: We need a way to add serializer for intents which has been plugged-in.
        // As a short term workaround, relax Kryo config to
        // registrationRequired=false
        super.activate();
        super.serializer = new KryoSerializer() {

            @Override
            protected void setupKryoPool() {
                serializerPool = KryoNamespace.newBuilder()
                        .setRegistrationRequired(false)
                        .register(KryoNamespaces.API)
                        .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                        .build();
            }

        };

        // TODO: enable near cache, allow read from backup for this IMap
        IMap<byte[], byte[]> rawIntents = super.theInstance.getMap("intents");
        intents = new SMap<>(rawIntents , super.serializer);

        // TODO: disable near cache, disable read from backup for this IMap
        IMap<byte[], byte[]> rawStates = super.theInstance.getMap("intent-states");
        states = new SMap<>(rawStates , super.serializer);
        EntryListener<IntentId, IntentState> listener = new RemoteIntentStateListener();
        states.addEntryListener(listener , false);

        transientStates.clear();

        // TODO: disable near cache, disable read from backup for this IMap
        IMap<byte[], byte[]> rawInstallables = super.theInstance.getMap("installable-intents");
        installable = new SMap<>(rawInstallables , super.serializer);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public IntentEvent createIntent(Intent intent) {
        Intent existing = intents.putIfAbsent(intent.id(), intent);
        if (existing != null) {
            // duplicate, ignore
            return null;
        } else {
            return this.setState(intent, IntentState.SUBMITTED);
        }
    }

    @Override
    public IntentEvent removeIntent(IntentId intentId) {
        Intent intent = intents.remove(intentId);
        installable.remove(intentId);
        if (intent == null) {
            // was already removed
            return null;
        }
        IntentEvent event = this.setState(intent, WITHDRAWN);
        states.remove(intentId);
        transientStates.remove(intentId);
        // TODO: Should we callremoveInstalledIntents if this Intent was
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
        final IntentState localState = transientStates.get(id);
        if (localState != null) {
            return localState;
        }
        return states.get(id);
    }

    private void verify(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        if (onlyLogTransitionError) {
            if (!expression) {
                log.error(errorMessageTemplate.replace("%s", "{}"), errorMessageArgs);
            }
        } else {
            Verify.verify(expression, errorMessageTemplate, errorMessageArgs);
        }
    }

    @Override
    public IntentEvent setState(Intent intent, IntentState state) {
        final IntentId id = intent.id();
        IntentEvent.Type type = null;
        final IntentState prevParking;
        boolean transientStateChangeOnly = false;

        // parking state transition
        switch (state) {
        case SUBMITTED:
            prevParking = states.get(id);
            if (prevParking == null) {
                IntentState existing = states.putIfAbsent(id, SUBMITTED);
                verify(existing == null, "Conditional replace %s => %s failed", prevParking, SUBMITTED);
            } else {
                verify(prevParking == WITHDRAWN,
                        "Illegal state transition attempted from %s to SUBMITTED",
                        prevParking);
                boolean updated = states.replace(id, prevParking, SUBMITTED);
                verify(updated, "Conditional replace %s => %s failed", prevParking, SUBMITTED);
            }
            type = IntentEvent.Type.SUBMITTED;
            break;
        case INSTALLED:
            prevParking = states.replace(id, INSTALLED);
            verify(PRE_INSTALLED.contains(prevParking),
                   "Illegal state transition attempted from %s to INSTALLED",
                   prevParking);
            type = IntentEvent.Type.INSTALLED;
            break;
        case FAILED:
            prevParking = states.replace(id, FAILED);
            type = IntentEvent.Type.FAILED;
            break;
        case WITHDRAWN:
            prevParking = states.replace(id, WITHDRAWN);
            verify(PRE_WITHDRAWN.contains(prevParking),
                   "Illegal state transition attempted from %s to WITHDRAWN",
                   prevParking);
            type = IntentEvent.Type.WITHDRAWN;
            break;
        default:
            transientStateChangeOnly = true;
            prevParking = null;
            break;
        }
        if (!transientStateChangeOnly) {
            log.debug("Parking State change: {} {}=>{}",  id, prevParking, state);
        }
        // Update instance local state, which includes non-parking state transition
        final IntentState prevTransient = transientStates.put(id, state);
        log.debug("Transient State change: {} {}=>{}", id, prevTransient, state);

        if (type == null) {
            return null;
        }
        return new IntentEvent(type, intent);
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

    public final class RemoteIntentStateListener extends EntryAdapter<IntentId, IntentState> {

        @Override
        public void onEntryEvent(EntryEvent<IntentId, IntentState> event) {
            final Member myself = theInstance.getCluster().getLocalMember();
            if (!myself.equals(event.getMember())) {
                // When Intent state was modified by remote node,
                // clear local transient state.
                final IntentId intentId = event.getKey();
                IntentState oldState = transientStates.remove(intentId);
                if (oldState != null) {
                    log.debug("{} state updated remotely, removing transient state {}",
                              intentId, oldState);
                }
            }
        }
    }
}
