/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.store.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkPacketStore;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketEvent;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketStoreDelegate;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import static org.onosproject.incubator.net.virtual.store.impl.OsgiPropertyConstants.MESSAGE_HANDLER_THREAD_POOL_SIZE;
import static org.onosproject.incubator.net.virtual.store.impl.OsgiPropertyConstants.MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;
import static org.onosproject.net.packet.PacketEvent.Type.EMIT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed virtual packet store implementation allowing packets to be sent to
 * remote instances.  Implementation is based on DistributedPacketStore class.
 */
@Component(immediate = true, enabled = false, service = VirtualNetworkPacketStore.class,
        property = {
                 MESSAGE_HANDLER_THREAD_POOL_SIZE + ":Integer=" + MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT,
        })
public class DistributedVirtualPacketStore
        extends AbstractVirtualStore<PacketEvent, PacketStoreDelegate>
        implements VirtualNetworkPacketStore {

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
            new MessageSubject("virtual-packet-out");

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

    private ExecutorService messageHandlingExecutor;

    /** Size of thread pool to assign message handler. */
    private static int messageHandlerThreadPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());

        modified(context);

        messageHandlingExecutor = Executors.newFixedThreadPool(
                messageHandlerThreadPoolSize,
                groupedThreads("onos/store/packet", "message-handlers", log));

        communicationService.<OutboundPacketWrapper>addSubscriber(PACKET_OUT_SUBJECT,
                SERIALIZER::decode,
                packetWrapper -> notifyDelegate(packetWrapper.networkId,
                                                new PacketEvent(EMIT,
                                                                packetWrapper.outboundPacket)),
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
    public void  modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        int newMessageHandlerThreadPoolSize;

        try {
            String s = get(properties, MESSAGE_HANDLER_THREAD_POOL_SIZE);

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
    public void emit(NetworkId networkId, OutboundPacket packet) {
        NodeId myId = clusterService.getLocalNode().id();
        // TODO revive this when there is MastershipService support for virtual devices
//        NodeId master = mastershipService.getMasterFor(packet.sendThrough());
//
//        if (master == null) {
//            log.warn("No master found for {}", packet.sendThrough());
//            return;
//        }
//
//        log.debug("master {} found for {}", myId, packet.sendThrough());
//        if (myId.equals(master)) {
//            notifyDelegate(networkId, new PacketEvent(EMIT, packet));
//            return;
//        }
//
//        communicationService.unicast(packet, PACKET_OUT_SUBJECT, SERIALIZER::encode, master)
//                            .whenComplete((r, error) -> {
//                                if (error != null) {
//                                    log.warn("Failed to send packet-out to {}", master, error);
//                                }
//                            });
    }

    @Override
    public void requestPackets(NetworkId networkId, PacketRequest request) {
        tracker.add(networkId, request);

    }

    @Override
    public void cancelPackets(NetworkId networkId, PacketRequest request) {
        tracker.remove(networkId, request);
    }

    @Override
    public List<PacketRequest> existingRequests(NetworkId networkId) {
        return tracker.requests(networkId);
    }

    private final class PacketRequestTracker {

        private ConsistentMap<NetworkId, Map<RequestKey, Set<PacketRequest>>> distRequests;
        private Map<NetworkId, Map<RequestKey, Set<PacketRequest>>> requests;

        private PacketRequestTracker() {
            distRequests = storageService.<NetworkId, Map<RequestKey, Set<PacketRequest>>>consistentMapBuilder()
                    .withName("onos-virtual-packet-requests")
                    .withSerializer(Serializer.using(KryoNamespace.newBuilder()
                            .register(KryoNamespaces.API)
                            .register(RequestKey.class)
                            .register(NetworkId.class)
                            .build()))
                    .build();
            requests = distRequests.asJavaMap();
        }

        private void add(NetworkId networkId, PacketRequest request) {
            AtomicBoolean firstRequest = addInternal(networkId, request);
            PacketStoreDelegate delegate = delegateMap.get(networkId);
            if (firstRequest.get() && delegate != null) {
                // The instance that makes the first request will push to all devices
                delegate.requestPackets(request);
            }
        }

        private AtomicBoolean addInternal(NetworkId networkId, PacketRequest request) {
            AtomicBoolean firstRequest = new AtomicBoolean(false);
            AtomicBoolean changed = new AtomicBoolean(true);
            Map<RequestKey, Set<PacketRequest>> requestsForNetwork = getMap(networkId);
            requestsForNetwork.compute(key(request), (s, existingRequests) -> {
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
                    changed.set(false);
                    return existingRequests;
                }
            });
            if (changed.get()) {
                requests.put(networkId, requestsForNetwork);
            }
            return firstRequest;
        }

        private void remove(NetworkId networkId, PacketRequest request) {
            AtomicBoolean removedLast = removeInternal(networkId, request);
            PacketStoreDelegate delegate = delegateMap.get(networkId);
            if (removedLast.get() && delegate != null) {
                // The instance that removes the last request will remove from all devices
                delegate.cancelPackets(request);
            }
        }

        private AtomicBoolean removeInternal(NetworkId networkId, PacketRequest request) {
            AtomicBoolean removedLast = new AtomicBoolean(false);
            AtomicBoolean changed = new AtomicBoolean(true);
            Map<RequestKey, Set<PacketRequest>> requestsForNetwork = getMap(networkId);
            requestsForNetwork.computeIfPresent(key(request), (s, existingRequests) -> {
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
                    changed.set(false);
                    return existingRequests;
                }
            });
            if (changed.get()) {
                requests.put(networkId, requestsForNetwork);
            }
            return removedLast;
        }

        private List<PacketRequest> requests(NetworkId networkId) {
            Map<RequestKey, Set<PacketRequest>> requestsForNetwork = getMap(networkId);
            List<PacketRequest> list = Lists.newArrayList();
            requestsForNetwork.values().forEach(v -> list.addAll(v));
            list.sort((o1, o2) -> o1.priority().priorityValue() - o2.priority().priorityValue());
            return list;
        }

        /*
         * Gets PacketRequests for specified networkId.
         */
        private Map<RequestKey, Set<PacketRequest>> getMap(NetworkId networkId) {
            return requests.computeIfAbsent(networkId, networkId1 -> {
                        log.debug("Creating new map for {}", networkId1);
                        Map newMap = Maps.newHashMap();
                        return newMap;
                    });
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

    private static OutboundPacketWrapper wrapper(NetworkId networkId, OutboundPacket outboundPacket) {
        return new OutboundPacketWrapper(networkId, outboundPacket);
    }

    /*
     * OutboundPacket in
     */
    private static final class OutboundPacketWrapper {
        private NetworkId networkId;
        private OutboundPacket outboundPacket;

        private OutboundPacketWrapper(NetworkId networkId, OutboundPacket outboundPacket) {
            this.networkId = networkId;
            this.outboundPacket = outboundPacket;
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
