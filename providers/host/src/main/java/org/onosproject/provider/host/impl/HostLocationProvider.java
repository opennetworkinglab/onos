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
package org.onosproject.provider.host.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCPPacketType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.packet.ipv6.IExtensionHeader;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.RouterSolicitation;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network end-station
 * hosts.
 */
@Component(immediate = true)
public class HostLocationProvider extends AbstractProvider implements HostProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private HostProviderService providerService;

    private final InternalHostProvider processor = new InternalHostProvider();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    private ApplicationId appId;

    @Property(name = "hostRemovalEnabled", boolValue = true,
            label = "Enable host removal on port/device down events")
    private boolean hostRemovalEnabled = true;

    @Property(name = "useArp", boolValue = true,
            label = "Enable using ARP for neighbor discovery by the " +
                    "Host Location Provider; default is true")
    private boolean useArp = true;

    @Property(name = "useIpv6ND", boolValue = false,
            label = "Enable using IPv6 Neighbor Discovery by the " +
                    "Host Location Provider; default is false")
    private boolean useIpv6ND = false;

    @Property(name = "useDhcp", boolValue = false,
            label = "Enable using DHCP for neighbor discovery by the " +
                    "Host Location Provider; default is false")
    private boolean useDhcp = false;

    @Property(name = "requestInterceptsEnabled", boolValue = true,
            label = "Enable requesting packet intercepts")
    private boolean requestInterceptsEnabled = true;

    protected ExecutorService eventHandler;

    private static final byte[] SENDER_ADDRESS = IpAddress.valueOf("0.0.0.0").toOctets();

    /**
     * Creates an OpenFlow host provider.
     */
    public HostLocationProvider() {
        super(new ProviderId("of", "org.onosproject.provider.host"));
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.provider.host");
        eventHandler = newSingleThreadScheduledExecutor(
                groupedThreads("onos/host-loc-provider", "event-handler", log));
        providerService = providerRegistry.register(this);
        packetService.addProcessor(processor, PacketProcessor.advisor(1));
        deviceService.addListener(deviceListener);

        modified(context);

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        withdrawIntercepts();

        providerRegistry.unregister(this);
        packetService.removeProcessor(processor);
        deviceService.removeListener(deviceListener);
        eventHandler.shutdown();
        providerService = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);

        if (requestInterceptsEnabled) {
            requestIntercepts();
        } else {
            withdrawIntercepts();
        }
    }

    /**
     * Request packet intercepts.
     */
    private void requestIntercepts() {
        // Use ARP
        TrafficSelector arpSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP)
                .build();
        if (useArp) {
            packetService.requestPackets(arpSelector, PacketPriority.CONTROL, appId);
        } else {
            packetService.cancelPackets(arpSelector, PacketPriority.CONTROL, appId);
        }

        // Use IPv6 Neighbor Discovery
        TrafficSelector ipv6NsSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchIPProtocol(IPv6.PROTOCOL_ICMP6)
                .matchIcmpv6Type(ICMP6.NEIGHBOR_SOLICITATION)
                .build();
        TrafficSelector ipv6NaSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchIPProtocol(IPv6.PROTOCOL_ICMP6)
                .matchIcmpv6Type(ICMP6.NEIGHBOR_ADVERTISEMENT)
                .build();
        if (useIpv6ND) {
            packetService.requestPackets(ipv6NsSelector, PacketPriority.CONTROL, appId);
            packetService.requestPackets(ipv6NaSelector, PacketPriority.CONTROL, appId);
        } else {
            packetService.cancelPackets(ipv6NsSelector, PacketPriority.CONTROL, appId);
            packetService.cancelPackets(ipv6NaSelector, PacketPriority.CONTROL, appId);
        }

        // Use DHCP
        TrafficSelector dhcpServerSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .build();
        TrafficSelector dhcpClientSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
                .build();
        if (useDhcp) {
            packetService.requestPackets(dhcpServerSelector, PacketPriority.CONTROL, appId);
            packetService.requestPackets(dhcpClientSelector, PacketPriority.CONTROL, appId);
        } else {
            packetService.cancelPackets(dhcpServerSelector, PacketPriority.CONTROL, appId);
            packetService.cancelPackets(dhcpClientSelector, PacketPriority.CONTROL, appId);
        }
    }

    /**
     * Withdraw packet intercepts.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);

        // IPv6 Neighbor Solicitation packet.
        selector.matchEthType(Ethernet.TYPE_IPV6);
        selector.matchIPProtocol(IPv6.PROTOCOL_ICMP6);
        selector.matchIcmpv6Type(ICMP6.NEIGHBOR_SOLICITATION);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);

        // IPv6 Neighbor Advertisement packet.
        selector.matchIcmpv6Type(ICMP6.NEIGHBOR_ADVERTISEMENT);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, "hostRemovalEnabled");
        if (flag == null) {
            log.info("Host removal on port/device down events is not configured, " +
                             "using current value of {}", hostRemovalEnabled);
        } else {
            hostRemovalEnabled = flag;
            log.info("Configured. Host removal on port/device down events is {}",
                     hostRemovalEnabled ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, "useArp");
        if (flag == null) {
            log.info("Using ARP is not configured, " +
                    "using current value of {}", useArp);
        } else {
            useArp = flag;
            log.info("Configured. Using ARP is {}",
                    useArp ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, "useIpv6ND");
        if (flag == null) {
            log.info("Using IPv6 Neighbor Discovery is not configured, " +
                             "using current value of {}", useIpv6ND);
        } else {
            useIpv6ND = flag;
            log.info("Configured. Using IPv6 Neighbor Discovery is {}",
                     useIpv6ND ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, "useDhcp");
        if (flag == null) {
            log.info("Using DHCP is not configured, " +
                    "using current value of {}", useDhcp);
        } else {
            useDhcp = flag;
            log.info("Configured. Using DHCP is {}",
                    useDhcp ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, "requestInterceptsEnabled");
        if (flag == null) {
            log.info("Request intercepts is not configured, " +
                    "using current value of {}", requestInterceptsEnabled);
        } else {
            requestInterceptsEnabled = flag;
            log.info("Configured. Request intercepts is {}",
                    requestInterceptsEnabled ? "enabled" : "disabled");
        }
    }

    @Override
    public void triggerProbe(Host host) {
        //log.info("Triggering probe on device {} ", host);

        // FIXME Disabling host probing for now, because sending packets from a
        // broadcast MAC address caused problems when two ONOS networks were
        // interconnected. Host probing should take into account the interface
        // configuration when determining which source address to use.

        //MastershipRole role = deviceService.getRole(host.location().deviceId());
        //if (role.equals(MastershipRole.MASTER)) {
        //    host.ipAddresses().forEach(ip -> {
        //        sendProbe(host, ip);
        //    });
        //} else {
        //    log.info("not the master, master will probe {}");
        //}
    }

    private void sendProbe(Host host, IpAddress targetIp) {
        Ethernet probePacket = null;
        if (targetIp.isIp4()) {
            // IPv4: Use ARP
            probePacket = buildArpRequest(targetIp, host);
        } else {
            // IPv6: Use Neighbor Discovery
            //TODO need to implement ndp probe
            log.info("Triggering probe on device {} ", host);
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(host.location().port()).build();

        OutboundPacket outboundPacket = new DefaultOutboundPacket(host.location().deviceId(), treatment,
                ByteBuffer.wrap(probePacket.serialize()));

        packetService.emit(outboundPacket);
    }

    // This method is using source ip as 0.0.0.0 , to receive the reply even from the sub net hosts.
    private Ethernet buildArpRequest(IpAddress targetIp, Host host) {

        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
           .setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
           .setProtocolType(ARP.PROTO_TYPE_IP)
           .setProtocolAddressLength((byte) IpAddress.INET_BYTE_LENGTH)
           .setOpCode(ARP.OP_REQUEST);

        arp.setSenderHardwareAddress(MacAddress.BROADCAST.toBytes())
                .setSenderProtocolAddress(SENDER_ADDRESS)
                .setTargetHardwareAddress(MacAddress.BROADCAST.toBytes())
                .setTargetProtocolAddress(targetIp.toOctets());

        Ethernet ethernet = new Ethernet();
        ethernet.setEtherType(Ethernet.TYPE_ARP)
                .setDestinationMACAddress(MacAddress.BROADCAST)
                .setSourceMACAddress(MacAddress.BROADCAST).setPayload(arp);

        ethernet.setPad(true);
        return ethernet;
    }

    private class InternalHostProvider implements PacketProcessor {
        /**
         * Create or update host information.
         * Will not update IP if IP is null, all zero or self-assigned.
         *
         * @param hid  host ID
         * @param mac  source Mac address
         * @param vlan VLAN ID
         * @param hloc host location
         * @param ip   source IP address or null if not updating
         */
        private void createOrUpdateHost(HostId hid, MacAddress mac,
                                        VlanId vlan, HostLocation hloc,
                                        IpAddress ip) {
            HostDescription desc = ip == null || ip.isZero() || ip.isSelfAssigned() ?
                    new DefaultHostDescription(mac, vlan, hloc) :
                    new DefaultHostDescription(mac, vlan, hloc, ip);
            try {
                providerService.hostDetected(hid, desc, false);
            } catch (IllegalStateException e) {
                log.debug("Host {} suppressed", hid);
            }
        }

        /**
         * Updates IP address for an existing host.
         *
         * @param hid host ID
         * @param ip IP address
         */
        private void updateHostIp(HostId hid, IpAddress ip) {
            Host host = hostService.getHost(hid);
            if (host == null) {
                log.debug("Fail to update IP for {}. Host does not exist");
                return;
            }

            HostDescription desc = new DefaultHostDescription(hid.mac(), hid.vlanId(),
                    host.location(), ip);
            try {
                providerService.hostDetected(hid, desc, false);
            } catch (IllegalStateException e) {
                log.debug("Host {} suppressed", hid);
            }
        }

        @Override
        public void process(PacketContext context) {
            if (context == null) {
                return;
            }

            Ethernet eth = context.inPacket().parsed();
            if (eth == null) {
                return;
            }

            MacAddress srcMac = eth.getSourceMAC();
            if (srcMac.isBroadcast() || srcMac.isMulticast()) {
                return;
            }

            VlanId vlan = VlanId.vlanId(eth.getVlanID());
            ConnectPoint heardOn = context.inPacket().receivedFrom();

            // If this arrived on control port, bail out.
            if (heardOn.port().isLogical()) {
                return;
            }

            // If this is not an edge port, bail out.
            Topology topology = topologyService.currentTopology();
            if (topologyService.isInfrastructure(topology, heardOn)) {
                return;
            }

            HostLocation hloc = new HostLocation(heardOn, System.currentTimeMillis());
            HostId hid = HostId.hostId(eth.getSourceMAC(), vlan);

            // ARP: possible new hosts, update both location and IP
            if (eth.getEtherType() == Ethernet.TYPE_ARP) {
                ARP arp = (ARP) eth.getPayload();
                IpAddress ip = IpAddress.valueOf(IpAddress.Version.INET,
                                                 arp.getSenderProtocolAddress());
                createOrUpdateHost(hid, srcMac, vlan, hloc, ip);

            // IPv4: update location only
            // DHCP ACK: additionally update IP of DHCP client
            } else if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
                IPacket pkt = eth.getPayload();
                if (pkt != null && pkt instanceof IPv4) {
                    pkt = pkt.getPayload();
                    if (pkt != null && pkt instanceof UDP) {
                        pkt = pkt.getPayload();
                        if (pkt != null && pkt instanceof DHCP) {
                            DHCP dhcp = (DHCP) pkt;
                            if (dhcp.getOptions().stream()
                                    .anyMatch(dhcpOption -> dhcpOption.getCode() ==
                                            DHCP.DHCPOptionCode.OptionCode_MessageType.getValue() &&
                                            dhcpOption.getLength() == 1 &&
                                            dhcpOption.getData()[0] == DHCPPacketType.DHCPACK.getValue())) {
                                MacAddress hostMac = MacAddress.valueOf(dhcp.getClientHardwareAddress());
                                VlanId hostVlan = VlanId.vlanId(eth.getVlanID());
                                HostId hostId = HostId.hostId(hostMac, hostVlan);
                                updateHostIp(hostId, IpAddress.valueOf(dhcp.getYourIPAddress()));
                            }
                        }
                    }
                }
                createOrUpdateHost(hid, srcMac, vlan, hloc, null);

            //
            // NeighborAdvertisement and NeighborSolicitation: possible
            // new hosts, update both location and IP.
            //
            // IPv6: update location only
            } else if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
                IPv6 ipv6 = (IPv6) eth.getPayload();
                IpAddress ip = IpAddress.valueOf(IpAddress.Version.INET6,
                                                 ipv6.getSourceAddress());

                // skip extension headers
                IPacket pkt = ipv6;
                while (pkt.getPayload() != null &&
                        pkt.getPayload() instanceof IExtensionHeader) {
                    pkt = pkt.getPayload();
                }

                // Neighbor Discovery Protocol
                pkt = pkt.getPayload();
                if (pkt != null && pkt instanceof ICMP6) {
                    pkt = pkt.getPayload();
                    // RouterSolicitation, RouterAdvertisement
                    if (pkt != null && (pkt instanceof RouterAdvertisement ||
                            pkt instanceof RouterSolicitation)) {
                        return;
                    }
                    if (pkt != null && (pkt instanceof NeighborSolicitation ||
                            pkt instanceof NeighborAdvertisement)) {
                        // Duplicate Address Detection
                        if (ip.isZero()) {
                            return;
                        }
                        // NeighborSolicitation, NeighborAdvertisement
                        createOrUpdateHost(hid, srcMac, vlan, hloc, ip);
                        return;
                    }
                }

                // multicast
                if (eth.isMulticast()) {
                    return;
                }

                // normal IPv6 packets
                createOrUpdateHost(hid, srcMac, vlan, hloc, null);
            }
        }
    }

    // Auxiliary listener to device events.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            eventHandler.execute(() -> handleEvent(event));
        }

        private void handleEvent(DeviceEvent event) {
            Device device = event.subject();
            switch (event.type()) {
                case DEVICE_ADDED:
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    if (hostRemovalEnabled && !deviceService.isAvailable(device.id())) {
                        processDeviceDown(device.id());
                    }
                    break;
                case DEVICE_SUSPENDED:
                case DEVICE_UPDATED:
                    // Nothing to do?
                    break;
                case DEVICE_REMOVED:
                    if (hostRemovalEnabled) {
                        processDeviceDown(device.id());
                    }
                    break;
                case PORT_ADDED:
                    break;
                case PORT_UPDATED:
                    if (hostRemovalEnabled && !event.port().isEnabled()) {
                        processPortDown(new ConnectPoint(device.id(), event.port().number()));
                    }
                    break;
                case PORT_REMOVED:
                    // Nothing to do?
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * When a device goes down, update the location of affected hosts.
     *
     * @param deviceId the device that goes down
     */
    private void processDeviceDown(DeviceId deviceId) {
        hostService.getConnectedHosts(deviceId).forEach(affectedHost -> affectedHost.locations().stream()
                .filter(hostLocation -> hostLocation.deviceId().equals(deviceId))
                .forEach(affectedLocation ->
                        providerService.removeLocationFromHost(affectedHost.id(), affectedLocation))
        );
    }

    /**
     * When a port goes down, update the location of affected hosts.
     *
     * @param connectPoint the port that goes down
     */
    private void processPortDown(ConnectPoint connectPoint) {
        hostService.getConnectedHosts(connectPoint).forEach(affectedHost ->
                providerService.removeLocationFromHost(affectedHost.id(), new HostLocation(connectPoint, 0L))
        );
    }

}
