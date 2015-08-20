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
package org.onosproject.vtn.impl;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.IpTunnelEndPoint;
import org.onosproject.net.behaviour.TunnelConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoint;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtn.VTNService;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of VTNService.
 */
@Component(immediate = true)
@Service
public class VTNManager implements VTNService {
    private final Logger log = getLogger(getClass());

    private static final String APP_ID = "org.onosproject.app.vtn";
    private ScheduledExecutorService backgroundService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService tenantNetworkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;
    private EventuallyConsistentMap<HostId, SegmentationId> binding;
    private ApplicationId appId;
    private HostListener hostListener = new InnerHostListener();
    private DeviceListener deviceListener = new InnerDeviceListener();
    private static final String IFACEID = "ifaceid";
    private static final String PORT_HEAD = "vxlan";
    private static final String DEFAULT_BRIDGE_NAME = "br-int";
    private static final String CONTROLLER_IP_KEY = "ipaddress";

    @Activate
    public void activate() {
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API);
        appId = coreService.registerApplication(APP_ID);
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        backgroundService = newSingleThreadScheduledExecutor(groupedThreads("onos-apps/vtn",
                                                                            "manager-background"));
        binding = storageService
                .<HostId, SegmentationId>eventuallyConsistentMapBuilder()
                .withName("all_tunnel").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        backgroundService.shutdown();
        binding.destroy();
        log.info("Stopped");
    }

    @Override
    public void onServerDetected(Device device) {
        Iterable<Device> devices = deviceService.getAvailableDevices();
        DriverHandler handler = driverService.createHandler(device.id());
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        bridgeConfig.addBridge(BridgeName.bridgeName(DEFAULT_BRIDGE_NAME));
        String ipAddress = device.annotations().value(CONTROLLER_IP_KEY);
        IpAddress ip = IpAddress.valueOf(ipAddress);
        Sets.newHashSet(devices)
                .stream()
                .filter(d -> d.type() == Device.Type.CONTROLLER)
                .filter(d -> !device.id().equals(d.id()))
                .forEach(d -> {
                             String ipAddress1 = d.annotations()
                                     .value(CONTROLLER_IP_KEY);
                             IpAddress ip1 = IpAddress.valueOf(ipAddress1);
                             applyTunnelConfig(ip, ip1, handler);
                             DriverHandler handler1 = driverService
                                     .createHandler(d.id());
                             applyTunnelConfig(ip1, ip, handler1);
                         });
    }

    @Override
    public void onServerVanished(Device device) {
        Iterable<Device> devices = deviceService.getAvailableDevices();
        String ipAddress = device.annotations().value(CONTROLLER_IP_KEY);
        IpAddress dst = IpAddress.valueOf(ipAddress);
        Sets.newHashSet(devices)
                .stream()
                .filter(d -> d.type() == Device.Type.CONTROLLER)
                .filter(d -> !device.id().equals(d.id()))
                .forEach(d -> {
                             String ipAddress1 = d.annotations()
                                     .value(CONTROLLER_IP_KEY);
                             DriverHandler handler = driverService
                                     .createHandler(d.id());
                             IpAddress src = IpAddress.valueOf(ipAddress1);
                             removeTunnelConfig(src, dst, handler);
                         });
    }

    private void applyTunnelConfig(IpAddress src, IpAddress dst,
                                   DriverHandler handler) {
        TunnelEndPoint tunnelAsSrc = IpTunnelEndPoint.ipTunnelPoint(src);
        TunnelEndPoint tunnelAsDst = IpTunnelEndPoint.ipTunnelPoint(dst);
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                tunnelAsSrc,
                                                                tunnelAsDst,
                                                                TunnelDescription.Type.VXLAN,
                                                                null);
        TunnelConfig config = handler.behaviour(TunnelConfig.class);
        config.createTunnel(tunnel);
    }

    private void removeTunnelConfig(IpAddress src, IpAddress dst,
                                    DriverHandler handler) {
        TunnelEndPoint tunnelAsSrc = IpTunnelEndPoint.ipTunnelPoint(src);
        TunnelEndPoint tunnelAsDst = IpTunnelEndPoint.ipTunnelPoint(dst);
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                tunnelAsSrc,
                                                                tunnelAsDst,
                                                                TunnelDescription.Type.VXLAN,
                                                                null);
        TunnelConfig config = handler.behaviour(TunnelConfig.class);
        config.removeTunnel(tunnel);
    }

    @Override
    public void onOvsDetected(Device device) {
        programMacDefaultRules(device.id(), appId, Objective.Operation.ADD);
        programPortDefaultRules(device.id(), appId, Objective.Operation.ADD);
    }

    @Override
    public void onOvsVanished(Device device) {
        programMacDefaultRules(device.id(), appId, Objective.Operation.REMOVE);
        programPortDefaultRules(device.id(), appId, Objective.Operation.REMOVE);
    }

    @Override
    public void onHostDetected(Host host) {
        String ifaceId = host.annotations().value(IFACEID);
        VirtualPortId portId = VirtualPortId.portId(ifaceId);
        VirtualPort port = virtualPortService.getPort(portId);
        TenantNetwork network = tenantNetworkService.getNetwork(port
                .networkId());
        binding.put(host.id(), network.segmentationId());
        DeviceId deviceId = host.location().deviceId();
        List<Port> allPorts = deviceService.getPorts(deviceId);
        PortNumber inPort = host.location().port();
        Set<Port> localPorts = new HashSet<>();
        allPorts.forEach(p -> {
            if (!p.number().name().startsWith(PORT_HEAD)) {
                localPorts.add(p);
            }
        });
        programLocalBcastRules(deviceId, network.segmentationId(), inPort,
                               allPorts, appId, Objective.Operation.ADD);
        programLocalOut(deviceId, network.segmentationId(), inPort, host.mac(),
                        appId, Objective.Operation.ADD);
        programTunnelFloodOut(deviceId, network.segmentationId(), inPort,
                              localPorts, appId, Objective.Operation.ADD);
        programTunnelOut(deviceId, network.segmentationId(), inPort,
                         host.mac(), appId, Objective.Operation.ADD);
        programLocalIn(deviceId, network.segmentationId(), inPort, host.mac(),
                       appId, Objective.Operation.ADD);
        programTunnelIn(deviceId, network.segmentationId(), inPort, host.mac(),
                        appId, Objective.Operation.ADD);
    }

    @Override
    public void onHostVanished(Host host) {
        SegmentationId segId = binding.remove(host.id());
        DeviceId deviceId = host.location().deviceId();
        List<Port> allPorts = deviceService.getPorts(deviceId);
        PortNumber inPort = host.location().port();
        Set<Port> localPorts = new HashSet<>();
        allPorts.forEach(p -> {
            if (!p.number().name().startsWith(PORT_HEAD)) {
                localPorts.add(p);
            }
        });
        programLocalBcastRules(deviceId, segId, inPort, allPorts, appId,
                               Objective.Operation.REMOVE);
        programLocalOut(deviceId, segId, inPort, host.mac(), appId,
                        Objective.Operation.REMOVE);
        programTunnelFloodOut(deviceId, segId, inPort, localPorts, appId,
                              Objective.Operation.REMOVE);
        programTunnelOut(deviceId, segId, inPort, host.mac(), appId,
                         Objective.Operation.REMOVE);
        programLocalIn(deviceId, segId, inPort, host.mac(), appId,
                       Objective.Operation.REMOVE);
        programTunnelIn(deviceId, segId, inPort, host.mac(), appId,
                        Objective.Operation.REMOVE);
    }

    private class InnerDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (Device.Type.CONTROLLER == device.type()
                    && DeviceEvent.Type.DEVICE_ADDED == event.type()) {
                backgroundService.execute(() -> {
                    onServerDetected(device);
                });
            } else if (Device.Type.CONTROLLER == device.type()
                    && DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED == event
                            .type()) {
                backgroundService.execute(() -> {
                    onServerVanished(device);
                });
            } else if (Device.Type.SWITCH == device.type()
                    && DeviceEvent.Type.DEVICE_ADDED == event.type()) {
                backgroundService.execute(() -> {
                    onOvsDetected(device);
                });
            } else if (Device.Type.SWITCH == device.type()
                    && DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED == event
                            .type()) {
                backgroundService.execute(() -> {
                    onOvsVanished(device);
                });
            } else {
                log.info("do nothing for this device type");
            }
        }

    }

    private class InnerHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (HostEvent.Type.HOST_ADDED == event.type()) {
                backgroundService.execute(() -> {
                    onHostDetected(host);
                });
            } else if (HostEvent.Type.HOST_REMOVED == event.type()) {
                backgroundService.execute(() -> {
                    onHostVanished(host);
                });
            } else {
                log.info("unknow host");
            }
        }

    }

    // Used to forward the flows to the local VM.
    private void programLocalOut(DeviceId dpid, SegmentationId segmentationId,
                                 PortNumber outPort, MacAddress sourceMac,
                                 ApplicationId appid, Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(sourceMac).build();
        TrafficTreatment treatment = DefaultTrafficTreatment
                .builder()
                .add(Instructions.modTunnelId(Long.parseLong(segmentationId
                             .toString()))).setOutput(outPort).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(dpid, objective.add());
        } else {
            flowObjectiveService.forward(dpid, objective.remove());
        }

    }

    // Used to forward the flows to the remote VM via VXLAN tunnel.
    private void programTunnelOut(DeviceId dpid, SegmentationId segmentationId,
                                  PortNumber outPort, MacAddress sourceMac,
                                  ApplicationId appid, Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(sourceMac).build();
        TrafficTreatment treatment = DefaultTrafficTreatment
                .builder()
                .add(Instructions.modTunnelId(Long.parseLong(segmentationId
                             .toString()))).setOutput(outPort).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).makePermanent().withFlag(Flag.SPECIFIC);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(dpid, objective.add());
        } else {
            flowObjectiveService.forward(dpid, objective.remove());
        }
    }

    // Used to forward multicast flows to remote VMs of the same tenant via
    // VXLAN tunnel.
    private void programTunnelFloodOut(DeviceId dpid,
                                       SegmentationId segmentationId,
                                       PortNumber ofPortOut,
                                       Iterable<Port> localports,
                                       ApplicationId appid,
                                       Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector
                .builder()
                .matchInPort(ofPortOut)

                .add(Criteria.matchTunnelId(Long.parseLong(segmentationId
                             .toString()))).matchEthDst(MacAddress.BROADCAST)
                .build();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        for (Port outport : localports) {
            treatment.setOutput(outport.number());
        }

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).makePermanent()
                .withFlag(Flag.SPECIFIC);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(dpid, objective.add());
        } else {
            flowObjectiveService.forward(dpid, objective.remove());
        }
    }

    // Applies default flows to mac table.
    private void programMacDefaultRules(DeviceId dpid, ApplicationId appid,
                                        Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().drop()
                .build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).makePermanent().withFlag(Flag.SPECIFIC);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(dpid, objective.add());
        } else {
            flowObjectiveService.forward(dpid, objective.remove());
        }
    }

    // Used to forward the flows to the local VMs with the same tenant.
    private void programLocalBcastRules(DeviceId dpid,
                                        SegmentationId segmentationId,
                                        PortNumber inPort, List<Port> allports,
                                        ApplicationId appid,
                                        Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector
                .builder()
                .matchInPort(inPort)
                .matchEthDst(MacAddress.BROADCAST)
                .add(Criteria.matchTunnelId(Long.parseLong(segmentationId
                             .toString()))).build();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        for (Port outport : allports) {
            if (inPort != outport.number()) {
                treatment.setOutput(outport.number());
            }
        }
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).makePermanent()
                .withFlag(Flag.SPECIFIC);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(dpid, objective.add());
        } else {
            flowObjectiveService.forward(dpid, objective.remove());
        }
    }

    // Used to apply local entry flow.
    private void programLocalIn(DeviceId dpid, SegmentationId segmentationId,
                                PortNumber inPort, MacAddress srcMac,
                                ApplicationId appid, Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort).matchEthSrc(srcMac).build();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.add(Instructions.modTunnelId(Long.parseLong(segmentationId
                .toString())));
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).makePermanent()
                .withFlag(Flag.SPECIFIC);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(dpid, objective.add());
        } else {
            flowObjectiveService.forward(dpid, objective.remove());
        }
    }

    // Used to forward the flows from the egress tunnel to the VM.
    private void programTunnelIn(DeviceId dpid, SegmentationId segmentationId,
                                 PortNumber inPort, MacAddress sourceMac,
                                 ApplicationId appid, Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector
                .builder()
                .matchInPort(inPort)
                .add(Criteria.matchTunnelId(Long.parseLong(segmentationId
                             .toString()))).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).makePermanent().withFlag(Flag.SPECIFIC);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(dpid, objective.add());
        } else {
            flowObjectiveService.forward(dpid, objective.remove());
        }
    }

    // Applies the default flows to port table.
    private void programPortDefaultRules(DeviceId dpid, ApplicationId appid,
                                         Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).makePermanent().withFlag(Flag.SPECIFIC);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(dpid, objective.add());
        } else {
            flowObjectiveService.forward(dpid, objective.remove());
        }
    }
}
