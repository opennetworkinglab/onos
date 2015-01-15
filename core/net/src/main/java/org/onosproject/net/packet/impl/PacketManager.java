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
package org.onosproject.net.packet.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketEvent;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketStore;
import org.onosproject.net.packet.PacketStoreDelegate;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;

/**
 * Provides a basic implementation of the packet SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class PacketManager
extends AbstractProviderRegistry<PacketProvider, PacketProviderService>
implements PacketService, PacketProviderRegistry {

    private final Logger log = getLogger(getClass());

    private final PacketStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PacketStore store;

    private final DeviceListener deviceListener = new InternalDeviceListener();

    private final Map<Integer, PacketProcessor> processors = new ConcurrentHashMap<>();

    private Set<PacketRequest> packetRequests =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final class PacketRequest {
        private final TrafficSelector selector;
        private final PacketPriority priority;
        private final ApplicationId appId;

        public PacketRequest(TrafficSelector selector, PacketPriority priority,
                             ApplicationId appId) {
            this.selector = selector;
            this.priority = priority;
            this.appId = appId;
        }

        public TrafficSelector selector() {
            return selector;
        }

        public PacketPriority priority() {
            return priority;
        }

        public ApplicationId appId() {
            return appId;
        }

    }

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }

    @Override
    public void addProcessor(PacketProcessor processor, int priority) {
        checkNotNull(processor, "Processor cannot be null");
        processors.put(priority, processor);
    }

    @Override
    public void removeProcessor(PacketProcessor processor) {
        checkNotNull(processor, "Processor cannot be null");
        processors.values().remove(processor);
    }

    @Override
    public void requestPackets(TrafficSelector selector, PacketPriority priority,
                               ApplicationId appId) {
        checkNotNull(selector, "Selector cannot be null");
        checkNotNull(appId, "Application ID cannot be null");

        PacketRequest request =
                new PacketRequest(selector, priority, appId);

        packetRequests.add(request);
        pushToAllDevices(request);
    }

    /**
     * Pushes a packet request flow rule to all devices.
     *
     * @param request the packet request
     */
    private void pushToAllDevices(PacketRequest request) {
        for (Device device : deviceService.getDevices()) {
            pushRule(device, request);
        }
    }

    /**
     * Pushes flow rules to the device to request packets be sent to the
     * controller.
     *
     * @param device the device to push the rules to
     * @param request the packet request
     */
    private void pushRule(Device device, PacketRequest request) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                                            .punt()
                                                            .build();

        FlowRule flow = new DefaultFlowRule(device.id(),
                                request.selector(),
                                treatment,
                                request.priority().priorityValue(),
                                request.appId(),
                                0, true);

        flowService.applyFlowRules(flow);
    }

    @Override
    public void emit(OutboundPacket packet) {
        checkNotNull(packet, "Packet cannot be null");

        store.emit(packet);
    }

    private void localEmit(OutboundPacket packet) {
        final Device device = deviceService.getDevice(packet.sendThrough());

        if (device == null) {
            return;
        }

        final PacketProvider packetProvider = getProvider(device.providerId());

        if (packetProvider != null) {
            packetProvider.emit(packet);
        }
    }

    @Override
    protected PacketProviderService createProviderService(PacketProvider provider) {
        return new InternalPacketProviderService(provider);
    }

    // Personalized link provider service issued to the supplied provider.
    private class InternalPacketProviderService
    extends AbstractProviderService<PacketProvider>
    implements PacketProviderService {

        protected InternalPacketProviderService(PacketProvider provider) {
            super(provider);
        }

        @Override
        public void processPacket(PacketContext context) {
            // TODO filter packets sent to processors based on registrations
            for (PacketProcessor processor : processors.values()) {
                processor.process(context);
            }
        }

    }

    /**
     * Internal callback from the packet store.
     */
    private class InternalStoreDelegate
    implements PacketStoreDelegate {
        @Override
        public void notify(PacketEvent event) {
            localEmit(event.subject());
        }
    }

    /**
     * Internal listener for device service events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (event.type() == DeviceEvent.Type.DEVICE_ADDED) {
                for (PacketRequest request : packetRequests) {
                    pushRule(device, request);
                }
            }
        }
    }

}
