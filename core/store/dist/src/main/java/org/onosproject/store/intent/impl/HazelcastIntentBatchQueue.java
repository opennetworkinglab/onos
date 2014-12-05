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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentBatchLeaderEvent;
import org.onosproject.net.intent.IntentBatchListener;
import org.onosproject.net.intent.IntentBatchService;
import org.onosproject.net.intent.IntentOperations;
import org.onosproject.store.hz.SQueue;
import org.onosproject.store.hz.StoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;

@Component(immediate = true)
@Service
public class HazelcastIntentBatchQueue
        implements IntentBatchService {

    private final Logger log = getLogger(getClass());
    private static final String TOPIC_BASE = "intent-batch-";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StoreService storeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;


    private HazelcastInstance theInstance;
    private NodeId localControllerNodeId;
    protected StoreSerializer serializer;
    private IntentBatchDelegate delegate;
    private InternalLeaderListener leaderListener = new InternalLeaderListener();
    private final Map<ApplicationId, SQueue<IntentOperations>> batchQueues
            = Maps.newHashMap();
    private final Set<ApplicationId> myTopics = Sets.newHashSet();
    private final Map<ApplicationId, IntentOperations> outstandingOps
            = Maps.newHashMap();

    private final AbstractListenerRegistry<IntentBatchLeaderEvent, IntentBatchListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    @Activate
    public void activate() {
        theInstance = storeService.getHazelcastInstance();
        localControllerNodeId = clusterService.getLocalNode().id();
        leadershipService.addListener(leaderListener);

        serializer = new KryoSerializer() {

            @Override
            protected void setupKryoPool() {
                serializerPool = KryoNamespace.newBuilder()
                        .setRegistrationRequired(false)
                        .register(KryoNamespaces.API)
                        .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                        .build();
            }

        };
        eventDispatcher.addSink(IntentBatchLeaderEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(IntentBatchLeaderEvent.class);
        leadershipService.removeListener(leaderListener);
        for (ApplicationId appId: batchQueues.keySet()) {
            leadershipService.withdraw(getTopic(appId));
        }
        log.info("Stopped");
    }

    public static String getTopic(ApplicationId appId) {
        return TOPIC_BASE + checkNotNull(appId.id());
    }

    public ApplicationId getAppId(String topic) {
        checkState(topic.startsWith(TOPIC_BASE),
                   "Trying to get app id for invalid topic: {}", topic);
        Short id = Short.parseShort(topic.substring(TOPIC_BASE.length()));
        return coreService.getAppId(id);
    }

    private SQueue<IntentOperations> getQueue(ApplicationId appId) {
        SQueue<IntentOperations> queue = batchQueues.get(appId);
        if (queue == null) {
            synchronized (this) {
                String topic = getTopic(appId);
                IQueue<byte[]> rawQueue = theInstance.getQueue(topic);
                queue = new SQueue<>(rawQueue, serializer);
                queue.addItemListener(new InternalItemListener(appId), false);
                batchQueues.putIfAbsent(appId, queue);
                leadershipService.runForLeadership(topic);
            }
        }
        return queue;
    }

    @Override
    public void addIntentOperations(IntentOperations ops) {
        checkNotNull(ops, "Intent operations cannot be null.");
        ApplicationId appId = ops.appId();
        getQueue(appId).add(ops);
        dispatchNextOperation(appId);
    }

    @Override
    public void removeIntentOperations(IntentOperations ops) {
        ApplicationId appId = ops.appId();
        synchronized (this) {
            IntentOperations outstanding = outstandingOps.get(appId);
            if (outstanding != null) {
                checkState(Objects.equals(ops, outstanding),
                           "Operation {} does not match outstanding operation {}",
                            ops, outstanding);
            } else {
                log.warn("Operation {} not found", ops);
            }
            SQueue<IntentOperations> queue = batchQueues.get(appId);
            checkState(queue.remove().equals(ops),
                       "Operations are wrong.");
            outstandingOps.remove(appId);
            dispatchNextOperation(appId);
        }
    }

    /**
     * Dispatches the next available operations to the delegate, unless
     * we are not the leader for this application id or there is an
     * outstanding operations for this application id.
     *
     * @param appId application id
     */
    private void dispatchNextOperation(ApplicationId appId) {
        synchronized (this) {
            if (!myTopics.contains(appId) ||
                    outstandingOps.containsKey(appId)) {
                return;
            }
            IntentOperations ops = batchQueues.get(appId).peek();
            if (ops != null) {
                outstandingOps.put(appId, ops);
                delegate.execute(ops);
            }
        }
    }

    /**
     * Record the leadership change for the given topic. If we have become the
     * leader, then dispatch the next operations. If we have lost leadership,
     * then cancel the last operations.
     *
     * @param topic topic based on application id
     * @param leader true if we have become the leader, false otherwise
     */
    private void leaderChanged(String topic, boolean leader) {
        ApplicationId appId = getAppId(topic);
        synchronized (this) {
            if (leader) {
                myTopics.add(appId);
                checkState(!outstandingOps.containsKey(appId),
                           "Existing intent ops for app id: {}", appId);
                dispatchNextOperation(appId);
            } else {
                myTopics.remove(appId);
                IntentOperations ops = outstandingOps.get(appId);
                if (ops != null) {
                    delegate.cancel(ops);
                }
                outstandingOps.remove(appId);
            }
        }
    }

    private class InternalItemListener implements ItemListener<IntentOperations> {

        private final ApplicationId appId;

        public InternalItemListener(ApplicationId appId) {
            this.appId = appId;
        }

        @Override
        public void itemAdded(ItemEvent<IntentOperations> item) {
            dispatchNextOperation(appId);
        }

        @Override
        public void itemRemoved(ItemEvent<IntentOperations> item) {
            // no-op
        }
    }

    private class InternalLeaderListener implements LeadershipEventListener {
        @Override
        public void event(LeadershipEvent event) {
            log.trace("Leadership Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);

            String topic = event.subject().topic();
            if (!topic.startsWith(TOPIC_BASE)) {
                return;         // Not our topic: ignore
            }
            if (!event.subject().leader().equals(localControllerNodeId)) {
                // run for leadership
                getQueue(getAppId(topic));
                return;         // The event is not about this instance: ignore
            }

            switch (event.type()) {
                case LEADER_ELECTED:
                    log.info("Elected leader for app {}", getAppId(topic));
                    leaderChanged(topic, true);
                    break;
                case LEADER_BOOTED:
                    log.info("Lost leader election for app {}", getAppId(topic));
                    leaderChanged(topic, false);
                    break;
                case LEADER_REELECTED:
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public Set<IntentOperations> getPendingOperations() {
        Set<IntentOperations> ops = Sets.newHashSet();
        synchronized (this) {
            for (SQueue<IntentOperations> queue : batchQueues.values()) {
                ops.addAll(queue);
            }
            return ops;
        }
    }

    @Override
    public boolean isLocalLeader(ApplicationId applicationId) {
        return myTopics.contains(applicationId);
    }

    @Override
    public void setDelegate(IntentBatchDelegate delegate) {
        this.delegate = checkNotNull(delegate, "Delegate cannot be null");
    }

    @Override
    public void unsetDelegate(IntentBatchDelegate delegate) {
        if (this.delegate != null && this.delegate.equals(delegate)) {
            this.delegate = null;
        }
    }

    @Override
    public void addListener(IntentBatchListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(IntentBatchListener listener) {
        listenerRegistry.removeListener(listener);
    }
}
