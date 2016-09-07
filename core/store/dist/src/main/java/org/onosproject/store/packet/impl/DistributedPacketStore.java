/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketEvent;
import org.onosproject.net.packet.PacketEvent.Type;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketStore;
import org.onosproject.net.packet.PacketStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed packet store implementation allowing packets to be sent to
 * remote instances.
 */
@Component(immediate = true)
@Service
public class DistributedPacketStore
        extends AbstractStore<PacketEvent, PacketStoreDelegate>
        implements PacketStore {

    private final Logger log = getLogger(getClass());

    private static final String FORMAT = "Setting: messageHandlerThreadPoolSize={}";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService communicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private PacketRequestTracker tracker;

    private static final MessageSubject PACKET_OUT_SUBJECT =
            new MessageSubject("packet-out");

    private static final StoreSerializer SERIALIZER = StoreSerializer.using(KryoNamespaces.API);

    private ExecutorService messageHandlingExecutor;

    private static final int DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE = 4;
    @Property(name = "messageHandlerThreadPoolSize", intValue = DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE,
            label = "Size of thread pool to assign message handler")
    private static int messageHandlerThreadPoolSize = DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE;

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
            String s = get(properties, "messageHandlerThreadPoolSize");

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

        private ConsistentMap<TrafficSelector, Set<PacketRequest>> requests;

        private PacketRequestTracker() {
            requests = storageService.<TrafficSelector, Set<PacketRequest>>consistentMapBuilder()
                    .withName("onos-packet-requests")
                    .withSerializer(Serializer.using(KryoNamespaces.API))
                    .build();
        }

        private void add(PacketRequest request) {
            AtomicBoolean firstRequest = addInternal(request);
            if (firstRequest.get() && delegate != null) {
                // The instance that makes the first request will push to all devices
                delegate.requestPackets(request);
            }
        }

        private AtomicBoolean addInternal(PacketRequest request) {
            AtomicBoolean firstRequest = new AtomicBoolean(false);
            requests.compute(request.selector(), (s, existingRequests) -> {
                // Reset to false just in case this is a retry due to
                // ConcurrentModificationException
                firstRequest.set(false);
                if (existingRequests == null) {
                    firstRequest.set(true);
                    return ImmutableSet.of(request);
                } else if (!existingRequests.contains(request)) {
                    firstRequest.set(true);
                    return ImmutableSet.<PacketRequest>builder()
                                       .addAll(existingRequests)
                                       .add(request)
                                       .build();
                } else {
                    return existingRequests;
                }
            });
            return firstRequest;
        }

        private void remove(PacketRequest request) {
            AtomicBoolean removedLast = removeInternal(request);
            if (removedLast.get() && delegate != null) {
                // The instance that removes the last request will remove from all devices
                delegate.cancelPackets(request);
            }
        }

        private AtomicBoolean removeInternal(PacketRequest request) {
            AtomicBoolean removedLast = new AtomicBoolean(false);
            requests.computeIfPresent(request.selector(), (s, existingRequests) -> {
                // Reset to false just in case this is a retry due to
                // ConcurrentModificationException
                removedLast.set(false);
                if (existingRequests.contains(request)) {
                    Set<PacketRequest> newRequests = Sets.newHashSet(existingRequests);
                    newRequests.remove(request);
                    if (newRequests.size() > 0) {
                        return ImmutableSet.copyOf(newRequests);
                    } else {
                        removedLast.set(true);
                        return null;
                    }
                } else {
                    return existingRequests;
                }
            });
            return removedLast;
        }

        private List<PacketRequest> requests() {
            List<PacketRequest> list = Lists.newArrayList();
            requests.values().forEach(v -> list.addAll(v.value()));
            list.sort((o1, o2) -> o1.priority().priorityValue() - o2.priority().priorityValue());
            return list;
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
