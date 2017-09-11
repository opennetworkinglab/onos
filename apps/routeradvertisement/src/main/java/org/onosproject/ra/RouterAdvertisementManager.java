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

package org.onosproject.ra;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceEvent;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.ComponentContext;

import javax.annotation.concurrent.GuardedBy;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Optional;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Manages IPv6 Router Advertisements.
 */
@Component(immediate = true)
public class RouterAdvertisementManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String PROP_RA_THREADS_POOL = "raPoolSize";
    private static final int DEFAULT_RA_THREADS_POOL_SIZE = 10;
    private static final String PROP_RA_THREADS_DELAY = "raThreadDelay";
    private static final int DEFAULT_RA_THREADS_DELAY = 5;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public MastershipService mastershipService;

    @Property(name = PROP_RA_THREADS_POOL, intValue = DEFAULT_RA_THREADS_POOL_SIZE,
            label = "Router Advertisement thread pool capacity")
    protected int raPoolSize = DEFAULT_RA_THREADS_POOL_SIZE;

    @Property(name = PROP_RA_THREADS_DELAY, intValue = DEFAULT_RA_THREADS_DELAY,
            label = "Router Advertisement thread delay in seconds")
    protected int raThreadDelay = DEFAULT_RA_THREADS_DELAY;

    private ScheduledExecutorService executors = null;

    @GuardedBy(value = "this")
    private final Map<ConnectPoint, ScheduledFuture<?>> transmitters = new LinkedHashMap<>();

    private static final String APP_NAME = "org.onosproject.routeradvertisement";
    private ApplicationId appId;

    // Listener for handling dynamic interface modifications.
    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            Interface i = event.subject();
            switch (event.type()) {
                case INTERFACE_ADDED:
                    if (mastershipService.getLocalRole(i.connectPoint().deviceId())
                            == MastershipRole.MASTER) {
                        activateRouterAdvertisement(i.connectPoint(),
                                i.ipAddressesList());
                    }
                    break;
                case INTERFACE_REMOVED:
                    if (mastershipService.getLocalRole(i.connectPoint().deviceId())
                            == MastershipRole.MASTER) {
                        deactivateRouterAdvertisement(i.connectPoint(),
                                i.ipAddressesList());
                    }
                    break;
                case INTERFACE_UPDATED:
                    break;
                default:
                    break;
            }
        }
    }
    private final InterfaceListener interfaceListener = new InternalInterfaceListener();

    // Enables RA threads on 'connectPoint' with configured IPv6s
    private void activateRouterAdvertisement(ConnectPoint connectPoint, List<InterfaceIpAddress> addresses) {
        synchronized (this) {
            RAWorkerThread worker = new RAWorkerThread(connectPoint, addresses, raThreadDelay);
            ScheduledFuture<?> handler = executors.scheduleAtFixedRate(worker, raThreadDelay,
                    raThreadDelay, TimeUnit.SECONDS);
            transmitters.put(connectPoint, handler);
        }

    }

    // Disables already activated RA threads on 'connectPoint'
    private void deactivateRouterAdvertisement(ConnectPoint connectPoint, List<InterfaceIpAddress> addresses) {
        synchronized (this) {
            if (connectPoint != null) {
                ScheduledFuture<?> handler = transmitters.get(connectPoint);
                handler.cancel(false);
                transmitters.remove(connectPoint);
            }
        }
    }

    @Activate
    protected void activate(ComponentContext context) {
        // Basic application registrations.
        appId = coreService.registerApplication(APP_NAME);
        componentConfigService.registerProperties(getClass());

        // Loading configured properties.
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            try {
                String s = get(properties, PROP_RA_THREADS_POOL);
                raPoolSize = isNullOrEmpty(s) ?
                        DEFAULT_RA_THREADS_POOL_SIZE : Integer.parseInt(s.trim());

                s = get(properties, PROP_RA_THREADS_DELAY);
                raThreadDelay = isNullOrEmpty(s) ?
                        DEFAULT_RA_THREADS_DELAY : Integer.parseInt(s.trim());

            } catch (NumberFormatException e) {
                log.warn("Component configuration had invalid value, loading default values.", e);
            }
        }

        // Interface listener for dynamic RA handling.
        interfaceService.addListener(interfaceListener);

        // Initialize RA thread pool
        executors = Executors.newScheduledThreadPool(raPoolSize,
                groupedThreads("RouterAdvertisement", "event-%d", log));

        // Start Router Advertisement Transmission for all configured interfaces.
        interfaceService.getInterfaces()
                .stream()
                .filter(i -> mastershipService.getLocalRole(i.connectPoint().deviceId())
                        == MastershipRole.MASTER)
                .filter(i -> i.ipAddressesList()
                        .stream()
                        .anyMatch(ia -> ia.ipAddress().version().equals(IpAddress.Version.INET6)))
                .forEach(j ->
                        activateRouterAdvertisement(j.connectPoint(), j.ipAddressesList())
                );
    }

    @Deactivate
    protected void deactivate() {
        // Unregister resources.
        componentConfigService.unregisterProperties(getClass(), false);
        interfaceService.removeListener(interfaceListener);

        // Clear out Router Advertisement Transmission for all configured interfaces.
        interfaceService.getInterfaces()
                .stream()
                .filter(i -> mastershipService.getLocalRole(i.connectPoint().deviceId())
                        == MastershipRole.MASTER)
                .filter(i -> i.ipAddressesList()
                        .stream()
                        .anyMatch(ia -> ia.ipAddress().version().equals(IpAddress.Version.INET6)))
                .forEach(j ->
                        deactivateRouterAdvertisement(j.connectPoint(), j.ipAddressesList())
                );
    }

    // Worker thread for actually sending ICMPv6 RA packets.
    private class RAWorkerThread implements Runnable {

        ConnectPoint connectPoint;
        List<InterfaceIpAddress> ipAddresses;
        int retransmitPeriod;

        // Various fixed values in RA packet
        public static final byte RA_HOP_LIMIT = (byte) 0xff;
        public static final short RA_ROUTER_LIFETIME = (short) 1800;
        public static final int RA_OPTIONS_BUFFER_SIZE = 500;
        public static final int RA_OPTION_MTU_VALUE = 1500;
        public static final int RA_OPTION_PREFIX_VALID_LIFETIME = 600;
        public static final int RA_OPTION_PREFIX_PREFERRED_LIFETIME = 600;
        public static final int RA_RETRANSMIT_CALIBRATION_PERIOD = 1;


        RAWorkerThread(ConnectPoint connectPoint, List<InterfaceIpAddress> ipAddresses, int period) {
            this.connectPoint = connectPoint;
            this.ipAddresses = ipAddresses;
            retransmitPeriod = period;
        }

        public void run() {
            // Router Advertisement header filling. Please refer RFC-2461.
            RouterAdvertisement ra = new RouterAdvertisement();
            ra.setCurrentHopLimit(RA_HOP_LIMIT);
            ra.setMFlag((byte) 0x01);
            ra.setOFlag((byte) 0x00);
            ra.setRouterLifetime(RA_ROUTER_LIFETIME);
            ra.setReachableTime(0);
            ra.setRetransmitTimer(retransmitPeriod + RA_RETRANSMIT_CALIBRATION_PERIOD);

            // Option : Source link-layer address.
            byte[] optionBuffer = new byte[RA_OPTIONS_BUFFER_SIZE];
            ByteBuffer option = ByteBuffer.wrap(optionBuffer);
            Optional<MacAddress> macAddress = interfaceService.getInterfacesByPort(connectPoint).stream()
                    .map(Interface::mac).findFirst();
            if (!macAddress.isPresent()) {
                log.warn("Unable to retrieve interface {} MAC address. Terminating RA transmission.", connectPoint);
                return;
            }
            option.put(macAddress.get().toBytes());
            ra.addOption(NeighborDiscoveryOptions.TYPE_SOURCE_LL_ADDRESS,
                    Arrays.copyOfRange(option.array(), 0, option.position()));

            // Option : MTU.
            option.rewind();
            option.putShort((short) 0);
            option.putInt(RA_OPTION_MTU_VALUE);
            ra.addOption(NeighborDiscoveryOptions.TYPE_MTU,
                    Arrays.copyOfRange(option.array(), 0, option.position()));

            // Option : Prefix information.
            ipAddresses.stream()
                    .filter(i -> i.ipAddress().version().equals(IpAddress.Version.INET6))
                    .forEach(i -> {
                        option.rewind();
                        option.put((byte) i.subnetAddress().prefixLength());
                        // Enable "onlink" option only.
                        option.put((byte) 0x80);
                        option.putInt(RA_OPTION_PREFIX_VALID_LIFETIME);
                        option.putInt(RA_OPTION_PREFIX_PREFERRED_LIFETIME);
                        // Clear reserved fields
                        option.putInt(0x00000000);
                        option.put(IpAddress.makeMaskedAddress(i.ipAddress(),
                                i.subnetAddress().prefixLength()).toOctets());
                        ra.addOption(NeighborDiscoveryOptions.TYPE_PREFIX_INFORMATION,
                                Arrays.copyOfRange(option.array(), 0, option.position()));

                    });

            // ICMPv6 header filling.
            ICMP6 icmpv6 = new ICMP6();
            icmpv6.setIcmpType(ICMP6.ROUTER_ADVERTISEMENT);
            icmpv6.setIcmpCode((byte) 0);
            icmpv6.setPayload(ra);

            // IPv6 header filling.
            byte[] ip6AllNodesAddress = Ip6Address.valueOf("ff02::1").toOctets();
            IPv6 ipv6 = new IPv6();
            ipv6.setDestinationAddress(ip6AllNodesAddress);
            /* RA packet L2 source address created from port MAC address.
             * Note : As per RFC-4861 RAs should be sent from link-local address.
             */
            ipv6.setSourceAddress(IPv6.getLinkLocalAddress(macAddress.get().toBytes()));
            ipv6.setNextHeader(IPv6.PROTOCOL_ICMP6);
            ipv6.setHopLimit(RA_HOP_LIMIT);
            ipv6.setTrafficClass((byte) 0xe0);
            ipv6.setPayload(icmpv6);

            // Ethernet header filling.
            Ethernet ethernet = new Ethernet();

            /* Ethernet IPv6 multicast address creation.
             * Refer : RFC 2624 section 7.
             */
            byte[] l2Ipv6MulticastAddress = MacAddress.IPV6_MULTICAST.toBytes();
            IntStream.range(1, 4).forEach(i -> l2Ipv6MulticastAddress[l2Ipv6MulticastAddress.length - i] =
                    ip6AllNodesAddress[ip6AllNodesAddress.length - i]);

            ethernet.setDestinationMACAddress(MacAddress.valueOf(l2Ipv6MulticastAddress));
            ethernet.setSourceMACAddress(macAddress.get().toBytes());
            ethernet.setEtherType(EthType.EtherType.IPV6.ethType().toShort());
            ethernet.setVlanID(Ethernet.VLAN_UNTAGGED);
            ethernet.setPayload(ipv6);
            ethernet.setPad(false);

            // Flush out PACKET_OUT.
            ByteBuffer stream = ByteBuffer.wrap(ethernet.serialize());
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(connectPoint.port()).build();
            OutboundPacket packet = new DefaultOutboundPacket(connectPoint.deviceId(),
                    treatment, stream);
            packetService.emit(packet);
        }
    }
}
