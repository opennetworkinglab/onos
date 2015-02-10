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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.intent.BatchWrite;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentOperation;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.impl.EventuallyConsistentMap;
import org.onosproject.store.impl.EventuallyConsistentMapEvent;
import org.onosproject.store.impl.EventuallyConsistentMapImpl;
import org.onosproject.store.impl.EventuallyConsistentMapListener;
import org.onosproject.store.impl.WallclockClockManager;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.intent.IntentState.INSTALL_REQ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of Intents in a distributed data store that uses optimistic
 * replication and gossip based techniques.
 */
@Component(immediate = false, enabled = false)
@Service
public class GossipIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());

    private EventuallyConsistentMap<IntentId, Intent> intents;

    private EventuallyConsistentMap<IntentId, IntentState> intentStates;

    private EventuallyConsistentMap<IntentId, List<Intent>> installables;

    // Map of intent key => pending intent operation
    private EventuallyConsistentMap<String, IntentOperation> pending;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PartitionService partitionService;

    @Activate
    public void activate() {
        KryoNamespace.Builder intentSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API);
        intents = new EventuallyConsistentMapImpl<>("intents", clusterService,
                                                clusterCommunicator,
                                                intentSerializer,
                                                new WallclockClockManager<>());

        intentStates = new EventuallyConsistentMapImpl<>("intent-states",
                                                       clusterService,
                                                       clusterCommunicator,
                                                       intentSerializer,
                                                       new WallclockClockManager<>());

        installables = new EventuallyConsistentMapImpl<>("intent-installables",
                                                         clusterService,
                                                         clusterCommunicator,
                                                         intentSerializer,
                                                         new WallclockClockManager<>());

        pending = new EventuallyConsistentMapImpl<>("intent-pending",
                                                    clusterService,
                                                    clusterCommunicator,
                                                    intentSerializer, // TODO
                                                    new WallclockClockManager<>());

        intentStates.addListener(new InternalIntentStatesListener());
        pending.addListener(new InternalPendingListener());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        intents.destroy();
        intentStates.destroy();
        installables.destroy();
        pending.destroy();

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
    public Intent getIntent(Key intentKey) {
        // TODO: Implement this
        return null;
    }


    public Intent getIntent(IntentId intentId) {
        return intents.get(intentId);
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        // TODO: implement this
        return IntentState.FAILED;
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        // TODO: implement this or delete class
        return null;
        /*
        return installables.get(intentId);
        */
    }

    @Override
    public List<BatchWrite.Operation> batchWrite(BatchWrite batch) {

        List<BatchWrite.Operation> failed = new ArrayList<>();

        for (BatchWrite.Operation op : batch.operations()) {
            switch (op.type()) {
            case CREATE_INTENT:
                checkArgument(op.args().size() == 1,
                              "CREATE_INTENT takes 1 argument. %s", op);
                Intent intent = op.arg(0);

                intents.put(intent.id(), intent);
                intentStates.put(intent.id(), INSTALL_REQ);

                // TODO remove from pending?


                break;
            case REMOVE_INTENT:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INTENT takes 1 argument. %s", op);
                IntentId intentId = op.arg(0);

                intents.remove(intentId);
                intentStates.remove(intentId);
                installables.remove(intentId);

                break;
            case SET_STATE:
                checkArgument(op.args().size() == 2,
                              "SET_STATE takes 2 arguments. %s", op);
                intent = op.arg(0);
                IntentState newState = op.arg(1);

                intentStates.put(intent.id(), newState);

                break;
            case SET_INSTALLABLE:
                checkArgument(op.args().size() == 2,
                              "SET_INSTALLABLE takes 2 arguments. %s", op);
                intentId = op.arg(0);
                List<Intent> installableIntents = op.arg(1);

                installables.put(intentId, installableIntents);

                break;
            case REMOVE_INSTALLED:
                checkArgument(op.args().size() == 1,
                              "REMOVE_INSTALLED takes 1 argument. %s", op);
                intentId = op.arg(0);
                installables.remove(intentId);
                break;
            default:
                log.warn("Unknown Operation encountered: {}", op);
                failed.add(op);
                break;
            }
        }

        return failed;
    }

    @Override
    public void write(IntentData newData) {
        // TODO
    }

    @Override
    public void batchWrite(Iterable<IntentData> updates) {
        // TODO
    }

    @Override
    public IntentData getIntentData(Key key) {
        return null; // TODO
    }

    @Override
    public void addPending(IntentData data) {
        // TODO implement

        // Check the intent versions
        //pending.put(op.key(), op);
    }

    @Override
    public boolean isMaster(Intent intent) {
        // TODO
        //return partitionService.isMine(intent.key());
        return false;
    }

    private void notifyDelegateIfNotNull(IntentEvent event) {
        if (event != null) {
            notifyDelegate(event);
        }
    }

    private final class InternalIntentStatesListener implements
            EventuallyConsistentMapListener<IntentId, IntentState> {
        @Override
        public void event(
                EventuallyConsistentMapEvent<IntentId, IntentState> event) {
            if (event.type() == EventuallyConsistentMapEvent.Type.PUT) {
                IntentEvent externalEvent;
                Intent intent = intents.get(event.key()); // TODO OK if this is null?

                try {
                    externalEvent = IntentEvent.getEvent(event.value(), intent);
                } catch (IllegalArgumentException e) {
                    externalEvent = null;
                }

                notifyDelegateIfNotNull(externalEvent);
            }
        }
    }

    private final class InternalPendingListener implements
            EventuallyConsistentMapListener<String, IntentOperation> {
        @Override
        public void event(
                EventuallyConsistentMapEvent<String, IntentOperation> event) {
            if (event.type() == EventuallyConsistentMapEvent.Type.PUT) {
                // The pending intents map has been updated. If we are master for
                // this intent's partition, notify the Manager that they should do
                // some work.
                if (isMaster(event.value().intent())) {
                    // TODO delegate.process(event.value());
                    log.debug("implement this");
                }
            }
        }
    }

}

