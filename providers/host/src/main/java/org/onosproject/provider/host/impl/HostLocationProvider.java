/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.collect.Sets;
import org.onlab.packet.ARP;
import org.onlab.packet.BasePacket;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCP6;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.packet.dhcp.Dhcp6ClientIdOption;
import org.onlab.packet.dhcp.Dhcp6IaAddressOption;
import org.onlab.packet.dhcp.Dhcp6IaNaOption;
import org.onlab.packet.dhcp.Dhcp6IaTaOption;
import org.onlab.packet.dhcp.Dhcp6RelayOption;
import org.onlab.packet.ipv6.IExtensionHeader;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.RouterSolicitation;
import org.onlab.util.PredictableExecutor;
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
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.config.basics.HostLearningConfig;
import org.onosproject.net.config.basics.SubjectFactories;
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
import org.onosproject.net.intf.InterfaceService;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.provider.host.impl.OsgiPropertyConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network end-station
 * hosts.
 */
@Component(immediate = true, service = HostProvider.class,
        property = {
                HOST_REMOVAL_ENABLED + ":Boolean=" + HOST_REMOVAL_ENABLED_DEFAULT,
                REQUEST_ARP + ":Boolean=" + REQUEST_ARP_DEFAULT,
                REQUEST_NDP + ":Boolean=" + REQUEST_NDP_DEFAULT,
                REQUEST_NDP_RS_RA + ":Boolean=" + REQUEST_NDP_RS_RA_DEFAULT,
                USE_DHCP + ":Boolean=" + USE_DHCP_DEFAULT,
                USE_DHCP6 + ":Boolean=" + USE_DHCP6_DEFAULT,
                REQUEST_INTERCEPTS_ENABLED + ":Boolean=" + REQUEST_INTERCEPTS_ENABLED_DEFAULT,
                MULTIHOMING_ENABLED + ":Boolean=" + MULTIHOMING_ENABLED_DEFAULT,
        })
public class HostLocationProvider extends AbstractProvider implements HostProvider {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    private final InternalHostProvider processor = new InternalHostProvider();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final InternalConfigListener cfgListener = new InternalConfigListener();

    private ApplicationId appId;

    /** Enable host removal on port/device down events. */
    private boolean hostRemovalEnabled = true;

    /** Request ARP packets for neighbor discovery by the Host Location Provider; default is true. */
    private boolean requestArp = true;

    /** Requests IPv6 NDP Neighbor Solicitation and Advertisement by the Host Location Provider; default is false. */
    private boolean requestIpv6ND = false;

    /** Requests IPv6 NDP Router Solicitation and Advertisement by the Host Location Provider; default is false. */
    private boolean requestIpv6NdpRsRa = false;

    /** Use DHCP to update IP address of the host; default is false. */
    private boolean useDhcp = false;

    /** Use DHCPv6 to update IP address of the host; default is false. */
    private boolean useDhcp6 = false;

    /** Enable requesting packet intercepts. */
    private boolean requestInterceptsEnabled = true;

    /** Allow hosts to be multihomed. */
    private boolean multihomingEnabled = false;

    private HostProviderService providerService;

    ExecutorService deviceEventHandler;
    private ExecutorService probeEventHandler;
    // Packet workers - 0 will leverage available processors
    private static final int DEFAULT_THREADS = 0;
    private PredictableExecutor packetWorkers;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService netcfgService;

    private ConfigFactory<ConnectPoint, HostLearningConfig> hostLearningConfig =
            new ConfigFactory<ConnectPoint, HostLearningConfig>(
                    SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY,
                    HostLearningConfig.class, "hostLearning") {
                @Override
                public HostLearningConfig createConfig() {
                    return new HostLearningConfig();
                }
            };

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
        deviceEventHandler = newSingleThreadScheduledExecutor(groupedThreads("onos/host-loc-provider",
                "device-event-handler", log));
        probeEventHandler = newSingleThreadScheduledExecutor(groupedThreads("onos/host-loc-provider",
                "probe-event-handler", log));
        packetWorkers = new PredictableExecutor(DEFAULT_THREADS, groupedThreads("onos/host-loc-provider",
                                                                                 "packet-worker-%d", log));
        providerService = providerRegistry.register(this);
        packetService.addProcessor(processor, PacketProcessor.advisor(1));
        deviceService.addListener(deviceListener);
        registry.registerConfigFactory(hostLearningConfig);
        modified(context);
        netcfgService.addListener(cfgListener);

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        withdrawIntercepts();

        providerRegistry.unregister(this);
        packetService.removeProcessor(processor);
        deviceService.removeListener(deviceListener);
        deviceEventHandler.shutdown();
        probeEventHandler.shutdown();
        packetWorkers.shutdown();
        providerService = null;
        registry.unregisterConfigFactory(hostLearningConfig);
        netcfgService.removeListener(cfgListener);
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
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP);
        if (requestArp) {
            packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);
        } else {
            packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
        }

        // Use IPv6 NDP Neighbor Solicitation and Advertisement
        selector.matchEthType(Ethernet.TYPE_IPV6)
                .matchIPProtocol(IPv6.PROTOCOL_ICMP6);
        if (requestIpv6ND) {
            selector.matchIcmpv6Type(ICMP6.NEIGHBOR_SOLICITATION);
            packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);
            selector.matchIcmpv6Type(ICMP6.NEIGHBOR_ADVERTISEMENT);
            packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);
        } else {
            selector.matchIcmpv6Type(ICMP6.NEIGHBOR_SOLICITATION);
            packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
            selector.matchIcmpv6Type(ICMP6.NEIGHBOR_ADVERTISEMENT);
            packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
        }

        // Use IPv6 NDP Router Solicitation and Advertisement
        if (requestIpv6NdpRsRa) {
            selector.matchIcmpv6Type(ICMP6.ROUTER_SOLICITATION);
            packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);
            selector.matchIcmpv6Type(ICMP6.ROUTER_ADVERTISEMENT);
            packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);
        } else {
            selector.matchIcmpv6Type(ICMP6.ROUTER_SOLICITATION);
            packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
            selector.matchIcmpv6Type(ICMP6.ROUTER_ADVERTISEMENT);
            packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
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

        flag = Tools.isPropertyEnabled(properties, HOST_REMOVAL_ENABLED);
        if (flag == null) {
            log.info("Host removal on port/device down events is not configured, " +
                     "using current value of {}", hostRemovalEnabled);
        } else {
            hostRemovalEnabled = flag;
            log.info("Configured. Host removal on port/device down events is {}",
                     hostRemovalEnabled ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, REQUEST_ARP);
        if (flag == null) {
            log.info("Using ARP is not configured, " +
                     "using current value of {}", requestArp);
        } else {
            requestArp = flag;
            log.info("Configured. Using ARP is {}",
                     requestArp ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, REQUEST_NDP);
        if (flag == null) {
            log.info("Using IPv6 Neighbor Discovery is not configured, " +
                             "using current value of {}", requestIpv6ND);
        } else {
            requestIpv6ND = flag;
            log.info("Configured. Using IPv6 NDP Neighbor Solicitation and Advertisement is {}",
                     requestIpv6ND ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, REQUEST_NDP_RS_RA);
        if (flag == null) {
            log.info("Using IPv6 Neighbor Discovery is not configured, " +
                    "using current value of {}", requestIpv6NdpRsRa);
        } else {
            requestIpv6NdpRsRa = flag;
            log.info("Configured. Using IPv6 NDP Router Solicitation and Advertisement is {}",
                    requestIpv6NdpRsRa ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, USE_DHCP);
        if (flag == null) {
            log.info("Using DHCP is not configured, " +
                     "using current value of {}", useDhcp);
        } else {
            useDhcp = flag;
            log.info("Configured. Using DHCP is {}",
                     useDhcp ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, USE_DHCP6);
        if (flag == null) {
            log.info("Using DHCP6 is not configured, " +
                             "using current value of {}", useDhcp6);
        } else {
            useDhcp6 = flag;
            log.info("Configured. Using DHCP6 is {}",
                     useDhcp6 ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, REQUEST_INTERCEPTS_ENABLED);
        if (flag == null) {
            log.info("Request intercepts is not configured, " +
                     "using current value of {}", requestInterceptsEnabled);
        } else {
            requestInterceptsEnabled = flag;
            log.info("Configured. Request intercepts is {}",
                     requestInterceptsEnabled ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, MULTIHOMING_ENABLED);
        if (flag == null) {
            log.info("Multihoming is not configured, " +
                    "using current value of {}", multihomingEnabled);
        } else {
            multihomingEnabled = flag;
            log.info("Configured. Multihoming is {}",
                    multihomingEnabled ? "enabled" : "disabled");
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
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(host.location().port()).build();

        OutboundPacket outboundPacket = new DefaultOutboundPacket(host.location().deviceId(), treatment,
                ByteBuffer.wrap(probePacket.serialize()));

        packetService.emit(outboundPacket);
    }

    // This method is using source ip as 0.0.0.0 , to receive the reply even from the sub net hosts.
    private Ethernet buildArpRequest(IpAddress targetIp, Host host) {
        return ARP.buildArpRequest(MacAddress.BROADCAST.toBytes(), Ip4Address.ZERO.toOctets(),
                MacAddress.BROADCAST.toBytes(), targetIp.toOctets(),
                MacAddress.BROADCAST.toBytes(), VlanId.NONE.toShort());
    }

    private class InternalHostProvider implements PacketProcessor {
        /**
         * Create or update host information.
         * Will not update IP if IP is null, all zero or self-assigned.
         *
         * @param hid       host ID
         * @param mac       source Mac address
         * @param vlan      VLAN ID
         * @param innerVlan inner VLAN ID
         * @param outerTpid outer TPID
         * @param hloc      host location
         * @param ip        source IP address or null if not updating
         */
        private void createOrUpdateHost(HostId hid, MacAddress mac, VlanId vlan,
                                        VlanId innerVlan, EthType outerTpid,
                                        HostLocation hloc, IpAddress ip) {
            log.debug("Creating Host {} based on Location {}", hid, hloc);
            Set<HostLocation> newLocations = Sets.newHashSet(hloc);

            if (multihomingEnabled) {
                Host existingHost = hostService.getHost(hid);
                if (existingHost != null) {
                    Set<HostLocation> prevLocations = existingHost.locations();

                    if (prevLocations.stream().noneMatch(loc -> loc.deviceId().equals(hloc.deviceId()))) {
                        // New location is on a device that we haven't seen before
                        // Could be a dual-home host.
                        newLocations.addAll(prevLocations);
                    } else {
                        // Move within the same switch
                        // Simply replace old location that is on the same device
                        prevLocations.stream().filter(loc -> !loc.deviceId().equals(hloc.deviceId()))
                                .forEach(newLocations::add);
                    }
                }
            }

            HostDescription desc = ip == null || ip.isZero() || ip.isSelfAssigned() ?
                    new DefaultHostDescription(mac, vlan, newLocations, Sets.newHashSet(),
                                               innerVlan, outerTpid, false) :
                    new DefaultHostDescription(mac, vlan, newLocations, Sets.newHashSet(ip),
                                               innerVlan, outerTpid, false);
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
                log.warn("Fail to update IP for {}. Host does not exist", hid);
                return;
            }

            HostDescription desc = new DefaultHostDescription(hid.mac(), hid.vlanId(),
                    host.locations(), Sets.newHashSet(ip), false);
            try {
                providerService.hostDetected(hid, desc, false);
            } catch (IllegalStateException e) {
                log.debug("Host {} suppressed", hid);
            }
        }

        @Override
        public void process(PacketContext context) {
            // Verify valid context
            if (context == null) {
                return;
            }
            // Verify valid Ethernet packet
            Ethernet eth = context.inPacket().parsed();
            if (eth == null) {
                return;
            }
            // Dispatch to a worker thread
            HostId hostId = HostId.hostId(eth.getSourceMAC(), VlanId.vlanId(eth.getVlanID()));
            packetWorkers.execute(() -> processPacketInternal(context), hostId.hashCode());
        }

        private void processPacketInternal(PacketContext context) {
            Ethernet eth = context.inPacket().parsed();

            MacAddress srcMac = eth.getSourceMAC();
            if (srcMac.isBroadcast() || srcMac.isMulticast()) {
                return;
            }

            VlanId vlan = VlanId.vlanId(eth.getVlanID());
            VlanId outerVlan = VlanId.vlanId(eth.getQinQVID());
            VlanId innerVlan = VlanId.NONE;
            EthType outerTpid = EthType.EtherType.UNKNOWN.ethType();
            // Set up values for double-tagged hosts
            if (outerVlan.toShort() != Ethernet.VLAN_UNTAGGED) {
                innerVlan = vlan;
                vlan = outerVlan;
                outerTpid = EthType.EtherType.lookup(eth.getQinQTPID()).ethType();
            }
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
            MacAddress destMac = eth.getDestinationMAC();

            // Ignore location probes
            if (multihomingEnabled && destMac.isOnos() && !MacAddress.NONE.equals(destMac)) {
                return;
            }

            HostLearningConfig cfg = netcfgService.getConfig(heardOn, HostLearningConfig.class);
            // if learning is disabled bail out.
            if ((cfg != null) && (!cfg.hostLearningEnabled())) {
                log.debug("Learning disabled for {}, abort.", heardOn);
                return;
            }

            // ARP: possible new hosts, update both location and IP
            if (eth.getEtherType() == Ethernet.TYPE_ARP) {
                ARP arp = (ARP) eth.getPayload();
                IpAddress ip = IpAddress.valueOf(IpAddress.Version.INET,
                                                 arp.getSenderProtocolAddress());
                createOrUpdateHost(hid, srcMac, vlan, innerVlan, outerTpid, hloc, ip);

            // IPv4: update location only
            } else if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
                // Update host location
                createOrUpdateHost(hid, srcMac, vlan, innerVlan, outerTpid, hloc, null);
                if (useDhcp) {
                    DHCP dhcp = findDhcp(eth).orElse(null);
                    // DHCP ACK: additionally update IP of DHCP client
                    if (dhcp != null  && dhcp.getPacketType().equals(DHCP.MsgType.DHCPACK)) {
                        MacAddress hostMac = MacAddress.valueOf(dhcp.getClientHardwareAddress());
                        VlanId hostVlan = VlanId.vlanId(eth.getVlanID());
                        HostId hostId = HostId.hostId(hostMac, hostVlan);
                        updateHostIp(hostId, IpAddress.valueOf(dhcp.getYourIPAddress()));
                    }
                }
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
                pkt = pkt.getPayload();

                // DHCPv6 protocol
                DHCP6 dhcp6 = findDhcp6(pkt).orElse(null);
                if (dhcp6 != null && useDhcp6) {
                    createOrUpdateHost(hid, srcMac, vlan, innerVlan, outerTpid, hloc, null);
                    handleDhcp6(dhcp6, vlan);
                    return;
                }

                if (pkt != null && pkt instanceof ICMP6) {
                    // Neighbor Discovery Protocol
                    pkt = pkt.getPayload();
                    if (pkt != null) {
                        if ((pkt instanceof RouterAdvertisement || pkt instanceof RouterSolicitation)) {
                            if (ip.isZero()) {
                                return;
                            }
                            // RouterSolicitation, RouterAdvertisement
                            createOrUpdateHost(hid, srcMac, vlan, innerVlan, outerTpid, hloc, ip);
                            return;
                        }
                        if (pkt instanceof NeighborSolicitation || pkt instanceof NeighborAdvertisement) {
                            // Duplicate Address Detection
                            if (ip.isZero()) {
                                return;
                            }
                            // NeighborSolicitation, NeighborAdvertisement
                            createOrUpdateHost(hid, srcMac, vlan, innerVlan, outerTpid, hloc, ip);

                            // Also learn from the target address of NeighborAdvertisement
                            if (pkt instanceof NeighborAdvertisement) {
                                NeighborAdvertisement na = (NeighborAdvertisement) pkt;
                                Ip6Address targetAddr = Ip6Address.valueOf(na.getTargetAddress());
                                createOrUpdateHost(hid, srcMac, vlan, innerVlan, outerTpid, hloc, targetAddr);
                            }
                            return;
                        }
                    }
                }

                // multicast, exclude DHCPv6
                if (eth.isMulticast() && dhcp6 == null) {
                    return;
                }

                // normal IPv6 packets
                createOrUpdateHost(hid, srcMac, vlan, innerVlan, outerTpid, hloc, null);
            }
        }

        /**
         * Handles DHCPv6 packet, if message type is ACK, update IP address
         * according to DHCPv6 payload (IA Address option).
         *
         * @param dhcp6 the DHCPv6 payload
         * @param vlanId the vlan of this packet
         */
        private void handleDhcp6(DHCP6 dhcp6, VlanId vlanId) {
            // extract the relay message if exist
            while (dhcp6 != null && DHCP6.RELAY_MSG_TYPES.contains(dhcp6.getMsgType())) {
                dhcp6 = dhcp6.getOptions().stream()
                        .filter(opt -> opt instanceof Dhcp6RelayOption)
                        .map(BasePacket::getPayload)
                        .map(pld -> (DHCP6) pld)
                        .findFirst()
                        .orElse(null);
            }

            if (dhcp6 == null) {
                // Can't find dhcp payload
                log.warn("Can't find dhcp payload from relay message");
                return;
            }

            if (dhcp6.getMsgType() != DHCP6.MsgType.REPLY.value()) {
                // Update IP address only when we received REPLY message
                return;
            }
            Optional<Dhcp6ClientIdOption> clientIdOption = dhcp6.getOptions()
                    .stream()
                    .filter(opt -> opt instanceof Dhcp6ClientIdOption)
                    .map(opt -> (Dhcp6ClientIdOption) opt)
                    .findFirst();

            if (!clientIdOption.isPresent()) {
                // invalid DHCPv6 option
                log.warn("Can't find client ID from DHCPv6 {}", dhcp6);
                return;
            }

            byte[] linkLayerAddr = clientIdOption.get().getDuid().getLinkLayerAddress();
            if (linkLayerAddr == null || linkLayerAddr.length != 6) {
                // No any mac address found
                log.warn("Can't find client mac from option {}", clientIdOption);
                return;
            }
            MacAddress clientMac = MacAddress.valueOf(linkLayerAddr);

            // Extract IPv6 address from IA NA ot IA TA option
            Optional<Dhcp6IaNaOption> iaNaOption = dhcp6.getOptions()
                    .stream()
                    .filter(opt -> opt instanceof Dhcp6IaNaOption)
                    .map(opt -> (Dhcp6IaNaOption) opt)
                    .findFirst();
            Optional<Dhcp6IaTaOption> iaTaOption = dhcp6.getOptions()
                    .stream()
                    .filter(opt -> opt instanceof Dhcp6IaTaOption)
                    .map(opt -> (Dhcp6IaTaOption) opt)
                    .findFirst();
            Optional<Dhcp6IaAddressOption> iaAddressOption;
            if (iaNaOption.isPresent()) {
                iaAddressOption = iaNaOption.get().getOptions().stream()
                        .filter(opt -> opt instanceof Dhcp6IaAddressOption)
                        .map(opt -> (Dhcp6IaAddressOption) opt)
                        .findFirst();
            } else if (iaTaOption.isPresent()) {
                iaAddressOption = iaTaOption.get().getOptions().stream()
                        .filter(opt -> opt instanceof Dhcp6IaAddressOption)
                        .map(opt -> (Dhcp6IaAddressOption) opt)
                        .findFirst();
            } else {
                iaAddressOption = Optional.empty();
            }
            if (iaAddressOption.isPresent()) {
                Ip6Address ip = iaAddressOption.get().getIp6Address();
                HostId hostId = HostId.hostId(clientMac, vlanId);
                updateHostIp(hostId, ip);
            } else {
                log.warn("Can't find IPv6 address from DHCPv6 {}", dhcp6);
            }
        }

        private Optional<DHCP> findDhcp(Ethernet eth) {
            IPacket pkt = eth.getPayload();
            return Stream.of(pkt)
                    .filter(Objects::nonNull)
                    .filter(p -> p instanceof IPv4)
                    .map(IPacket::getPayload)
                    .filter(Objects::nonNull)
                    .filter(p -> p instanceof UDP)
                    .map(IPacket::getPayload)
                    .filter(Objects::nonNull)
                    .filter(p -> p instanceof DHCP)
                    .map(p -> (DHCP) p)
                    .findFirst();
        }

        private Optional<DHCP6> findDhcp6(IPacket pkt) {
            return Stream.of(pkt)
                    .filter(Objects::nonNull)
                    .filter(p -> p instanceof UDP)
                    .map(IPacket::getPayload)
                    .filter(Objects::nonNull)
                    .filter(p -> p instanceof DHCP6)
                    .map(p -> (DHCP6) p)
                    .findFirst();
        }
    }

    // Auxiliary listener to device events.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            deviceEventHandler.execute(() -> handleEvent(event));
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
                    if (hostRemovalEnabled) {
                        processPortDown(new ConnectPoint(device.id(), event.port().number()));
                    }
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


    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    log.debug("HostLearningConfig event of type  {}", event.type());
                    // if learning enabled do nothing
                    HostLearningConfig learningConfig = (HostLearningConfig) event.config().get();
                    if (learningConfig.hostLearningEnabled()) {
                        return;
                    }

                    // if host learning is disable remove this location from existing, learnt hosts
                    ConnectPoint connectPoint = learningConfig.subject();
                    Set<Host> connectedHosts = hostService.getConnectedHosts(connectPoint);
                    for (Host host : connectedHosts) {
                        BasicHostConfig hostConfig = netcfgService.getConfig(host.id(), BasicHostConfig.class);

                        if ((hostConfig == null) || (!hostConfig.locations().contains(connectPoint))) {
                            // timestamp shoud not matter for comparing HostLocation and ConnectPoint
                            providerService.removeLocationFromHost(host.id(), new HostLocation(connectPoint, 1));
                        }
                    }
                    break;
                case CONFIG_REMOVED:
                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            if (!event.configClass().equals(HostLearningConfig.class)) {
                return false;
            }
            return true;
        }
    }
}
