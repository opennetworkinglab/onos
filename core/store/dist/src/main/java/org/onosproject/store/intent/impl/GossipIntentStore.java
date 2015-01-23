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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.intent.BatchWrite;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentClockService;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.impl.Timestamped;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.impl.DistributedStoreSerializers;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.minPriority;
import static org.onlab.util.Tools.namedThreads;
import static org.onosproject.net.intent.IntentState.INSTALL_REQ;
import static org.onosproject.store.intent.impl.GossipIntentStoreMessageSubjects.INTENT_ANTI_ENTROPY_ADVERTISEMENT;
import static org.onosproject.store.intent.impl.GossipIntentStoreMessageSubjects.INTENT_SET_INSTALLABLES_MSG;
import static org.onosproject.store.intent.impl.GossipIntentStoreMessageSubjects.INTENT_UPDATED_MSG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of Intents in a distributed data store that uses optimistic
 * replication and gossip based techniques.
 */
@Component(immediate = true, enabled = false)
@Service
public class GossipIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());

    private final ConcurrentMap<IntentId, Intent> intents =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<IntentId, Timestamped<IntentState>> intentStates
            = new ConcurrentHashMap<>();

    private final Set<IntentId> withdrawRequestedIntents
            = Sets.newConcurrentHashSet();

    private ConcurrentMap<IntentId, Timestamped<List<Intent>>> installables
            = new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentClockService intentClockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(InternalIntentEvent.class)
                    .register(InternalSetInstallablesEvent.class)
                    //.register(InternalIntentAntiEntropyEvent.class)
                    //.register(IntentAntiEntropyAdvertisement.class)
                    .build();
        }
    };

    private ExecutorService executor;

    private ScheduledExecutorService backgroundExecutor;

    // TODO: Make these anti-entropy params configurable
    private long initialDelaySec = 5;
    private long periodSec = 5;

    @Activate
    public void activate() {
        clusterCommunicator.addSubscriber(INTENT_UPDATED_MSG,
                new InternalIntentCreateOrUpdateEventListener());
        clusterCommunicator.addSubscriber(INTENT_SET_INSTALLABLES_MSG,
                                          new InternalIntentSetInstallablesListener());
        clusterCommunicator.addSubscriber(
                INTENT_ANTI_ENTROPY_ADVERTISEMENT,
                new InternalIntentAntiEntropyAdvertisementListener());

        executor = Executors.newCachedThreadPool(namedThreads("intent-fg-%d"));

        backgroundExecutor =
                newSingleThreadScheduledExecutor(minPriority(namedThreads("intent-bg-%d")));

        // start anti-entropy thread
        //backgroundExecutor.scheduleAtFixedRate(new SendAdvertisementTask(),
                    //initialDelaySec, periodSec, TimeUnit.SECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        executor.shutdownNow();
        backgroundExecutor.shutdownNow();
        try {
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.error("Timeout during executor shutdown");
            }
        } catch (InterruptedException e) {
            log.error("Error during executor shutdown", e);
        }

        intents.clear();

        log.info("Stopped");
    }

    @Override
    public long getIntentCount() {
        return intents.size();
    }

    @Override
    public Iterable<Intent> getIntents() {
        // TODO don't actually need to copy intents, they are immutable
        return ImmutableList.copyOf(intents.values());
    }

    @Override
    public Intent getIntent(IntentId intentId) {
        return intents.get(intentId);
    }

    @Override
    public IntentState getIntentState(IntentId intentId) {
        Timestamped<IntentState> state = intentStates.get(intentId);
        if (state != null) {
            return state.value();
        }
        return null;
    }

    private IntentEvent setStateInternal(IntentId intentId, IntentState newState, Timestamp timestamp) {
        switch (newState) {
        case WITHDRAW_REQ:
            withdrawRequestedIntents.add(intentId);
            break;
        case INSTALL_REQ:
        case COMPILING:
        case INSTALLING:
        case INSTALLED:
        case RECOMPILING:
        case WITHDRAWING:
        case WITHDRAWN:
        case FAILED:
            synchronized (intentStates) {
                Timestamped<IntentState> existing = intentStates.get(intentId);
                if (existing == null || !existing.isNewer(timestamp)) {
                    intentStates.put(intentId, new Timestamped<>(newState, timestamp));
                }
            }
            break;
        default:
            log.warn("Unknown intent state {}", newState);
            break;
        }

        try {
            // TODO make sure it's OK if the intent is null
            return IntentEvent.getEvent(newState, intents.get(intentId));
        } catch (IllegalArgumentException e) {
            // Transient states can't be used for events, so don't send one
            return null;
        }
    }

    private void setInstallableIntentsInternal(IntentId intentId,
                                               List<Intent> installableIntents,
                                               Timestamp timestamp) {
        synchronized (installables) {
            Timestamped<List<Intent>> existing = installables.get(intentId);
            if (existing == null || !existing.isNewer(timestamp)) {
                installables.put(intentId,
                                 new Timestamped<>(installableIntents, timestamp));
            }
        }
    }

    @Override
    public List<Intent> getInstallableIntents(IntentId intentId) {
        Timestamped<List<Intent>> tInstallables = installables.get(intentId);
        if (tInstallables != null) {
            return tInstallables.value();
        }
        return null;
    }

    @Override
    public List<BatchWrite.Operation> batchWrite(BatchWrite batch) {

        List<IntentEvent> events = Lists.newArrayList();
        List<BatchWrite.Operation> failed = new ArrayList<>();

        for (BatchWrite.Operation op : batch.operations()) {
            switch (op.type()) {
            case CREATE_INTENT:
                checkArgument(op.args().size() == 1,
                              "CREATE_INTENT takes 1 argument. %s", op);
                Intent intent = op.arg(0);

                events.add(createIntentInternal(intent));
                notifyPeers(new InternalIntentEvent(
                        intent.id(), intent, INSTALL_REQ, null));

                break;
            case REMOVE_INTENT:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INTENT takes 1 argument. %s", op);
                IntentId intentId = (IntentId) op.arg(0);
                // TODO implement

                break;
            case SET_STATE:
                checkArgument(op.args().size() == 2,
                              "SET_STATE takes 2 arguments. %s", op);
                intent = op.arg(0);
                IntentState newState = op.arg(1);

                Timestamp timestamp = intentClockService.getTimestamp(
                        intent.id());
                IntentEvent externalEvent = setStateInternal(intent.id(), newState, timestamp);
                events.add(externalEvent);
                notifyPeers(new InternalIntentEvent(intent.id(), null, newState, timestamp));

                break;
            case SET_INSTALLABLE:
                checkArgument(op.args().size() == 2,
                              "SET_INSTALLABLE takes 2 arguments. %s", op);
                intentId = op.arg(0);
                List<Intent> installableIntents = op.arg(1);

                Timestamp timestamp1 = intentClockService.getTimestamp(intentId);
                setInstallableIntentsInternal(
                        intentId, installableIntents, timestamp1);

                notifyPeers(new InternalSetInstallablesEvent(intentId, installableIntents, timestamp1));

                break;
            case REMOVE_INSTALLED:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INSTALLED takes 1 argument. %s", op);
                intentId = op.arg(0);
                // TODO implement
                break;
            default:
                log.warn("Unknown Operation encountered: {}", op);
                failed.add(op);
                break;
            }
        }

        notifyDelegate(events);
        return failed;
    }

    private IntentEvent createIntentInternal(Intent intent) {
        Intent oldValue = intents.putIfAbsent(intent.id(), intent);
        if (oldValue == null) {
            return IntentEvent.getEvent(INSTALL_REQ, intent);
        }

        log.warn("Intent ID {} already in store, throwing new update away",
                 intent.id());
        return null;
    }

    private void notifyPeers(InternalIntentEvent event) {
        try {
            broadcastMessage(INTENT_UPDATED_MSG, event);
        } catch (IOException e) {
            // TODO this won't happen; remove from API
            log.debug("IOException broadcasting update", e);
        }
    }

    private void notifyPeers(InternalSetInstallablesEvent event) {
        try {
            broadcastMessage(INTENT_SET_INSTALLABLES_MSG, event);
        } catch (IOException e) {
            // TODO this won't happen; remove from API
            log.debug("IOException broadcasting update", e);
        }
    }

    private void broadcastMessage(MessageSubject subject, Object event) throws
            IOException {
        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                subject,
                SERIALIZER.encode(event));
        clusterCommunicator.broadcast(message);
    }

    private void unicastMessage(NodeId peer,
                                MessageSubject subject,
                                Object event) throws IOException {
        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                subject,
                SERIALIZER.encode(event));
        clusterCommunicator.unicast(message, peer);
    }

    private void notifyDelegateIfNotNull(IntentEvent event) {
        if (event != null) {
            notifyDelegate(event);
        }
    }

    private final class InternalIntentCreateOrUpdateEventListener
            implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {

            log.debug("Received intent update event from peer: {}", message.sender());
            InternalIntentEvent event = SERIALIZER.decode(message.payload());

            IntentId intentId = event.intentId();
            Intent intent = event.intent();
            IntentState state = event.state();
            Timestamp timestamp = event.timestamp();

            executor.submit(() -> {
                try {
                    switch (state) {
                    case INSTALL_REQ:
                        notifyDelegateIfNotNull(createIntentInternal(intent));
                        break;
                    default:
                        notifyDelegateIfNotNull(setStateInternal(intentId, state, timestamp));
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Exception thrown handling intent create or update", e);
                }
            });
        }
    }

    private final class InternalIntentSetInstallablesListener
            implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            log.debug("Received intent set installables event from peer: {}", message.sender());
            InternalSetInstallablesEvent event = SERIALIZER.decode(message.payload());

            IntentId intentId = event.intentId();
            List<Intent> installables = event.installables();
            Timestamp timestamp = event.timestamp();

            executor.submit(() -> {
                try {
                    setInstallableIntentsInternal(intentId, installables, timestamp);
                } catch (Exception e) {
                    log.warn("Exception thrown handling intent set installables", e);
                }
            });
        }
    }

    private final class InternalIntentAntiEntropyAdvertisementListener
            implements ClusterMessageHandler {

        @Override
        public void handle(ClusterMessage message) {
            log.trace("Received intent Anti-Entropy advertisement from peer: {}", message.sender());
            // TODO implement
            //IntentAntiEntropyAdvertisement advertisement = SERIALIZER.decode(message.payload());
            backgroundExecutor.submit(() -> {
                try {
                    log.debug("something");
                    //handleAntiEntropyAdvertisement(advertisement);
                } catch (Exception e) {
                    log.warn("Exception thrown handling intent advertisements", e);
                }
            });
        }
    }
}

