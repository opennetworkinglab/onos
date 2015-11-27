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
package org.onosproject.vtn.manager.impl;

import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment.Builder;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.onosproject.vtn.manager.VTNService;
import org.onosproject.vtn.table.ClassifierService;
import org.onosproject.vtn.table.L2ForwardService;
import org.onosproject.vtn.table.impl.ClassifierServiceImpl;
import org.onosproject.vtn.table.impl.L2ForwardServiceImpl;
import org.onosproject.vtn.util.DataPathIdGenerator;
import org.onosproject.vtn.util.VtnConfig;
import org.onosproject.vtn.util.VtnData;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Provides implementation of VTNService.
 */
@Component(immediate = true)
@Service
public class VTNManager implements VTNService {
    private final Logger log = getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.vtn";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

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
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    private ApplicationId appId;
    private ClassifierService classifierService;
    private L2ForwardService l2ForwardService;

    private final HostListener hostListener = new InnerHostListener();
    private final DeviceListener deviceListener = new InnerDeviceListener();

    private static final String IFACEID = "ifaceid";
    private static final String CONTROLLER_IP_KEY = "ipaddress";
    public static final String DRIVER_NAME = "onosfw";
    private static final String EX_PORT_NAME = "eth0";
    private static final String VIRTUALPORT = "vtn-virtual-port";
    private static final String SWITCHES_OF_CONTROLLER = "switchesOfController";
    private static final String SWITCH_OF_LOCAL_HOST_PORTS = "switchOfLocalHostPorts";
    private static final String DEFAULT_IP = "0.0.0.0";

    private EventuallyConsistentMap<VirtualPortId, VirtualPort> vPortStore;
    private EventuallyConsistentMap<IpAddress, Boolean> switchesOfController;
    private EventuallyConsistentMap<DeviceId, NetworkOfLocalHostPorts> switchOfLocalHostPorts;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        classifierService = new ClassifierServiceImpl(appId);
        l2ForwardService = new L2ForwardServiceImpl(appId);

        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                                .register(KryoNamespaces.API)
                                .register(NetworkOfLocalHostPorts.class)
                                .register(TenantNetworkId.class)
                                .register(Host.class)
                                .register(TenantNetwork.class)
                                .register(TenantId.class)
                                .register(SubnetId.class)
                                .register(VirtualPortId.class)
                                .register(VirtualPort.State.class)
                                .register(AllowedAddressPair.class)
                                .register(FixedIp.class)
                                .register(BindingHostId.class)
                                .register(SecurityGroup.class)
                                .register(IpAddress.class)
                                .register(DefaultVirtualPort.class);

        vPortStore = storageService
                .<VirtualPortId, VirtualPort>eventuallyConsistentMapBuilder()
                .withName(VIRTUALPORT).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        switchesOfController = storageService
                .<IpAddress, Boolean>eventuallyConsistentMapBuilder()
                .withName(SWITCHES_OF_CONTROLLER).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        switchOfLocalHostPorts = storageService
                .<DeviceId, NetworkOfLocalHostPorts>eventuallyConsistentMapBuilder()
                .withName(SWITCH_OF_LOCAL_HOST_PORTS).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        log.info("Stopped");
    }

    @Override
    public void onControllerDetected(Device controllerDevice) {
        if (controllerDevice == null) {
            log.error("The controller device is null");
            return;
        }
        String localIpAddress = controllerDevice.annotations()
                .value(CONTROLLER_IP_KEY);
        IpAddress localIp = IpAddress.valueOf(localIpAddress);
        DeviceId controllerDeviceId = controllerDevice.id();
        DriverHandler handler = driverService.createHandler(controllerDeviceId);
        if (mastershipService.isLocalMaster(controllerDeviceId)) {
            // Get DataPathIdGenerator
            String ipaddress = controllerDevice.annotations().value("ipaddress");
            DataPathIdGenerator dpidGenerator = DataPathIdGenerator.builder()
                                            .addIpAddress(ipaddress).build();
            DeviceId deviceId = dpidGenerator.getDeviceId();
            String dpid = dpidGenerator.getDpId();
            // Inject pipeline driver name
            BasicDeviceConfig config = configService.addConfig(deviceId,
                                                               BasicDeviceConfig.class);
            config.driver(DRIVER_NAME);
            configService.applyConfig(deviceId, BasicDeviceConfig.class, config.node());
            // Add Bridge
            VtnConfig.applyBridgeConfig(handler, dpid, EX_PORT_NAME);
            log.info("A new ovs is created in node {}", localIp.toString());
            switchesOfController.put(localIp, true);
        }
        // Create tunnel in br-int on all controllers
        programTunnelConfig(controllerDeviceId, localIp, handler);
    }

    @Override
    public void onControllerVanished(Device controllerDevice) {
        if (controllerDevice == null) {
            log.error("The device is null");
            return;
        }
        String dstIp = controllerDevice.annotations().value(CONTROLLER_IP_KEY);
        IpAddress dstIpAddress = IpAddress.valueOf(dstIp);
        DeviceId controllerDeviceId = controllerDevice.id();
        if (mastershipService.isLocalMaster(controllerDeviceId)) {
            switchesOfController.remove(dstIpAddress);
        }
        // remove tunnel in br-int on other controllers
        programTunnelConfig(controllerDeviceId, dstIpAddress, null);
    }

    @Override
    public void onOvsDetected(Device device) {
        // Create tunnel out flow rules
        applyTunnelOut(device, Objective.Operation.ADD);
    }

    @Override
    public void onOvsVanished(Device device) {
        // Remove Tunnel out flow rules
        applyTunnelOut(device, Objective.Operation.REMOVE);
    }

    @Override
    public void onHostDetected(Host host) {
        // apply L2 openflow rules
        applyHostMonitoredL2Rules(host, Objective.Operation.ADD);
    }

    @Override
    public void onHostVanished(Host host) {
        // apply L2 openflow rules
        applyHostMonitoredL2Rules(host, Objective.Operation.REMOVE);
    }

    private void programTunnelConfig(DeviceId localDeviceId, IpAddress localIp,
                                     DriverHandler localHandler) {
        if (mastershipService.isLocalMaster(localDeviceId)) {
            VtnConfig.applyTunnelConfig(localHandler, localIp, IpAddress.valueOf(DEFAULT_IP));
            log.info("Add tunnel on {}", localIp);
        }
    }

    private void applyTunnelOut(Device device, Objective.Operation type) {
        if (device == null) {
            log.error("The device is null");
            return;
        }
        if (!mastershipService.isLocalMaster(device.id())) {
            return;
        }
        String controllerIp = VtnData.getControllerIpOfSwitch(device);
        if (controllerIp == null) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);
        if (!switchesOfController.containsKey(ipAddress)) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }
        if (type == Objective.Operation.ADD) {
            switchOfLocalHostPorts.put(device.id(), new NetworkOfLocalHostPorts());
        } else if (type == Objective.Operation.REMOVE) {
            switchOfLocalHostPorts.remove(device.id());
        }
        Iterable<Device> devices = deviceService.getAvailableDevices();
        DeviceId localControllerId = VtnData.getControllerId(device, devices);
        DriverHandler handler = driverService.createHandler(localControllerId);
        Set<PortNumber> ports = VtnConfig.getPortNumbers(handler);
        Iterable<Host> allHosts = hostService.getHosts();
        String tunnelName = "vxlan-" + DEFAULT_IP;
        if (allHosts != null) {
            Sets.newHashSet(allHosts).stream().forEach(host -> {
                MacAddress hostMac = host.mac();
                String ifaceId = host.annotations().value(IFACEID);
                if (ifaceId == null) {
                    log.error("The ifaceId of Host is null");
                    return;
                }
                VirtualPortId virtualPortId = VirtualPortId.portId(ifaceId);
                VirtualPort virtualPort = virtualPortService
                        .getPort(virtualPortId);
                TenantNetwork network = tenantNetworkService
                        .getNetwork(virtualPort.networkId());
                SegmentationId segmentationId = network.segmentationId();
                DeviceId remoteDeviceId = host.location().deviceId();
                Device remoteDevice = deviceService.getDevice(remoteDeviceId);
                String remoteControllerIp = VtnData
                        .getControllerIpOfSwitch(remoteDevice);
                if (remoteControllerIp == null) {
                    log.error("Can't find remote controller of device: {}",
                              remoteDeviceId.toString());
                    return;
                }
                IpAddress remoteIpAddress = IpAddress
                        .valueOf(remoteControllerIp);
                ports.stream()
                        .filter(p -> p.name().equalsIgnoreCase(tunnelName))
                        .forEach(p -> {
                    l2ForwardService
                            .programTunnelOut(device.id(), segmentationId, p,
                                              hostMac, type, remoteIpAddress);
                });
            });
        }
    }

    private void applyHostMonitoredL2Rules(Host host, Objective.Operation type) {
        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }
        String ifaceId = host.annotations().value(IFACEID);
        if (ifaceId == null) {
            log.error("The ifaceId of Host is null");
            return;
        }
        VirtualPortId virtualPortId = VirtualPortId.portId(ifaceId);
        VirtualPort virtualPort = virtualPortService.getPort(virtualPortId);
        if (virtualPort == null) {
            virtualPort = vPortStore.get(virtualPortId);
        }

        Iterable<Device> devices = deviceService.getAvailableDevices();
        PortNumber inPort = host.location().port();
        MacAddress mac = host.mac();
        Device device = deviceService.getDevice(deviceId);
        String controllerIp = VtnData.getControllerIpOfSwitch(device);
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);
        TenantNetwork network = tenantNetworkService.getNetwork(virtualPort.networkId());
        if (network == null) {
            log.error("Can't find network of the host");
            return;
        }
        SegmentationId segmentationId = network.segmentationId();
        // Get all the tunnel PortNumber in the current node
        Iterable<Port> ports = deviceService.getPorts(deviceId);
        Collection<PortNumber> localTunnelPorts = VtnData.getLocalTunnelPorts(ports);
        // Get all the local vm's PortNumber in the current node
        Map<TenantNetworkId, Set<PortNumber>> localHostPorts = switchOfLocalHostPorts
                .get(deviceId).getNetworkOfLocalHostPorts();
        Set<PortNumber> networkOflocalHostPorts = localHostPorts.get(network.id());
        for (PortNumber p : localTunnelPorts) {
            programGroupTable(deviceId, appId, p, devices, type);
        }

        if (type == Objective.Operation.ADD) {
            vPortStore.put(virtualPortId, virtualPort);
            if (networkOflocalHostPorts == null) {
                networkOflocalHostPorts = new HashSet<PortNumber>();
                localHostPorts.putIfAbsent(network.id(), networkOflocalHostPorts);
            }
            networkOflocalHostPorts.add(inPort);
            l2ForwardService.programLocalBcastRules(deviceId, segmentationId,
                                                    inPort, networkOflocalHostPorts,
                                                    localTunnelPorts,
                                                    type);
            classifierService.programTunnelIn(deviceId, segmentationId,
                                              localTunnelPorts,
                                              type);
        } else if (type == Objective.Operation.REMOVE) {
            vPortStore.remove(virtualPortId);
            if (networkOflocalHostPorts != null) {
                l2ForwardService.programLocalBcastRules(deviceId, segmentationId,
                                                        inPort, networkOflocalHostPorts,
                                                        localTunnelPorts,
                                                        type);
                networkOflocalHostPorts.remove(inPort);
                if (networkOflocalHostPorts.isEmpty()) {
                    classifierService.programTunnelIn(deviceId, segmentationId,
                                                      localTunnelPorts,
                                                      type);
                    switchOfLocalHostPorts.get(deviceId).getNetworkOfLocalHostPorts()
                                                .remove(virtualPort.networkId());
                }
            }
        }

        l2ForwardService.programLocalOut(deviceId, segmentationId, inPort, mac,
                                         type);

        l2ForwardService.programTunnelBcastRules(deviceId, segmentationId,
                                                 networkOflocalHostPorts,
                                                 localTunnelPorts,
                                                 type);

        programTunnelOuts(devices, ipAddress, segmentationId, mac,
                          type);

        classifierService.programLocalIn(deviceId, segmentationId, inPort, mac,
                                         appId, type);
    }

    private void programTunnelOuts(Iterable<Device> devices,
                                   IpAddress ipAddress,
                                   SegmentationId segmentationId,
                                   MacAddress dstMac,
                                   Objective.Operation type) {
        String tunnelName = "vxlan-" + DEFAULT_IP;
        Sets.newHashSet(devices).stream()
                .filter(d -> d.type() == Device.Type.CONTROLLER)
                .filter(d -> !("ovsdb:" + ipAddress).equals(d.id().toString()))
                .forEach(d -> {
                    DriverHandler handler = driverService.createHandler(d.id());
                    BridgeConfig bridgeConfig = handler
                            .behaviour(BridgeConfig.class);
                    Collection<BridgeDescription> bridgeDescriptions = bridgeConfig
                            .getBridges();
                    Set<PortNumber> ports = bridgeConfig.getPortNumbers();
                    Iterator<BridgeDescription> it = bridgeDescriptions
                            .iterator();
                    if (it.hasNext()) {
                        BridgeDescription sw = it.next();
                        ports.stream()
                                .filter(p -> p.name()
                                        .equalsIgnoreCase(tunnelName))
                                .forEach(p -> {
                            l2ForwardService.programTunnelOut(sw.deviceId(),
                                                              segmentationId, p,
                                                              dstMac, type, ipAddress);
                        });
                    }
                });
    }

    private class InnerDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (Device.Type.CONTROLLER == device.type()) {
                if (DeviceEvent.Type.DEVICE_ADDED == event.type()) {
                    onControllerDetected(device);
                }
                if (DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED == event.type()) {
                    if (deviceService.isAvailable(device.id())) {
                        onControllerDetected(device);
                    } else {
                        onControllerVanished(device);
                    }
                }
            } else if (Device.Type.SWITCH == device.type()) {
                if (DeviceEvent.Type.DEVICE_ADDED == event.type()) {
                    onOvsDetected(device);
                }
                if (DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED == event.type()) {
                    if (deviceService.isAvailable(device.id())) {
                        onOvsDetected(device);
                    } else {
                        onOvsVanished(device);
                    }
                }
            } else {
                log.info("Do nothing for this device type");
            }
        }
    }

    private class InnerHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (HostEvent.Type.HOST_ADDED == event.type()) {
                onHostDetected(host);
            } else if (HostEvent.Type.HOST_REMOVED == event.type()) {
                onHostVanished(host);
            } else if (HostEvent.Type.HOST_UPDATED == event.type()) {
                onHostVanished(host);
                onHostDetected(host);
            }
        }

    }

    // Local Host Ports of Network.
    private class NetworkOfLocalHostPorts {
        private final Map<TenantNetworkId, Set<PortNumber>> networkOfLocalHostPorts =
                                      new HashMap<TenantNetworkId, Set<PortNumber>>();

        public Map<TenantNetworkId, Set<PortNumber>> getNetworkOfLocalHostPorts() {
            return networkOfLocalHostPorts;
        }
    }

    private void programGroupTable(DeviceId deviceId, ApplicationId appid,
                                   PortNumber portNumber, Iterable<Device> devices, Objective.Operation type) {
        if (type.equals(Objective.Operation.REMOVE)) {
            return;
        }

        List<GroupBucket> buckets = Lists.newArrayList();
        Sets.newHashSet(devices)
        .stream()
        .filter(d -> d.type() == Device.Type.CONTROLLER)
        .filter(d -> !deviceId.equals(d.id()))
        .forEach(d -> {
                    String ipAddress = d.annotations()
                             .value(CONTROLLER_IP_KEY);
                    Ip4Address dst = Ip4Address.valueOf(ipAddress);
                    Builder builder = DefaultTrafficTreatment.builder();

                    DriverHandler handler = driverService.createHandler(deviceId);
                    ExtensionTreatmentResolver resolver =  handler.behaviour(ExtensionTreatmentResolver.class);
                    ExtensionTreatment treatment = resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
                    try {
                        treatment.setPropertyValue("tunnelDst", dst);
                    } catch (Exception e) {
                       log.error("Failed to get extension instruction to set tunnel dst {}", deviceId);
                    }

                    builder.extension(treatment, deviceId);
                    builder.setOutput(portNumber);
                    GroupBucket bucket = DefaultGroupBucket
                            .createAllGroupBucket(builder.build());
                    buckets.add(bucket);
                 });
        final GroupKey key = new DefaultGroupKey(APP_ID.getBytes());
        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                                                                        GroupDescription.Type.ALL,
                                                                        new GroupBuckets(buckets),
                                                                        key,
                                                                        L2ForwardServiceImpl.GROUP_ID,
                                                                        appid);
        groupService.addGroup(groupDescription);
    }
}
