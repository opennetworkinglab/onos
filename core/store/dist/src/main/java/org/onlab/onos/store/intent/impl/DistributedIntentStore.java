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

import com.google.common.collect.ImmutableSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentStore;
import org.onlab.onos.net.intent.IntentStoreDelegate;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.serializers.KryoNamespaces;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.onos.store.serializers.StoreSerializer;
import org.onlab.onos.store.service.DatabaseAdminService;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.impl.CMap;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Verify.verify;
import static org.onlab.onos.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = false, enabled = false)
@Service
public class DistributedIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    /** Valid parking state, which can transition to INSTALLED. */
    private static final Set<IntentState> PRE_INSTALLED = EnumSet.of(SUBMITTED, FAILED);

    /** Valid parking state, which can transition to WITHDRAWN. */
    private static final Set<IntentState> PRE_WITHDRAWN = EnumSet.of(INSTALLED, FAILED);

    private final Logger log = getLogger(getClass());

    // Assumption: IntentId will not have synonyms
    private CMap<IntentId, Intent> intents;
    private CMap<IntentId, IntentState> states;

    // TODO left behind transient state issue: ONOS-103
    // Map to store instance local intermediate state transition
    private transient Map<IntentId, IntentState> transientStates = new ConcurrentHashMap<>();

    private CMap<IntentId, List<Intent>> installable;

    private StoreSerializer serializer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DatabaseAdminService dbAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DatabaseService dbService;

    @Activate
    public void activate() {
        // FIXME: We need a way to add serializer for intents which has been plugged-in.
        // As a short term workaround, relax Kryo config to
        // registrationRequired=false
        serializer = new KryoSerializer() {

            @Override
            protected void setupKryoPool() {
                serializerPool = KryoNamespace.newBuilder()
                        .setRegistrationRequired(false)
                        .register(KryoNamespaces.API)
                        .build()
                        .populate(1);
            }
        };

        intents = new CMap<>(dbAdminService, dbService, "intents", serializer);

        states = new CMap<>(dbAdminService, dbService, "intent-states", serializer);

        transientStates.clear();

        installable = new CMap<>(dbAdminService, dbService, "installable-intents", serializer);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public IntentEvent createIntent(Intent intent) {
        boolean absent = intents.putIfAbsent(intent.id(), intent);
        if (!absent) {
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


    @Override
    public IntentEvent setState(Intent intent, IntentState state) {
        final IntentId id = intent.id();
        IntentEvent.Type type = null;
        final IntentState prevParking;
        boolean transientStateChangeOnly = false;
        boolean updated;

        // parking state transition
        switch (state) {
        case SUBMITTED:
            prevParking = states.get(id);
            verify(prevParking == null,
                   "Illegal state transition attempted from %s to SUBMITTED",
                   prevParking);
            updated = states.putIfAbsent(id, SUBMITTED);
            verify(updated, "Conditional replace %s => %s failed", prevParking, SUBMITTED);
            type = IntentEvent.Type.SUBMITTED;
            break;

        case INSTALLED:
            prevParking = states.get(id);
            verify(PRE_INSTALLED.contains(prevParking),
                   "Illegal state transition attempted from %s to INSTALLED",
                   prevParking);
            updated = states.replace(id, prevParking, INSTALLED);
            verify(updated, "Conditional replace %s => %s failed", prevParking, INSTALLED);
            type = IntentEvent.Type.INSTALLED;
            break;

        case FAILED:
            prevParking = states.get(id);
            updated = states.replace(id, prevParking, FAILED);
            verify(updated, "Conditional replace %s => %s failed", prevParking, FAILED);
            type = IntentEvent.Type.FAILED;
            break;

        case WITHDRAWN:
            prevParking = states.get(id);
            verify(PRE_WITHDRAWN.contains(prevParking),
                   "Illegal state transition attempted from %s to WITHDRAWN",
                   prevParking);
            updated = states.replace(id, prevParking, WITHDRAWN);
            verify(updated, "Conditional replace %s => %s failed", prevParking, WITHDRAWN);
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
}
