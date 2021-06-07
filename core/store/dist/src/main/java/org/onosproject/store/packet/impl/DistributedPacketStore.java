/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.store.packet.impl;

import com.google.common.collect.Lists;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketEvent;
import org.onosproject.net.packet.PacketEvent.Type;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketStore;
import org.onosproject.net.packet.PacketStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMultimap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.store.OsgiPropertyConstants.DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE;
import static org.onosproject.store.OsgiPropertyConstants.DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed packet store implementation allowing packets to be sent to
 * remote instances.
 */
@Component(
        immediate = true,
        service = PacketStore.class,
        property = {
                DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE + ":Integer=" + DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT
        }
)
public class DistributedPacketStore
        extends AbstractStore<PacketEvent, PacketStoreDelegate>
        implements PacketStore {

    private final Logger log = getLogger(getClass());

    private static final String FORMAT = "Setting: messageHandlerThreadPoolSize={}";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService communicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    private PacketRequestTracker tracker;

    private static final MessageSubject PACKET_OUT_SUBJECT =
            new MessageSubject("packet-out");

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

    private ExecutorService messageHandlingExecutor;

    /** Size of thread pool to assign message handler. */
    private static int messageHandlerThreadPoolSize = DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;

    private static final int MAX_BACKOFF = 50;

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());

        modified(context);

        messageHandlingExecutor = Executors.newFixedThreadPool(
                messageHandlerThreadPoolSize,
                groupedThreads("onos/store/packet", "message-handlers", log));

        communicationService.<OutboundPacket>addSubscriber(PACKET_OUT_SUBJECT,
                SERIALIZER::decode,
                packet -> notifyDelegate(new PacketEvent(Type.EMIT, packet)),
                messageHandlingExecutor);

        tracker = new PacketRequestTracker();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        communicationService.removeSubscriber(PACKET_OUT_SUBJECT);
        messageHandlingExecutor.shutdown();
        tracker = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        int newMessageHandlerThreadPoolSize;

        try {
            String s = get(properties, DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE);

            newMessageHandlerThreadPoolSize =
                    isNullOrEmpty(s) ? messageHandlerThreadPoolSize : Integer.parseInt(s.trim());

        } catch (NumberFormatException e) {
            log.warn(e.getMessage());
            newMessageHandlerThreadPoolSize = messageHandlerThreadPoolSize;
        }

        // Any change in the following parameters implies thread pool restart
        if (newMessageHandlerThreadPoolSize != messageHandlerThreadPoolSize) {
            setMessageHandlerThreadPoolSize(newMessageHandlerThreadPoolSize);
            restartMessageHandlerThreadPool();
        }

        log.info(FORMAT, messageHandlerThreadPoolSize);
    }


    @Override
    public void emit(OutboundPacket packet) {
        NodeId myId = clusterService.getLocalNode().id();
        NodeId master = mastershipService.getMasterFor(packet.sendThrough());

        if (master == null) {
            return;
        }

        if (myId.equals(master)) {
            notifyDelegate(new PacketEvent(Type.EMIT, packet));
            return;
        }

        communicationService.unicast(packet, PACKET_OUT_SUBJECT, SERIALIZER::encode, master)
                            .whenComplete((r, error) -> {
                                if (error != null) {
                                    log.warn("Failed to send packet-out to {}", master, error);
                                }
                            });
    }

    @Override
    public void requestPackets(PacketRequest request) {
        tracker.add(request);
    }

    @Override
    public void cancelPackets(PacketRequest request) {
        tracker.remove(request);
    }

    @Override
    public List<PacketRequest> existingRequests() {
        return tracker.requests();
    }

    private final class PacketRequestTracker {

        private ConsistentMultimap<RequestKey, PacketRequest> requests;

        private PacketRequestTracker() {
            requests = storageService.<RequestKey, PacketRequest>consistentMultimapBuilder()
                    .withName("onos-packet-requests")
                    .withSerializer(Serializer.using(KryoNamespace.newBuilder()
                            .register(KryoNamespaces.API)
                            .register(RequestKey.class)
                            .build()))
                    .build();
        }

        private void add(PacketRequest request) {
            boolean firstRequest = addInternal(request);
            if (firstRequest && delegate != null) {
                // The instance that makes the first request will push to all devices
                delegate.requestPackets(request);
            }
        }

        private boolean addInternal(PacketRequest request) {
            return requests.put(key(request), request);
        }

        private void remove(PacketRequest request) {
            boolean removedLast = removeInternal(request);
            if (removedLast && delegate != null) {
                // The instance that removes the last request will remove from all devices
                delegate.cancelPackets(request);
            }
        }

        private boolean removeInternal(PacketRequest request) {
            Collection<? extends PacketRequest> values =
                Versioned.valueOrNull(requests.removeAndGet(key(request), request));
            return values == null || values.isEmpty();
        }

        private List<PacketRequest> requests() {
            List<PacketRequest> list = Lists.newArrayList();
            requests.forEach(entry -> list.add(entry.getValue()));
            list.sort((o1, o2) -> o1.priority().priorityValue() - o2.priority().priorityValue());
            return list;
        }
    }

    /**
     * Creates a new request key from a packet request.
     *
     * @param request packet request
     * @return request key
     */
    private static RequestKey key(PacketRequest request) {
        return new RequestKey(request.selector(), request.priority());
    }

    /**
     * Key of a packet request.
     */
    private static final class RequestKey {
        private final TrafficSelector selector;
        private final PacketPriority priority;

        private RequestKey(TrafficSelector selector, PacketPriority priority) {
            this.selector = selector;
            this.priority = priority;
        }

        @Override
        public int hashCode() {
            return Objects.hash(selector, priority);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            if (!(other instanceof RequestKey)) {
                return false;
            }

            RequestKey that = (RequestKey) other;

            return Objects.equals(selector, that.selector) &&
                    Objects.equals(priority, that.priority);
        }
    }

    /**
     * Sets thread pool size of message handler.
     *
     * @param poolSize
     */
    private void setMessageHandlerThreadPoolSize(int poolSize) {
        checkArgument(poolSize >= 0, "Message handler pool size must be 0 or more");
        messageHandlerThreadPoolSize = poolSize;
    }

    /**
     * Restarts thread pool of message handler.
     */
    private void restartMessageHandlerThreadPool() {
        ExecutorService prevExecutor = messageHandlingExecutor;
        messageHandlingExecutor = newFixedThreadPool(getMessageHandlerThreadPoolSize(),
                                                     groupedThreads("DistPktStore", "messageHandling-%d", log));
        prevExecutor.shutdown();
    }

    /**
     * Gets current thread pool size of message handler.
     *
     * @return messageHandlerThreadPoolSize
     */
    private int getMessageHandlerThreadPoolSize() {
        return messageHandlerThreadPoolSize;
    }
}
