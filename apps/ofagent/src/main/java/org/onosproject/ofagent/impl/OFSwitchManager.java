/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.ofagent.impl;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentEvent;
import org.onosproject.ofagent.api.OFAgentListener;
import org.onosproject.ofagent.api.OFAgentService;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.ofagent.api.OFSwitch;
import org.onosproject.ofagent.api.OFSwitchCapabilities;
import org.onosproject.ofagent.api.OFSwitchService;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.BoundedThreadPool.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.ofagent.api.OFAgent.State.STARTED;
import static org.onosproject.ofagent.api.OFAgentService.APPLICATION_NAME;

/**
 * Manages OF switches.
 */
@Component(immediate = true)
@Service
public class OFSwitchManager implements OFSwitchService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final OFSwitchCapabilities DEFAULT_CAPABILITIES =
            DefaultOFSwitchCapabilities.builder()
            .flowStats()
            .tableStats()
            .portStats()
            .groupStats()
            .queueStats()
            .ipReasm()
            .portBlocked()
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualNetworkService virtualNetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OFAgentService ofAgentService;

    private final ConcurrentHashMap<DeviceId, OFSwitch> ofSwitchMap = new ConcurrentHashMap<>();
    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final OFAgentListener ofAgentListener = new InternalOFAgentListener();
    private final VirtualNetworkListener vNetworkListener = new InternalVirtualNetworkListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final FlowRuleListener flowRuleListener = new InternalFlowRuleListener();
    private final PacketProcessor packetProcessor = new InternalPacketProcessor();

    private NioEventLoopGroup ioWorker;
    private ApplicationId appId;
    private NodeId localId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APPLICATION_NAME);
        localId = clusterService.getLocalNode().id();
        ioWorker = new NioEventLoopGroup();

        ofAgentService.agents().forEach(this::processOFAgentCreated);
        ofAgentService.addListener(ofAgentListener);
        virtualNetService.addListener(vNetworkListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        virtualNetService.removeListener(vNetworkListener);
        ofAgentService.removeListener(ofAgentListener);
        ofAgentService.agents().forEach(this::processOFAgentStopped);

        ioWorker.shutdownGracefully();
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public Set<OFSwitch> ofSwitches() {
        return ImmutableSet.copyOf(ofSwitchMap.values());
    }

    @Override
    public Set<OFSwitch> ofSwitches(NetworkId networkId) {
        Set<OFSwitch> ofSwitches = devices(networkId).stream()
                .map(ofSwitchMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(ofSwitches);
    }

    private void addOFSwitch(NetworkId networkId, DeviceId deviceId) {
        OFSwitch ofSwitch = DefaultOFSwitch.of(
                dpidWithDeviceId(deviceId),
                DEFAULT_CAPABILITIES);
        ofSwitchMap.put(deviceId, ofSwitch);
        log.info("Added virtual OF switch for {}", deviceId);

        OFAgent ofAgent = ofAgentService.agent(networkId);
        if (ofAgent.state() == STARTED) {
            connectController(ofSwitch, ofAgent.controllers());
        }
    }

    private void deleteOFSwitch(DeviceId deviceId) {
        OFSwitch ofSwitch = ofSwitchMap.get(deviceId);
        ofSwitch.controllerChannels().forEach(ChannelOutboundInvoker::disconnect);

        ofSwitchMap.remove(deviceId);
        log.info("Removed virtual OFSwitch for {}", deviceId);
    }

    private void connectController(OFSwitch ofSwitch, Set<OFController> controllers) {
        controllers.forEach(controller -> {
            OFConnectionHandler connectionHandler = new OFConnectionHandler(
                    ofSwitch,
                    controller,
                    ioWorker);
            connectionHandler.connect();
        });
    }

    private void disconnectController(OFSwitch ofSwitch, Set<OFController> controllers) {
        Set<SocketAddress> controllerAddrs = controllers.stream()
                .map(ctrl -> new InetSocketAddress(
                        ctrl.ip().toInetAddress(), ctrl.port().toInt()))
                .collect(Collectors.toSet());

        ofSwitch.controllerChannels().stream()
                .filter(channel -> controllerAddrs.contains(channel.remoteAddress()))
                .forEach(ChannelOutboundInvoker::disconnect);
    }

    private Set<DeviceId> devices(NetworkId networkId) {
        Set<DeviceId> deviceIds = virtualNetService.getVirtualDevices(networkId)
                .stream()
                .map(Device::id)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(deviceIds);
    }

    private DatapathId dpidWithDeviceId(DeviceId deviceId) {
        String strDeviceId = deviceId.toString().split(":")[1];
        checkArgument(strDeviceId.length() == 16, "Invalid device ID " + strDeviceId);

        String resultedHexString = "";
        for (int i = 0; i < 8; i++) {
            resultedHexString = resultedHexString + strDeviceId.charAt(2 * i)
                    + strDeviceId.charAt(2 * i + 1);
            if (i != 7) {
                resultedHexString += ":";
            }
        }
        return DatapathId.of(resultedHexString);
    }

    private void processOFAgentCreated(OFAgent ofAgent) {
        devices(ofAgent.networkId()).forEach(deviceId -> {
            addOFSwitch(ofAgent.networkId(), deviceId);
        });
    }

    private void processOFAgentRemoved(OFAgent ofAgent) {
        devices(ofAgent.networkId()).forEach(this::deleteOFSwitch);
    }

    private void processOFAgentStarted(OFAgent ofAgent) {
        devices(ofAgent.networkId()).forEach(deviceId -> {
            OFSwitch ofSwitch = ofSwitchMap.get(deviceId);
            if (ofSwitch != null) {
                connectController(ofSwitch, ofAgent.controllers());
            }
        });

        DeviceService deviceService = virtualNetService.get(
                ofAgent.networkId(),
                DeviceService.class);
        deviceService.addListener(deviceListener);

        PacketService packetService = virtualNetService.get(
                ofAgent.networkId(),
                PacketService.class);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));

        FlowRuleService flowRuleService = virtualNetService.get(
                ofAgent.networkId(),
                FlowRuleService.class);
        flowRuleService.addListener(flowRuleListener);
    }

    private void processOFAgentStopped(OFAgent ofAgent) {
        devices(ofAgent.networkId()).forEach(deviceId -> {
            OFSwitch ofSwitch = ofSwitchMap.get(deviceId);
            if (ofSwitch != null) {
                disconnectController(ofSwitch, ofAgent.controllers());
            }
        });

        DeviceService deviceService = virtualNetService.get(
                ofAgent.networkId(),
                DeviceService.class);
        deviceService.removeListener(deviceListener);

        PacketService packetService = virtualNetService.get(
                ofAgent.networkId(),
                PacketService.class);
        packetService.removeProcessor(packetProcessor);

        FlowRuleService flowRuleService = virtualNetService.get(
                ofAgent.networkId(),
                FlowRuleService.class);
        flowRuleService.removeListener(flowRuleListener);
    }

    private class InternalVirtualNetworkListener implements VirtualNetworkListener {

        @Override
        public void event(VirtualNetworkEvent event) {
            switch (event.type()) {
                case VIRTUAL_DEVICE_ADDED:
                    eventExecutor.execute(() -> {
                        log.debug("Virtual device {} added to network {}",
                                event.virtualDevice().id(),
                                event.subject());
                        addOFSwitch(event.subject(), event.virtualDevice().id());
                    });
                    break;
                case VIRTUAL_DEVICE_UPDATED:
                    // TODO handle device availability updates
                    break;
                case VIRTUAL_DEVICE_REMOVED:
                    eventExecutor.execute(() -> {
                        log.debug("Virtual device {} removed from network {}",
                                event.virtualDevice().id(),
                                event.subject());
                        deleteOFSwitch(event.virtualDevice().id());
                    });
                    break;
                case NETWORK_UPDATED:
                case NETWORK_REMOVED:
                case NETWORK_ADDED:
                case VIRTUAL_PORT_ADDED:
                case VIRTUAL_PORT_UPDATED:
                case VIRTUAL_PORT_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class InternalOFAgentListener implements OFAgentListener {

        @Override
        public boolean isRelevant(OFAgentEvent event) {
            return Objects.equals(localId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OFAgentEvent event) {
            switch (event.type()) {
                case OFAGENT_CREATED:
                    eventExecutor.execute(() -> {
                        OFAgent ofAgent = event.subject();
                        log.debug("Processing OFAgent created: {}", ofAgent);
                        processOFAgentCreated(ofAgent);
                    });
                    break;
                case OFAGENT_REMOVED:
                    eventExecutor.execute(() -> {
                        OFAgent ofAgent = event.subject();
                        log.debug("Processing OFAgent removed: {}", ofAgent);
                        processOFAgentRemoved(ofAgent);
                    });
                    break;
                case OFAGENT_CONTROLLER_ADDED:
                    // TODO handle additional controller
                    break;
                case OFAGENT_CONTROLLER_REMOVED:
                    // TODO handle removed controller
                    break;
                case OFAGENT_STARTED:
                    eventExecutor.execute(() -> {
                        OFAgent ofAgent = event.subject();
                        log.debug("Processing OFAgent started: {}", ofAgent);
                        processOFAgentStarted(ofAgent);
                    });
                    break;
                case OFAGENT_STOPPED:
                    eventExecutor.execute(() -> {
                        OFAgent ofAgent = event.subject();
                        log.debug("Processing OFAgent stopped: {}", ofAgent);
                        processOFAgentStopped(ofAgent);
                    });
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                case PORT_ADDED:
                    // TODO handle event
                case PORT_REMOVED:
                    // TODO handle event
                case PORT_STATS_UPDATED:
                case PORT_UPDATED:
                default:
                    break;
            }
        }
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // TODO handle packet-in
        }
    }

    private class InternalFlowRuleListener implements FlowRuleListener {

        @Override
        public void event(FlowRuleEvent event) {
            // TODO handle flow rule event
        }
    }
}
