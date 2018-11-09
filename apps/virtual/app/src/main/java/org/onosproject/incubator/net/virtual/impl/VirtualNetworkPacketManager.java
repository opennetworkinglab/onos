/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.virtual.AbstractVnetService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkPacketStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualPacketProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualPacketProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.packet.DefaultPacketRequest;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketEvent;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketProcessorEntry;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketStoreDelegate;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VirtualNetworkPacketManager extends AbstractVnetService
        implements PacketService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final VirtualNetworkService manager;

    protected VirtualNetworkPacketStore store;
    private final List<ProcessorEntry> processors = Lists.newCopyOnWriteArrayList();

    private NodeId localNodeId;

    private DeviceService deviceService;
    private FlowObjectiveService objectiveService;

    private VirtualProviderRegistryService providerRegistryService = null;

    private InternalPacketProviderService providerService = null;

    public VirtualNetworkPacketManager(VirtualNetworkService virtualNetworkManager,
                                       NetworkId networkId) {
        super(virtualNetworkManager, networkId);
        this.manager = virtualNetworkManager;

        //Set node id as same as the node hosting virtual manager
        ClusterService clusterService = serviceDirectory.get(ClusterService.class);
        this.localNodeId = clusterService.getLocalNode().id();

        this.store = serviceDirectory.get(VirtualNetworkPacketStore.class);
        this.store.setDelegate(networkId(), new InternalStoreDelegate());

        this.deviceService = manager.get(networkId(), DeviceService.class);
        this.objectiveService = manager.get(networkId(), FlowObjectiveService.class);

        providerRegistryService =
                serviceDirectory.get(VirtualProviderRegistryService.class);
        providerService = new InternalPacketProviderService();
        providerRegistryService.registerProviderService(networkId(), providerService);
    }

    @Override
    public void addProcessor(PacketProcessor processor, int priority) {
        ProcessorEntry entry = new ProcessorEntry(processor, priority);

        // Insert the new processor according to its priority.
        int i = 0;
        for (; i < processors.size(); i++) {
            if (priority < processors.get(i).priority()) {
                break;
            }
        }
        processors.add(i, entry);
    }

    @Override
    public void removeProcessor(PacketProcessor processor) {
        // Remove the processor entry.
        for (int i = 0; i < processors.size(); i++) {
            if (processors.get(i).processor() == processor) {
                processors.remove(i);
                break;
            }
        }
    }

    @Override
    public List<PacketProcessorEntry> getProcessors() {
        return ImmutableList.copyOf(processors);
    }

    @Override
    public void requestPackets(TrafficSelector selector, PacketPriority priority, ApplicationId appId) {
        PacketRequest request = new DefaultPacketRequest(selector, priority, appId,
                                                         localNodeId, Optional.empty());
        store.requestPackets(networkId(), request);
    }

    @Override
    public void requestPackets(TrafficSelector selector, PacketPriority priority,
                               ApplicationId appId, Optional<DeviceId> deviceId) {
        PacketRequest request =
                new DefaultPacketRequest(selector, priority, appId,
                                         localNodeId, deviceId);

        store.requestPackets(networkId(), request);
    }

    @Override
    public void cancelPackets(TrafficSelector selector, PacketPriority priority, ApplicationId appId) {
        PacketRequest request = new DefaultPacketRequest(selector, priority, appId,
                                                         localNodeId, Optional.empty());
        store.cancelPackets(networkId(), request);
    }

    @Override
    public void cancelPackets(TrafficSelector selector, PacketPriority priority,
                              ApplicationId appId, Optional<DeviceId> deviceId) {
        PacketRequest request = new DefaultPacketRequest(selector, priority,
                                                         appId, localNodeId,
                                                         deviceId);
        store.cancelPackets(networkId(), request);
    }

    @Override
    public List<PacketRequest> getRequests() {
        return store.existingRequests(networkId());
    }

    @Override
    public void emit(OutboundPacket packet) {
        store.emit(networkId(), packet);
    }

    /**
     * Personalized packet provider service issued to the supplied provider.
     */
    private class InternalPacketProviderService
            extends AbstractVirtualProviderService<VirtualPacketProvider>
            implements VirtualPacketProviderService {

        protected InternalPacketProviderService() {
            super();

            Set<ProviderId> providerIds =
                    providerRegistryService.getProvidersByService(this);
            ProviderId providerId = providerIds.stream().findFirst().get();
            VirtualPacketProvider provider = (VirtualPacketProvider)
                    providerRegistryService.getProvider(providerId);
            setProvider(provider);
        }

        @Override
        public void processPacket(PacketContext context) {
            // TODO filter packets sent to processors based on registrations
            for (ProcessorEntry entry : processors) {
                try {
                    long start = System.nanoTime();
                    entry.processor().process(context);
                    entry.addNanos(System.nanoTime() - start);
                } catch (Exception e) {
                    log.warn("Packet processor {} threw an exception", entry.processor(), e);
                }
            }
        }

    }

    /**
     * Entity for tracking stats for a packet processor.
     */
    private class ProcessorEntry implements PacketProcessorEntry {
        private final PacketProcessor processor;
        private final int priority;
        private long invocations = 0;
        private long nanos = 0;

        public ProcessorEntry(PacketProcessor processor, int priority) {
            this.processor = processor;
            this.priority = priority;
        }

        @Override
        public PacketProcessor processor() {
            return processor;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public long invocations() {
            return invocations;
        }

        @Override
        public long totalNanos() {
            return nanos;
        }

        @Override
        public long averageNanos() {
            return invocations > 0 ? nanos / invocations : 0;
        }

        void addNanos(long nanos) {
            this.nanos += nanos;
            this.invocations++;
        }
    }

    private void localEmit(NetworkId networkId, OutboundPacket packet) {
        Device device = deviceService.getDevice(packet.sendThrough());
        if (device == null) {
            return;
        }
        VirtualPacketProvider packetProvider = providerService.provider();

        if (packetProvider != null) {
            packetProvider.emit(networkId, packet);
        }
    }

    /**
     * Internal callback from the packet store.
     */
    protected class InternalStoreDelegate implements PacketStoreDelegate {
        @Override
        public void notify(PacketEvent event) {
            localEmit(networkId(), event.subject());
        }

        @Override
        public void requestPackets(PacketRequest request) {
            DeviceId deviceid = request.deviceId().orElse(null);

            if (deviceid != null) {
                pushRule(deviceService.getDevice(deviceid), request);
            } else {
                pushToAllDevices(request);
            }
        }

        @Override
        public void cancelPackets(PacketRequest request) {
            DeviceId deviceid = request.deviceId().orElse(null);

            if (deviceid != null) {
                removeRule(deviceService.getDevice(deviceid), request);
            } else {
                removeFromAllDevices(request);
            }
        }
    }

    /**
     * Pushes packet intercept flow rules to the device.
     *
     * @param device  the device to push the rules to
     * @param request the packet request
     */
    private void pushRule(Device device, PacketRequest request) {
        if (!device.type().equals(Device.Type.VIRTUAL)) {
            return;
        }

        ForwardingObjective forwarding = createBuilder(request)
                .add(new ObjectiveContext() {
                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        log.warn("Failed to install packet request {} to {}: {}",
                                 request, device.id(), error);
                    }
                });

        objectiveService.forward(device.id(), forwarding);
    }

    /**
     * Removes packet intercept flow rules from the device.
     *
     * @param device  the device to remove the rules deom
     * @param request the packet request
     */
    private void removeRule(Device device, PacketRequest request) {
        if (!device.type().equals(Device.Type.VIRTUAL)) {
            return;
        }
        ForwardingObjective forwarding = createBuilder(request)
                .remove(new ObjectiveContext() {
                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        log.warn("Failed to withdraw packet request {} from {}: {}",
                                 request, device.id(), error);
                    }
                });
        objectiveService.forward(device.id(), forwarding);
    }

    /**
     * Pushes a packet request flow rule to all devices.
     *
     * @param request the packet request
     */
    private void pushToAllDevices(PacketRequest request) {
        log.debug("Pushing packet request {} to all devices", request);
        for (Device device : deviceService.getDevices()) {
            pushRule(device, request);
        }
    }

    /**
     * Removes packet request flow rule from all devices.
     *
     * @param request the packet request
     */
    private void removeFromAllDevices(PacketRequest request) {
        deviceService.getAvailableDevices().forEach(d -> removeRule(d, request));
    }

    private DefaultForwardingObjective.Builder createBuilder(PacketRequest request) {
        return DefaultForwardingObjective.builder()
                .withPriority(request.priority().priorityValue())
                .withSelector(request.selector())
                .fromApp(request.appId())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withTreatment(DefaultTrafficTreatment.builder().punt().build())
                .makePermanent();
    }
}
