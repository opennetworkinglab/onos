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

package org.onosproject.simplefabric;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.IPv6;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.component.ComponentService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceEvent;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Collection;
import java.util.Set;
import java.util.Map;

import static org.onosproject.simplefabric.RouteTools.createBinaryString;


/**
 * Reactive routing configuration manager.
 */
@Component(immediate = true)
@Service
public class SimpleFabricManager extends ListenerRegistry<SimpleFabricEvent, SimpleFabricListener>
        implements SimpleFabricService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    // compoents to be activated within SimpleFabric
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentService componentService;

    // SimpleFabric variables
    private ApplicationId appId = null;

    // l2 broadcast networks
    private Set<L2Network> l2Networks = new HashSet<>();
    private Set<Interface> l2NetworkInterfaces = new HashSet<>();

    // Subnet table
    private Set<IpSubnet> ipSubnets = new HashSet<>();
    private InvertedRadixTree<IpSubnet> ip4SubnetTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
    private InvertedRadixTree<IpSubnet> ip6SubnetTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());

    // Border Route table
    private Set<Route> borderRoutes = new HashSet<>();
    private InvertedRadixTree<Route> ip4BorderRouteTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
    private InvertedRadixTree<Route> ip6BorderRouteTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());

    // VirtialGateway
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
    private final InternalInterfaceListener interfaceListener = new InternalInterfaceListener();

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
        componentService.activate(appId, SimpleFabricReactiveRouting.class.getName());
        if (SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR) {
            componentService.activate(appId, SimpleFabricL2Forward.class.getName());
        }

        refreshThread = new InternalRefreshThread();
        refreshThread.start();

        log.info("simple fabric started");
    }

    @Deactivate
    public void deactivate() {
        log.info("simple fabric stopping");

        componentService.deactivate(appId, SimpleFabricNeighbour.class.getName());
        componentService.deactivate(appId, SimpleFabricReactiveRouting.class.getName());
        if (SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR) {
            componentService.deactivate(appId, SimpleFabricL2Forward.class.getName());
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
    // returns found dirty and refresh listners are called (true) or not (false)
    private boolean refresh() {
        log.debug("simple fabric refresh");
        boolean dirty = false;

        SimpleFabricConfig config = configService.getConfig(coreService.registerApplication(APP_ID),
                                                            SimpleFabricConfig.class);
        if (config == null) {
            log.debug("No simple fabric config available!");
            return false;
        }

        // l2Networks
        Set<L2Network> newL2Networks = new HashSet<>();
        Set<Interface> newL2NetworkInterfaces = new HashSet<>();
        for (L2Network newL2NetworkConfig : config.getL2Networks()) {
            L2Network newL2Network = L2Network.of(newL2NetworkConfig);

            // fill up interfaces and Hosts with active port only
            for (String ifaceName : newL2NetworkConfig.interfaceNames()) {
                Interface iface = getInterfaceByName(ifaceName);
                if (iface != null && deviceService.isAvailable(iface.connectPoint().deviceId())) {
                     newL2Network.addInterface(iface);
                     newL2NetworkInterfaces.add(iface);
                }
            }
            for (Host host : hostService.getHosts()) {
                // consider host with ip only
                if (!host.ipAddresses().isEmpty()) {
                    Interface iface = findAvailableDeviceHostInterface(host);
                    if (iface != null && newL2Network.contains(iface)) {
                        newL2Network.addHost(host);
                    }
                }
            }
            newL2Network.setDirty(true);

            // update newL2Network's dirty flags if same entry already exists
            for (L2Network prevL2Network : l2Networks) {
                if (prevL2Network.equals(newL2Network)) {
                    newL2Network.setDirty(prevL2Network.dirty());
                    break;
                }
            }
            newL2Networks.add(newL2Network);
        }
        if (!l2Networks.equals(newL2Networks)) {
            l2Networks = newL2Networks;
            dirty = true;
        }
        if (!l2NetworkInterfaces.equals(newL2NetworkInterfaces)) {
            l2NetworkInterfaces = newL2NetworkInterfaces;
            dirty = true;
        }

        // ipSubnets
        Set<IpSubnet> newIpSubnets = config.ipSubnets();
        InvertedRadixTree<IpSubnet> newIp4SubnetTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
        InvertedRadixTree<IpSubnet> newIp6SubnetTable =
                 new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
        Map<IpAddress, MacAddress> newVirtualGatewayIpMacMap = Maps.newConcurrentMap();
        for (IpSubnet subnet : newIpSubnets) {
            if (subnet.ipPrefix().isIp4()) {
                newIp4SubnetTable.put(createBinaryString(subnet.ipPrefix()), subnet);
            } else {
                newIp6SubnetTable.put(createBinaryString(subnet.ipPrefix()), subnet);
            }
            newVirtualGatewayIpMacMap.put(subnet.gatewayIp(), subnet.gatewayMac());
        }
        if (!ipSubnets.equals(newIpSubnets)) {
            ipSubnets = newIpSubnets;
            ip4SubnetTable = newIp4SubnetTable;
            ip6SubnetTable = newIp6SubnetTable;
            dirty = true;
        }
        if (!virtualGatewayIpMacMap.equals(newVirtualGatewayIpMacMap)) {
            virtualGatewayIpMacMap = newVirtualGatewayIpMacMap;
            dirty = true;
        }

        // borderRoutes config handling
        Set<Route> newBorderRoutes = config.borderRoutes();
        if (!borderRoutes.equals(newBorderRoutes)) {
            InvertedRadixTree<Route> newIp4BorderRouteTable =
                    new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
            InvertedRadixTree<Route> newIp6BorderRouteTable =
                    new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
            for (Route route : newBorderRoutes) {
                if (route.prefix().isIp4()) {
                    newIp4BorderRouteTable.put(createBinaryString(route.prefix()), route);
                } else {
                    newIp6BorderRouteTable.put(createBinaryString(route.prefix()), route);
                }
            }
            borderRoutes = newBorderRoutes;
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
    public ApplicationId getAppId() {
        if (appId == null) {
            appId = coreService.registerApplication(APP_ID);
        }
        return appId;
    }

    @Override
    public Collection<L2Network> getL2Networks() {
        return ImmutableSet.copyOf(l2Networks);
    }

    @Override
    public Set<IpSubnet> getIpSubnets() {
        return ImmutableSet.copyOf(ipSubnets);
    }

    @Override
    public Set<Route> getBorderRoutes() {
        return ImmutableSet.copyOf(borderRoutes);
    }

    @Override
    public boolean isVMac(MacAddress mac) {
        return virtualGatewayIpMacMap.containsValue(mac);
    }

    @Override
    public boolean isL2NetworkInterface(Interface intf) {
        return l2NetworkInterfaces.contains(intf);
    }

    @Override
    public MacAddress findVMacForIp(IpAddress ip) {
        return virtualGatewayIpMacMap.get(ip);
    }

    @Override
    public L2Network findL2Network(ConnectPoint port, VlanId vlanId) {
        for (L2Network l2Network : l2Networks) {
            if (l2Network.contains(port, vlanId)) {
                return l2Network;
            }
        }
        return null;
    }

    @Override
    public L2Network findL2Network(String name) {
        for (L2Network l2Network : l2Networks) {
            if (l2Network.name().equals(name)) {
                return l2Network;
            }
        }
        return null;
    }

    @Override
    public IpSubnet findIpSubnet(IpAddress ip) {
        if (ip.isIp4()) {
            return ip4SubnetTable.getValueForLongestKeyPrefixing(
                     createBinaryString(IpPrefix.valueOf(ip, Ip4Address.BIT_LENGTH)));
        } else {
            return ip6SubnetTable.getValueForLongestKeyPrefixing(
                     createBinaryString(IpPrefix.valueOf(ip, Ip6Address.BIT_LENGTH)));
        }
    }

    @Override
    public Route findBorderRoute(IpAddress ip) {
        // ASSUME: ipAddress is out of ipSubnet
        if (ip.isIp4()) {
            return ip4BorderRouteTable.getValueForLongestKeyPrefixing(
                     createBinaryString(IpPrefix.valueOf(ip, Ip4Address.BIT_LENGTH)));
        } else {
            return ip6BorderRouteTable.getValueForLongestKeyPrefixing(
                     createBinaryString(IpPrefix.valueOf(ip, Ip6Address.BIT_LENGTH)));
        }
    }


    @Override
    public Interface findHostInterface(Host host) {
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
        IpSubnet ipSubnet = findIpSubnet(ip);
        if (ipSubnet == null) {
            log.warn("simple fabric request mac failed for unknown IpSubnet: {}", ip);
            return false;
        }
        L2Network l2Network = findL2Network(ipSubnet.l2NetworkName());
        if (l2Network == null) {
            log.warn("simple fabric request mac failed for unknown l2Network name {}: {}",
                     ipSubnet.l2NetworkName(), ip);
            return false;
        }
        log.debug("simple fabric send request mac L2Network {}: {}", l2Network.name(), ip);
        for (Interface iface : l2Network.interfaces()) {
            Ethernet neighbourReq;
            if (ip.isIp4()) {
                neighbourReq = ARP.buildArpRequest(ipSubnet.gatewayMac().toBytes(),
                                                   ipSubnet.gatewayIp().toOctets(),
                                                   ip.toOctets(),
                                                   iface.vlan().toShort());
            } else {
                byte[] soliciteIp = IPv6.getSolicitNodeAddress(ip.toOctets());
                neighbourReq = NeighborSolicitation.buildNdpSolicit(
                                                   ip.toOctets(),
                                                   ipSubnet.gatewayIp().toOctets(),
                                                   soliciteIp,
                                                   ipSubnet.gatewayMac().toBytes(),
                                                   IPv6.getMCastMacAddress(soliciteIp),
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
            out.println("    ALLOW_IPV6="
                        + SimpleFabricService.ALLOW_IPV6);
            out.println("    ALLOW_ETH_ADDRESS_SELECTOR="
                        + SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR);
            out.println("    REACTIVE_SINGLE_TO_SINGLE="
                        + SimpleFabricService.REACTIVE_SINGLE_TO_SINGLE);
            out.println("    REACTIVE_ALLOW_LINK_CP="
                        + SimpleFabricService.REACTIVE_ALLOW_LINK_CP);
            out.println("    REACTIVE_HASHED_PATH_SELECTION="
                        + SimpleFabricService.REACTIVE_HASHED_PATH_SELECTION);
            out.println("    REACTIVE_MATCH_IP_PROTO="
                        + SimpleFabricService.REACTIVE_MATCH_IP_PROTO);
            out.println("");
            out.println("SimpleFabricAppId:");
            out.println("    " + getAppId());
            out.println("");
            out.println("l2Networks:");
            for (L2Network l2Network : getL2Networks()) {
                out.println("    " + l2Network);
            }
            out.println("");
            out.println("ipSubnets:");
            for (IpSubnet ipSubnet : getIpSubnets()) {
                out.println("    " + ipSubnet);
            }
            out.println("");
            out.println("borderRoutes:");
            for (Route route : getBorderRoutes()) {
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

