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

package org.onosproject.simplefabric.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.app.ApplicationService;
import org.onosproject.component.ComponentService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceEvent;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.simplefabric.api.FabricNetwork;
import org.onosproject.simplefabric.api.FabricRoute;
import org.onosproject.simplefabric.api.FabricSubnet;
import org.onosproject.simplefabric.api.SimpleFabricEvent;
import org.onosproject.simplefabric.api.SimpleFabricListener;
import org.onosproject.simplefabric.api.SimpleFabricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.onosproject.routeservice.RouteTools.createBinaryString;
import static org.onosproject.simplefabric.api.Constants.ALLOW_ETH_ADDRESS_SELECTOR;
import static org.onosproject.simplefabric.api.Constants.ALLOW_IPV6;
import static org.onosproject.simplefabric.api.Constants.APP_ID;
import static org.onosproject.simplefabric.api.Constants.IDLE_INTERVAL_MSEC;
import static org.onosproject.simplefabric.api.Constants.REACTIVE_ALLOW_LINK_CP;
import static org.onosproject.simplefabric.api.Constants.REACTIVE_HASHED_PATH_SELECTION;
import static org.onosproject.simplefabric.api.Constants.REACTIVE_MATCH_IP_PROTO;
import static org.onosproject.simplefabric.api.Constants.REACTIVE_SINGLE_TO_SINGLE;


/**
 * Reactive routing configuration manager.
 */
@Component(immediate = true, service = SimpleFabricService.class)
public class SimpleFabricManager extends ListenerRegistry<SimpleFabricEvent, SimpleFabricListener>
        implements SimpleFabricService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    // compoents to be activated within SimpleFabric
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentService componentService;

    // SimpleFabric variables
    private ApplicationId appId = null;

    // fabric networks
    private Set<FabricNetwork> fabricNetworks = new HashSet<>();
    private Set<Interface> networkInterfaces = new HashSet<>();

    // Subnet table
    private Set<FabricSubnet> fabricSubnets = new HashSet<>();
    private InvertedRadixTree<FabricSubnet> ip4SubnetTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
    private InvertedRadixTree<FabricSubnet> ip6SubnetTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());

    // Fabric Route table
    private Set<FabricRoute> fabricRoutes = new HashSet<>();
    private InvertedRadixTree<FabricRoute> ip4BorderRouteTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
    private InvertedRadixTree<FabricRoute> ip6BorderRouteTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());

    // Virtual gateway
    private Map<IpAddress, MacAddress> virtualGatewayIpMacMap = Maps.newConcurrentMap();

    // Refresh monitor thread
    private Object refreshMonitor = new Object();
    private boolean doRefresh = false;
    private boolean doFlush = false;
    private InternalRefreshThread refreshThread;

    // Listener for Service Events
    private final InternalNetworkConfigListener configListener = new InternalNetworkConfigListener();
    private final InternalDeviceListener deviceListener = new InternalDeviceListener();
    private final InternalHostListener hostListener = new InternalHostListener();

    private ConfigFactory<ApplicationId, SimpleFabricConfig> simpleFabricConfigFactory =
        new ConfigFactory<ApplicationId, SimpleFabricConfig>(
                SubjectFactories.APP_SUBJECT_FACTORY,
                SimpleFabricConfig.class, SimpleFabricConfig.KEY) {
        @Override
        public SimpleFabricConfig createConfig() {
            return new SimpleFabricConfig();
       }
    };

    @Activate
    public void activate() {
        log.info("simple fabric starting");

        if (appId == null) {
            appId = coreService.registerApplication(APP_ID);
        }

        // initial refresh
        refresh();

        configService.addListener(configListener);
        registry.registerConfigFactory(simpleFabricConfigFactory);
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);

        componentService.activate(appId, SimpleFabricNeighbour.class.getName());
        componentService.activate(appId, SimpleFabricRouting.class.getName());
        if (ALLOW_ETH_ADDRESS_SELECTOR) {
            componentService.activate(appId, SimpleFabricForwarding.class.getName());
        }

        refreshThread = new InternalRefreshThread();
        refreshThread.start();

        log.info("simple fabric started");
    }

    @Deactivate
    public void deactivate() {
        log.info("simple fabric stopping");

        componentService.deactivate(appId, SimpleFabricNeighbour.class.getName());
        componentService.deactivate(appId, SimpleFabricRouting.class.getName());
        if (ALLOW_ETH_ADDRESS_SELECTOR) {
            componentService.deactivate(appId, SimpleFabricForwarding.class.getName());
        }

        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        registry.unregisterConfigFactory(simpleFabricConfigFactory);
        configService.removeListener(configListener);

        refreshThread.stop();
        refreshThread = null;

        log.info("simple fabric stopped");
    }

    // Set up from configuration
    // returns found isDirty and refresh listeners are called (true) or not (false)
    private boolean refresh() {
        log.debug("simple fabric refresh");
        boolean dirty = false;

        SimpleFabricConfig config = configService.getConfig(coreService.registerApplication(APP_ID),
                                                            SimpleFabricConfig.class);
        if (config == null) {
            log.debug("No simple fabric config available!");
            return false;
        }

        // fabricNetworks
        Set<FabricNetwork> newFabricNetworks = new HashSet<>();
        Set<Interface> newInterfaces = new HashSet<>();
        for (FabricNetwork newFabricNetworkConfig : config.fabricNetworks()) {
            FabricNetwork newFabricNetwork = DefaultFabricNetwork.of(newFabricNetworkConfig);

            // fill up interfaces and Hosts with active port only
            for (String ifaceName : newFabricNetworkConfig.interfaceNames()) {
                Interface iface = getInterfaceByName(ifaceName);
                if (iface != null && deviceService.isAvailable(iface.connectPoint().deviceId())) {
                     newFabricNetwork.addInterface(iface);
                     newInterfaces.add(iface);
                }
            }
            for (Host host : hostService.getHosts()) {
                // consider host with ip only
                if (!host.ipAddresses().isEmpty()) {
                    Interface iface = findAvailableDeviceHostInterface(host);
                    if (iface != null && newFabricNetwork.contains(iface)) {
                        newFabricNetwork.addHost(host);
                    }
                }
            }
            newFabricNetwork.setDirty(true);

            // update newFabricNetwork's isDirty flags if same entry already exists
            for (FabricNetwork prevFabricNetwork : fabricNetworks) {
                if (prevFabricNetwork.equals(newFabricNetwork)) {
                    newFabricNetwork.setDirty(prevFabricNetwork.isDirty());
                    break;
                }
            }
            newFabricNetworks.add(newFabricNetwork);
        }
        if (!fabricNetworks.equals(newFabricNetworks)) {
            fabricNetworks = newFabricNetworks;
            dirty = true;
        }
        if (!networkInterfaces.equals(newInterfaces)) {
            networkInterfaces = newInterfaces;
            dirty = true;
        }

        // default Fabric Subnets
        Set<FabricSubnet> newFabricSubnets = config.fabricSubnets();
        InvertedRadixTree<FabricSubnet> newIp4SubnetTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
        InvertedRadixTree<FabricSubnet> newIp6SubnetTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
        Map<IpAddress, MacAddress> newVirtualGatewayIpMacMap = Maps.newConcurrentMap();
        for (FabricSubnet subnet : newFabricSubnets) {
            if (subnet.prefix().isIp4()) {
                newIp4SubnetTable.put(createBinaryString(subnet.prefix()), subnet);
            } else {
                newIp6SubnetTable.put(createBinaryString(subnet.prefix()), subnet);
            }
            newVirtualGatewayIpMacMap.put(subnet.gatewayIp(), subnet.gatewayMac());
        }
        if (!fabricSubnets.equals(newFabricSubnets)) {
            fabricSubnets = newFabricSubnets;
            ip4SubnetTable = newIp4SubnetTable;
            ip6SubnetTable = newIp6SubnetTable;
            dirty = true;
        }
        if (!virtualGatewayIpMacMap.equals(newVirtualGatewayIpMacMap)) {
            virtualGatewayIpMacMap = newVirtualGatewayIpMacMap;
            dirty = true;
        }

        // fabricRoutes config handling
        Set<FabricRoute> newFabricRoutes = config.fabricRoutes();
        if (!fabricRoutes.equals(newFabricRoutes)) {
            InvertedRadixTree<FabricRoute> newIp4BorderRouteTable =
                    new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
            InvertedRadixTree<FabricRoute> newIp6BorderRouteTable =
                    new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
            for (FabricRoute route : newFabricRoutes) {
                if (route.prefix().isIp4()) {
                    newIp4BorderRouteTable.put(createBinaryString(route.prefix()), route);
                } else {
                    newIp6BorderRouteTable.put(createBinaryString(route.prefix()), route);
                }
            }
            fabricRoutes = newFabricRoutes;
            ip4BorderRouteTable = newIp4BorderRouteTable;
            ip6BorderRouteTable = newIp6BorderRouteTable;
            dirty = true;
        }

        // notify to SimpleFabric listeners
        if (dirty) {
            log.info("simple fabric refresh; notify events");
            process(new SimpleFabricEvent(SimpleFabricEvent.Type.SIMPLE_FABRIC_UPDATED, "updated"));
        }
        return dirty;
    }

    private Interface getInterfaceByName(String interfaceName) {
        Interface intf = interfaceService.getInterfaces().stream()
                          .filter(iface -> iface.name().equals(interfaceName))
                          .findFirst()
                          .orElse(null);
        if (intf == null) {
            log.warn("simple fabric unknown interface name: {}", interfaceName);
        }
        return intf;
    }

    @Override
    public ApplicationId appId() {
        if (appId == null) {
            appId = coreService.registerApplication(APP_ID);
        }
        return appId;
    }

    @Override
    public Collection<FabricNetwork> fabricNetworks() {
        return ImmutableSet.copyOf(fabricNetworks);
    }

    @Override
    public Set<FabricSubnet> defaultFabricSubnets() {
        return ImmutableSet.copyOf(fabricSubnets);
    }

    @Override
    public Set<FabricRoute> fabricRoutes() {
        return ImmutableSet.copyOf(fabricRoutes);
    }

    @Override
    public boolean isVirtualGatewayMac(MacAddress mac) {
        return virtualGatewayIpMacMap.containsValue(mac);
    }

    @Override
    public boolean isFabricNetworkInterface(Interface intf) {
        return networkInterfaces.contains(intf);
    }

    @Override
    public MacAddress vMacForIp(IpAddress ip) {
        return virtualGatewayIpMacMap.get(ip);
    }

    @Override
    public FabricNetwork fabricNetwork(ConnectPoint port, VlanId vlanId) {
        for (FabricNetwork fabricNetwork : fabricNetworks) {
            if (fabricNetwork.contains(port, vlanId)) {
                return fabricNetwork;
            }
        }
        return null;
    }

    @Override
    public FabricNetwork fabricNetwork(String name) {
        for (FabricNetwork fabricNetwork : fabricNetworks) {
            if (fabricNetwork.name().equals(name)) {
                return fabricNetwork;
            }
        }
        return null;
    }

    @Override
    public FabricSubnet fabricSubnet(IpAddress ip) {
        if (ip.isIp4()) {
            return ip4SubnetTable.getValueForLongestKeyPrefixing(
                     createBinaryString(IpPrefix.valueOf(ip, Ip4Address.BIT_LENGTH)));
        } else {
            return ip6SubnetTable.getValueForLongestKeyPrefixing(
                     createBinaryString(IpPrefix.valueOf(ip, Ip6Address.BIT_LENGTH)));
        }
    }

    @Override
    public FabricRoute fabricRoute(IpAddress ip) {
        // ASSUME: ipAddress is out of fabricSubnet
        if (ip.isIp4()) {
            return ip4BorderRouteTable.getValueForLongestKeyPrefixing(
                     createBinaryString(IpPrefix.valueOf(ip, Ip4Address.BIT_LENGTH)));
        } else {
            return ip6BorderRouteTable.getValueForLongestKeyPrefixing(
                     createBinaryString(IpPrefix.valueOf(ip, Ip6Address.BIT_LENGTH)));
        }
    }


    @Override
    public Interface hostInterface(Host host) {
        return interfaceService.getInterfaces().stream()
                .filter(iface -> iface.connectPoint().equals(host.location()) &&
                                 iface.vlan().equals(host.vlan()))
                .findFirst()
                .orElse(null);
    }

    private Interface findAvailableDeviceHostInterface(Host host) {
        return interfaceService.getInterfaces().stream()
                .filter(iface -> iface.connectPoint().equals(host.location()) &&
                                 iface.vlan().equals(host.vlan()))
                .filter(iface -> deviceService.isAvailable(iface.connectPoint().deviceId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean requestMac(IpAddress ip) {
        FabricSubnet fabricSubnet = fabricSubnet(ip);
        if (fabricSubnet == null) {
            log.warn("simple fabric request mac failed for unknown fabricSubnet: {}", ip);
            return false;
        }
        FabricNetwork fabricNetwork = fabricNetwork(fabricSubnet.networkName());
        if (fabricNetwork == null) {
            log.warn("simple fabric request mac failed for unknown fabricNetwork name {}: {}",
                     fabricSubnet.networkName(), ip);
            return false;
        }
        log.debug("simple fabric send request mac fabricNetwork {}: {}", fabricNetwork.name(), ip);
        for (Interface iface : fabricNetwork.interfaces()) {
            Ethernet neighbourReq;
            if (ip.isIp4()) {
                neighbourReq = ARP.buildArpRequest(fabricSubnet.gatewayMac().toBytes(),
                                                   fabricSubnet.gatewayIp().toOctets(),
                                                   ip.toOctets(),
                                                   iface.vlan().toShort());
            } else {
                byte[] soliciteIp = IPv6.getSolicitNodeAddress(ip.toOctets());
                neighbourReq = NeighborSolicitation.buildNdpSolicit(
                                                   ip.getIp6Address(),
                                                   fabricSubnet.gatewayIp().getIp6Address(),
                                                   Ip6Address.valueOf(soliciteIp),
                                                   MacAddress.valueOf(fabricSubnet.gatewayMac().toBytes()),
                                                   MacAddress.valueOf(IPv6.getMCastMacAddress(soliciteIp)),
                                                   iface.vlan());
            }
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                               .setOutput(iface.connectPoint().port()).build();
            OutboundPacket packet = new DefaultOutboundPacket(iface.connectPoint().deviceId(),
                                               treatment, ByteBuffer.wrap(neighbourReq.serialize()));
            packetService.emit(packet);
        }
        return true;
    }

    @Override
    public void dumpToStream(String subject, OutputStream out) {
        SimpleFabricEvent event = new SimpleFabricEvent(SimpleFabricEvent.Type.SIMPLE_FABRIC_DUMP, subject, out);
        dump(event.subject(), event.out());  // dump in itself
        process(event);  // dump in sub modules
    }

    // Dump handler
    protected void dump(String subject, PrintStream out) {
        if ("show".equals(subject)) {
            out.println("Static Configuration Flag:");
            out.println("    ALLOW_IPV6=" + ALLOW_IPV6);
            out.println("    ALLOW_ETH_ADDRESS_SELECTOR=" + ALLOW_ETH_ADDRESS_SELECTOR);
            out.println("    REACTIVE_SINGLE_TO_SINGLE=" + REACTIVE_SINGLE_TO_SINGLE);
            out.println("    REACTIVE_ALLOW_LINK_CP=" + REACTIVE_ALLOW_LINK_CP);
            out.println("    REACTIVE_HASHED_PATH_SELECTION=" + REACTIVE_HASHED_PATH_SELECTION);
            out.println("    REACTIVE_MATCH_IP_PROTO=" + REACTIVE_MATCH_IP_PROTO);
            out.println("");
            out.println("SimpleFabricAppId:");
            out.println("    " + appId());
            out.println("");
            out.println("fabricNetworks:");
            for (FabricNetwork fabricNetwork : fabricNetworks()) {
                out.println("    " + fabricNetwork);
            }
            out.println("");
            out.println("fabricSubnets:");
            for (FabricSubnet fabricIpSubnet : defaultFabricSubnets()) {
                out.println("    " + fabricIpSubnet);
            }
            out.println("");
            out.println("fabricRoutes:");
            for (FabricRoute route : fabricRoutes()) {
                out.println("    " + route);
            }
        }
    }

    // Refresh action thread and notifier

    private class InternalRefreshThread extends Thread {
        @Override
        public void run() {
            while (true) {
                boolean doRefreshMarked = false;
                boolean doFlushMarked = false;
                synchronized (refreshMonitor) {
                    if (!doRefresh && !doFlush) {
                        try {
                            refreshMonitor.wait(IDLE_INTERVAL_MSEC);
                        } catch (InterruptedException e) {
                            log.warn("run thread interrupted", e);
                            Thread.currentThread().interrupt();
                        }
                    }
                    doRefreshMarked = doRefresh;
                    doRefresh = false;
                    doFlushMarked = doFlush;
                    doFlush = false;
                }
                if (doRefreshMarked) {
                    try {
                        refresh();
                    } catch (Exception e) {
                        log.warn("simple fabric refresh failed: exception={}", e);
                    }
                }
                if (doFlushMarked) {
                    try {
                        log.info("simple fabric flush execute");
                        process(new SimpleFabricEvent(SimpleFabricEvent.Type.SIMPLE_FABRIC_FLUSH, "flush"));
                    } catch (Exception e) {
                        log.warn("simple fabric flush failed: exception={}", e);
                    }
                }
                if (!doRefreshMarked && !doFlushMarked) {
                    try {
                        if (!refresh()) {
                            process(new SimpleFabricEvent(SimpleFabricEvent.Type.SIMPLE_FABRIC_IDLE, "idle"));
                        }
                    } catch (Exception e) {
                        log.warn("simple fabric idle failed: exception={}", e);
                    }
                }
            }
        }
    }

    @Override
    public void triggerRefresh() {
        synchronized (refreshMonitor) {
            doRefresh = true;
            refreshMonitor.notifyAll();
        }
    }

    @Override
    public void triggerFlush() {
        synchronized (refreshMonitor) {
            doFlush = true;
            refreshMonitor.notifyAll();
        }
    }

    // Service Listeners

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
            case CONFIG_REGISTERED:
            case CONFIG_UNREGISTERED:
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
            case CONFIG_REMOVED:
                if (event.configClass().equals(SimpleFabricConfig.class)) {
                    triggerRefresh();
                }
                break;
            default:
                break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
            case DEVICE_AVAILABILITY_CHANGED:
            case DEVICE_REMOVED:
            case DEVICE_SUSPENDED:
            case DEVICE_UPDATED:
            case PORT_ADDED:
            case PORT_REMOVED:
            case PORT_UPDATED:
            // case PORT_STATS_UPDATED:  IGNORED
                triggerRefresh();
                break;
            default:
                break;
            }
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            Host prevHost = event.prevSubject();
            switch (event.type()) {
            case HOST_MOVED:
            case HOST_REMOVED:
            case HOST_ADDED:
            case HOST_UPDATED:
                triggerRefresh();
                break;
            default:
                break;
            }
        }
    }

    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            Interface iface = event.subject();
            Interface prevIface = event.prevSubject();
            switch (event.type()) {
            case INTERFACE_ADDED:
            case INTERFACE_REMOVED:
            case INTERFACE_UPDATED:
                triggerRefresh();
                break;
            default:
                break;
            }
        }
    }

}

