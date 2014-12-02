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

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.metrics.MetricsService;
import org.onlab.onos.core.MetricsHelper;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentStore;
import org.onlab.onos.net.intent.IntentStore.BatchWrite.Operation;
import org.onlab.onos.net.intent.IntentStoreDelegate;
import org.onlab.onos.store.hz.AbstractHazelcastStore;
import org.onlab.onos.store.hz.SMap;
import org.onlab.onos.store.serializers.KryoNamespaces;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.onlab.onos.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.metrics.MetricsUtil.*;

@Component(immediate = true, enabled = true)
@Service
public class HazelcastIntentStore
        extends AbstractHazelcastStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore, MetricsHelper {

    /** Valid parking state, which can transition to INSTALLED. */
    private static final Set<IntentState> PRE_INSTALLED = EnumSet.of(SUBMITTED, INSTALLED, FAILED);

    /** Valid parking state, which can transition to WITHDRAWN. */
    private static final Set<IntentState> PRE_WITHDRAWN = EnumSet.of(INSTALLED, FAILED);

    private static final Set<IntentState> PARKING = EnumSet.of(SUBMITTED, INSTALLED, WITHDRAWN, FAILED);

    private final Logger log = getLogger(getClass());

    // Assumption: IntentId will not have synonyms
    private SMap<IntentId, Intent> intents;
    private SMap<IntentId, IntentState> states;

    // Map to store instance local intermediate state transition
    private transient Map<IntentId, IntentState> transientStates = new ConcurrentHashMap<>();

    private SMap<IntentId, List<Intent>> installable;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    // TODO make this configurable
    private boolean onlyLogTransitionError = true;

    private Timer createIntentTimer;
    private Timer removeIntentTimer;
    private Timer setInstallableIntentsTimer;
    private Timer getInstallableIntentsTimer;
    private Timer removeInstalledIntentsTimer;
    private Timer setStateTimer;
    private Timer getIntentCountTimer;
    private Timer getIntentsTimer;
    private Timer getIntentTimer;
    private Timer getIntentStateTimer;

    private Timer createResponseTimer(String methodName) {
        return createTimer("IntentStore", methodName, "responseTime");
    }

    @Override
    @Activate
    public void activate() {
        createIntentTimer = createResponseTimer("createIntent");
        removeIntentTimer = createResponseTimer("removeIntent");
        setInstallableIntentsTimer = createResponseTimer("setInstallableIntents");
        getInstallableIntentsTimer = createResponseTimer("getInstallableIntents");
        removeInstalledIntentsTimer = createResponseTimer("removeInstalledIntents");
        setStateTimer = createResponseTimer("setState");
        getIntentCountTimer = createResponseTimer("getIntentCount");
        getIntentsTimer = createResponseTimer("getIntents");
        getIntentTimer = createResponseTimer("getIntent");
        getIntentStateTimer = createResponseTimer("getIntentState");

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
    public MetricsService metricsService() {
        return metricsService;
    }

    @Override
    public void createIntent(Intent intent) {
        Context timer = startTimer(createIntentTimer);
        try {
            Intent existing = intents.putIfAbsent(intent.id(), intent);
            if (existing != null) {
                // duplicate, ignore
                return;
            } else {
                this.setState(intent, IntentState.SUBMITTED);
                return;
            }
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public void removeIntent(IntentId intentId) {
        Context timer = startTimer(removeIntentTimer);
        checkState(getIntentState(intentId) == WITHDRAWN,
                   "Intent state for {} is not WITHDRAWN.", intentId);
        try {
            intents.remove(intentId);
            installable.remove(intentId);
            states.remove(intentId);
            transientStates.remove(intentId);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public long getIntentCount() {
        Context timer = startTimer(getIntentCountTimer);
        try {
            return intents.size();
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public Iterable<Intent> getIntents() {
        Context timer = startTimer(getIntentsTimer);
        try {
            return ImmutableSet.copyOf(intents.values());
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public Intent getIntent(IntentId intentId) {
        Context timer = startTimer(getIntentTimer);
        try {
            return intents.get(intentId);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        Context timer = startTimer(getIntentStateTimer);
        try {
            final IntentState localState = transientStates.get(id);
            if (localState != null) {
                return localState;
            }
            return states.get(id);
        } finally {
            stopTimer(timer);
        }
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
    public void setState(Intent intent, IntentState state) {
        Context timer = startTimer(setStateTimer);
        try {

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

            if (type != null) {
                notifyDelegate(new IntentEvent(type, intent));
            }
            return;
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public void setInstallableIntents(IntentId intentId, List<Intent> result) {
        Context timer = startTimer(setInstallableIntentsTimer);
        try {
            installable.put(intentId, result);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public List<Intent> getInstallableIntents(IntentId intentId) {
        Context timer = startTimer(getInstallableIntentsTimer);
        try {
            return installable.get(intentId);
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public void removeInstalledIntents(IntentId intentId) {
        Context timer = startTimer(removeInstalledIntentsTimer);
        try {
            installable.remove(intentId);
        } finally {
            stopTimer(timer);
        }
    }

    // TODO slice out methods after merging Ali's patch
    // CHECKSTYLE IGNORE MethodLength FOR NEXT 1 LINES
    @Override
    public List<Operation> batchWrite(BatchWrite batch) {
        // Hazelcast version will never fail for conditional failure now.
        List<Operation> failed = new ArrayList<>();

        List<Pair<Operation, List<Future<?>>>> futures = new ArrayList<>(batch.operations().size());
        List<IntentEvent> events = Lists.newArrayList();

        for (Operation op : batch.operations()) {
            switch (op.type()) {
            case CREATE_INTENT:
                checkArgument(op.args().size() == 1,
                              "CREATE_INTENT takes 1 argument. %s", op);
                Intent intent = op.arg(0);
                futures.add(Pair.of(op,
                                    ImmutableList.of(intents.putAsync(intent.id(), intent),
                                                     states.putAsync(intent.id(), SUBMITTED))));
                break;

            case REMOVE_INTENT:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INTENT takes 1 argument. %s", op);
                IntentId intentId = (IntentId) op.arg(0);
                futures.add(Pair.of(op,
                                    ImmutableList.of(intents.removeAsync(intentId),
                                                     states.removeAsync(intentId),
                                                     installable.removeAsync(intentId))));
                break;

            case SET_STATE:
                checkArgument(op.args().size() == 2,
                              "SET_STATE takes 2 arguments. %s", op);
                intent = op.arg(0);
                IntentState newState = op.arg(1);
                futures.add(Pair.of(op,
                                    ImmutableList.of(states.putAsync(intent.id(), newState))));
                break;

            case SET_INSTALLABLE:
                checkArgument(op.args().size() == 2,
                              "SET_INSTALLABLE takes 2 arguments. %s", op);
                intentId = op.arg(0);
                List<Intent> installableIntents = op.arg(1);
                futures.add(Pair.of(op,
                                    ImmutableList.of(installable.putAsync(intentId, installableIntents))));
                break;

            case REMOVE_INSTALLED:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INSTALLED takes 1 argument. %s", op);
                intentId = op.arg(0);
                futures.add(Pair.of(op,
                                    ImmutableList.of(installable.removeAsync(intentId))));
                break;

            default:
                log.warn("Unknown Operation encountered: {}", op);
                failed.add(op);
                break;
            }
        }

        // verify result
        for (Pair<Operation, List<Future<?>>> future : futures) {
            final Operation op = future.getLeft();
            final List<Future<?>> subops = future.getRight();

            switch (op.type()) {

            case CREATE_INTENT:
            {
                Intent intent = op.arg(0);
                IntentState newIntentState = SUBMITTED;

                try {
                    Intent prevIntent = (Intent) subops.get(0).get();
                    IntentState prevIntentState = (IntentState) subops.get(1).get();

                    if (prevIntent != null || prevIntentState != null) {
                        log.warn("Overwriting existing Intent: {}@{} with {}@{}",
                                 prevIntent, prevIntentState,
                                 intent, newIntentState);
                    }
                    events.add(IntentEvent.getEvent(SUBMITTED, intent));
                } catch (InterruptedException e) {
                    log.error("Batch write was interrupted while processing {}", op,  e);
                    failed.add(op);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("Batch write failed processing {}", op,  e);
                    failed.add(op);
                }
                break;
            }

            case REMOVE_INTENT:
            {
                IntentId intentId = op.arg(0);

                try {
                    Intent prevIntent = (Intent) subops.get(0).get();
                    IntentState prevIntentState = (IntentState) subops.get(1).get();
                    @SuppressWarnings("unchecked")
                    List<Intent> prevInstallable = (List<Intent>) subops.get(2).get();

                    if (prevIntent == null) {
                        log.warn("Intent {} was already removed.", intentId);
                    }
                    if (prevIntentState == null) {
                        log.warn("Intent {} state was already removed", intentId);
                    }
                    if (prevInstallable == null) {
                        log.info("Intent {} installable was already removed", intentId);
                    }
                } catch (InterruptedException e) {
                    log.error("Batch write was interrupted while processing {}", op,  e);
                    failed.add(op);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("Batch write failed processing {}", op,  e);
                    failed.add(op);
                }
                break;
            }

            case SET_STATE:
            {
                Intent intent = op.arg(0);
                IntentId intentId = intent.id();
                IntentState newState = op.arg(1);

                try {
                    IntentState prevIntentState = (IntentState) subops.get(0).get();

                    if (PARKING.contains(newState)) {
                        transientStates.remove(intentId);
                        events.add(IntentEvent.getEvent(newState, intent));
                    }

                    log.trace("{} - {} -> {}", intentId, prevIntentState, newState);
                    // TODO sanity check and log?
                } catch (InterruptedException e) {
                    log.error("Batch write was interrupted while processing {}", op,  e);
                    failed.add(op);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("Batch write failed processing {}", op,  e);
                    failed.add(op);
                }
                break;
            }

            case SET_INSTALLABLE:
            {
                IntentId intentId = op.arg(0);
                List<Intent> installableIntents = op.arg(1);

                try {
                    @SuppressWarnings("unchecked")
                    List<Intent> prevInstallable = (List<Intent>) subops.get(0).get();

                    if (prevInstallable != null) {
                        log.warn("Overwriting Intent {} installable {} -> {}",
                                 intentId, prevInstallable, installableIntents);
                    }
                } catch (InterruptedException e) {
                    log.error("Batch write was interrupted while processing {}", op,  e);
                    failed.add(op);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("Batch write failed processing {}", op,  e);
                    failed.add(op);
                }
                break;
            }

            case REMOVE_INSTALLED:
            {
                IntentId intentId = op.arg(0);

                try {
                    @SuppressWarnings("unchecked")
                    List<Intent> prevInstallable = (List<Intent>) subops.get(0).get();

                    if (prevInstallable == null) {
                        log.warn("Intent {} installable was already removed", intentId);
                    }
                } catch (InterruptedException e) {
                    log.error("Batch write was interrupted while processing {}", op,  e);
                    failed.add(op);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("Batch write failed processing {}", op,  e);
                    failed.add(op);
                }
                break;
            }

            default:
                log.warn("Unknown Operation encountered: {}", op);
                if (!failed.contains(op)) {
                    failed.add(op);
                }
                break;
            }
        }

        notifyDelegate(events);

        return failed;
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

                notifyDelegate(IntentEvent.getEvent(event.getValue(), getIntent(intentId)));
            }
        }
    }
}
