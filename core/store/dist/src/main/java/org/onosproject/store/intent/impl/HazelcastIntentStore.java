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
package org.onosproject.store.intent.impl;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
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
import org.onlab.util.KryoNamespace;
import org.onosproject.core.MetricsHelper;
import org.onosproject.net.intent.BatchWrite;
import org.onosproject.net.intent.BatchWrite.Operation;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.store.hz.AbstractHazelcastStore;
import org.onosproject.store.hz.SMap;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.metrics.MetricsUtil.startTimer;
import static org.onlab.metrics.MetricsUtil.stopTimer;
import static org.onosproject.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, enabled = false)
@Service
public class HazelcastIntentStore
        extends AbstractHazelcastStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore, MetricsHelper {

    /** Valid parking state, which can transition to INSTALLED. */
    private static final Set<IntentState> PRE_INSTALLED = EnumSet.of(INSTALL_REQ, INSTALLED, FAILED);

    /** Valid parking state, which can transition to WITHDRAWN. */
    private static final Set<IntentState> PRE_WITHDRAWN = EnumSet.of(INSTALLED, FAILED);

    private static final Set<IntentState> PARKING = EnumSet.of(INSTALL_REQ, INSTALLED, WITHDRAWN, FAILED);

    private final Logger log = getLogger(getClass());

    // Assumption: IntentId will not have synonyms
    private static final String INTENTS_MAP_NAME = "intents";
    private SMap<IntentId, Intent> intents;
    private static final String INTENT_STATES_MAP_NAME = "intent-states";
    private SMap<IntentId, IntentState> states;

    // Map to store instance local intermediate state transition
    private transient Map<IntentId, IntentState> transientStates = new ConcurrentHashMap<>();

    private static final String INSTALLABLE_INTENTS_MAP_NAME = "installable-intents";
    private SMap<IntentId, List<Intent>> installable;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    private boolean onlyLogTransitionError = true;

    private Timer getInstallableIntentsTimer;
    private Timer getIntentCountTimer;
    private Timer getIntentsTimer;
    private Timer getIntentTimer;
    private Timer getIntentStateTimer;

    // manual near cache of Intent
    // (Note: IntentId -> Intent is expected to be immutable)
    // entry will be evicted, when state for that IntentId is removed.
    private Map<IntentId, Intent> localIntents;

    private String stateListenerId;

    private String intentsListenerId;

    private Timer createResponseTimer(String methodName) {
        return createTimer("IntentStore", methodName, "responseTime");
    }

    @Override
    @Activate
    public void activate() {
        localIntents = new ConcurrentHashMap<>();

        getInstallableIntentsTimer = createResponseTimer("getInstallableIntents");
        getIntentCountTimer = createResponseTimer("getIntentCount");
        getIntentsTimer = createResponseTimer("getIntents");
        getIntentTimer = createResponseTimer("getIntent");
        getIntentStateTimer = createResponseTimer("getIntentState");

        // We need a way to add serializer for intents which has been plugged-in.
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

        final Config config = theInstance.getConfig();

        MapConfig intentsCfg = config.getMapConfig(INTENTS_MAP_NAME);
        intentsCfg.setAsyncBackupCount(MapConfig.MAX_BACKUP_COUNT - intentsCfg.getBackupCount());

        IMap<byte[], byte[]> rawIntents = super.theInstance.getMap(INTENTS_MAP_NAME);
        intents = new SMap<>(rawIntents , super.serializer);
        intentsListenerId = intents.addEntryListener(new RemoteIntentsListener(), true);

        MapConfig statesCfg = config.getMapConfig(INTENT_STATES_MAP_NAME);
        statesCfg.setAsyncBackupCount(MapConfig.MAX_BACKUP_COUNT - statesCfg.getBackupCount());

        IMap<byte[], byte[]> rawStates = super.theInstance.getMap(INTENT_STATES_MAP_NAME);
        states = new SMap<>(rawStates , super.serializer);
        EntryListener<IntentId, IntentState> listener = new RemoteIntentStateListener();
        stateListenerId = states.addEntryListener(listener, true);

        transientStates.clear();

        MapConfig installableCfg = config.getMapConfig(INSTALLABLE_INTENTS_MAP_NAME);
        installableCfg.setAsyncBackupCount(MapConfig.MAX_BACKUP_COUNT - installableCfg.getBackupCount());

        IMap<byte[], byte[]> rawInstallables = super.theInstance.getMap(INSTALLABLE_INTENTS_MAP_NAME);
        installable = new SMap<>(rawInstallables , super.serializer);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        intents.removeEntryListener(intentsListenerId);
        states.removeEntryListener(stateListenerId);
        log.info("Stopped");
    }

    @Override
    public MetricsService metricsService() {
        return metricsService;
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
    public Intent getIntent(Key intentKey) {
        return null;
    }


    public Intent getIntent(IntentId intentId) {
        Context timer = startTimer(getIntentTimer);
        try {
            Intent intent = localIntents.get(intentId);
            if (intent != null) {
                return intent;
            }
            intent = intents.get(intentId);
            if (intent != null) {
                localIntents.put(intentId, intent);
            }
            return intent;
        } finally {
            stopTimer(timer);
        }
    }

    @Override
    public IntentState getIntentState(Key key) {
        // TODO: either implement this or remove this class
        return IntentState.FAILED;
        /*
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
        */
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
    public List<Intent> getInstallableIntents(Key intentKey) {
        // TODO: implement this or delete class
        return null;

        /*
        Context timer = startTimer(getInstallableIntentsTimer);
        try {
            return installable.get(intentId);
        } finally {
            stopTimer(timer);
        }
        */
    }

    @Override
    public List<Operation> batchWrite(BatchWrite batch) {
        if (batch.isEmpty()) {
            return Collections.emptyList();
        }

        // Hazelcast version will never fail for conditional failure now.
        List<Operation> failed = new ArrayList<>();

        List<Pair<Operation, List<Future<?>>>> futures = new ArrayList<>(batch.operations().size());
        List<IntentEvent> events = Lists.newArrayList();

        batchWriteAsync(batch, failed, futures);

        // verify result
        verifyAsyncWrites(futures, failed, events);

        notifyDelegate(events);

        return failed;
    }

    private void batchWriteAsync(BatchWrite batch, List<Operation> failed,
                                 List<Pair<Operation, List<Future<?>>>> futures) {
        for (Operation op : batch.operations()) {
            switch (op.type()) {
            case CREATE_INTENT:
                checkArgument(op.args().size() == 1,
                              "CREATE_INTENT takes 1 argument. %s", op);
                Intent intent = op.arg(0);
                futures.add(Pair.of(op,
                                    ImmutableList.of(intents.putAsync(intent.id(), intent),
                                                     states.putAsync(intent.id(), INSTALL_REQ))));
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
    }

    /**
     * Checks the async write result Futures and prepare Events to post.
     *
     * @param futures async write Futures
     * @param failed list to output failed batch write operations
     * @param events list to output events to post as result of writes
     */
    private void verifyAsyncWrites(List<Pair<Operation, List<Future<?>>>> futures,
                                   List<Operation> failed,
                                   List<IntentEvent> events) {
        for (Pair<Operation, List<Future<?>>> future : futures) {
            final Operation op = future.getLeft();
            final List<Future<?>> subops = future.getRight();

            switch (op.type()) {

            case CREATE_INTENT:
            {
                Intent intent = op.arg(0);
                IntentState newIntentState = INSTALL_REQ;

                try {
                    Intent prevIntent = (Intent) subops.get(0).get();
                    IntentState prevIntentState = (IntentState) subops.get(1).get();

                    if (prevIntent != null || prevIntentState != null) {
                        log.warn("Overwriting existing Intent: {}@{} with {}@{}",
                                 prevIntent, prevIntentState,
                                 intent, newIntentState);
                    }
                    events.add(IntentEvent.getEvent(INSTALL_REQ, intent));
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
                    if (prevInstallable != null) {
                        log.warn("Intent {} removed installable still found", intentId);
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
    }

    public final class RemoteIntentsListener extends EntryAdapter<IntentId, Intent> {

        @Override
        public void entryAdded(EntryEvent<IntentId, Intent> event) {
            localIntents.put(event.getKey(), event.getValue());
        }

        @Override
        public void entryUpdated(EntryEvent<IntentId, Intent> event) {
            entryAdded(event);
        }
    }

    public final class RemoteIntentStateListener extends EntryAdapter<IntentId, IntentState> {

        @Override
        public void onEntryEvent(EntryEvent<IntentId, IntentState> event) {
            final IntentId intentId = event.getKey();
            final Member myself = theInstance.getCluster().getLocalMember();
            if (!myself.equals(event.getMember())) {
                // When Intent state was modified by remote node,
                // clear local transient state.
                IntentState oldState = transientStates.remove(intentId);
                if (oldState != null) {
                    log.debug("{} state updated remotely, removing transient state {}",
                              intentId, oldState);
                }

                if (event.getValue() != null) {
                    // notify if this is not entry removed event

                    final Intent intent = getIntent(intentId);
                    if (intent == null) {
                        log.warn("no Intent found for {} on Event {}", intentId, event);
                        return;
                    }
                    notifyDelegate(IntentEvent.getEvent(event.getValue(), intent));
                    // remove IntentCache
                    localIntents.remove(intentId, intent);
                }
            }

            // populate manual near cache, to prepare for
            // transition event to WITHDRAWN
            getIntent(intentId);
        }
    }
}
