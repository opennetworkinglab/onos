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

import com.google.common.collect.ImmutableMap;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceEvent;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.ra.config.RouterAdvertisementDeviceConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.ra.OsgiPropertyConstants.RA_FLAG_MBIT_STATUS;
import static org.onosproject.ra.OsgiPropertyConstants.RA_FLAG_MBIT_STATUS_DEFAULT;
import static org.onosproject.ra.OsgiPropertyConstants.RA_FLAG_OBIT_STATUS;
import static org.onosproject.ra.OsgiPropertyConstants.RA_FLAG_OBIT_STATUS_DEFAULT;
import static org.onosproject.ra.OsgiPropertyConstants.RA_GLOBAL_PREFIX_CONF_STATUS;
import static org.onosproject.ra.OsgiPropertyConstants.RA_GLOBAL_PREFIX_CONF_STATUS_DEFAULT;
import static org.onosproject.ra.OsgiPropertyConstants.RA_OPTION_PREFIX_STATUS;
import static org.onosproject.ra.OsgiPropertyConstants.RA_OPTION_PREFIX_STATUS_DEFAULT;
import static org.onosproject.ra.OsgiPropertyConstants.RA_THREADS_DELAY;
import static org.onosproject.ra.OsgiPropertyConstants.RA_THREADS_DELAY_DEFAULT;
import static org.onosproject.ra.OsgiPropertyConstants.RA_THREADS_POOL;
import static org.onosproject.ra.OsgiPropertyConstants.RA_THREADS_POOL_SIZE_DEFAULT;

/**
 * Manages IPv6 Router Advertisements.
 */
@Component(
    immediate = true,
    service = RoutingAdvertisementService.class,
    property = {
        RA_THREADS_POOL + ":Integer=" + RA_THREADS_POOL_SIZE_DEFAULT,
        RA_THREADS_DELAY + ":Integer=" + RA_THREADS_DELAY_DEFAULT,
        RA_FLAG_MBIT_STATUS + ":Boolean=" + RA_FLAG_MBIT_STATUS_DEFAULT,
        RA_FLAG_OBIT_STATUS + ":Boolean=" + RA_FLAG_OBIT_STATUS_DEFAULT,
        RA_OPTION_PREFIX_STATUS + ":Boolean=" + RA_OPTION_PREFIX_STATUS_DEFAULT,
        RA_GLOBAL_PREFIX_CONF_STATUS  + ":Boolean=" + RA_GLOBAL_PREFIX_CONF_STATUS_DEFAULT
    }
)
public class RouterAdvertisementManager implements RoutingAdvertisementService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry networkConfigRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    /** Thread pool capacity. */
    protected int raPoolSize = RA_THREADS_POOL_SIZE_DEFAULT;

    /** Thread delay in seconds. */
    protected int raThreadDelay = RA_THREADS_DELAY_DEFAULT;

    /** Turn M-bit flag on/off. */
    protected boolean raFlagMbitStatus = RA_FLAG_MBIT_STATUS_DEFAULT;

    /** Turn O-bit flag on/off. */
    protected boolean raFlagObitStatus = RA_FLAG_OBIT_STATUS_DEFAULT;

    /** Prefix option support needed or not. */
    protected boolean raOptionPrefixStatus = RA_OPTION_PREFIX_STATUS_DEFAULT;

    /** Global prefix configuration support on/off. */
    protected boolean raGlobalPrefixConfStatus = RA_GLOBAL_PREFIX_CONF_STATUS_DEFAULT;

    @GuardedBy(value = "this")
    private final Map<ConnectPoint, Map.Entry<ScheduledFuture<?>, List<InterfaceIpAddress>>> transmitters =
            new LinkedHashMap<>();

    // TODO: should consider using concurrent variants
    @GuardedBy(value = "this")
    private final Map<DeviceId, List<InterfaceIpAddress>> globalPrefixes = new LinkedHashMap<>();

    @Override
    public synchronized ImmutableMap<DeviceId, List<InterfaceIpAddress>> getGlobalPrefixes() {
        return ImmutableMap.copyOf(globalPrefixes);
    }

    @SuppressWarnings("GuardedBy")
    @GuardedBy(value = "this")
    private Function<Interface, Map.Entry<ConnectPoint, List<InterfaceIpAddress>>> prefixGenerator =
            i -> {
                Map.Entry<ConnectPoint, List<InterfaceIpAddress>> prefixEntry;
                if (raGlobalPrefixConfStatus && globalPrefixes.containsKey(i.connectPoint().deviceId())) {
                    prefixEntry = new AbstractMap.SimpleEntry<>(i.connectPoint(),
                            globalPrefixes.get(i.connectPoint().deviceId()));
                } else {
                    prefixEntry = new AbstractMap.SimpleEntry<>(i.connectPoint(), i.ipAddressesList());
                }
                return prefixEntry;
            };

    private ScheduledExecutorService executors = null;

    private static final String APP_NAME = "org.onosproject.routeradvertisement";
    private ApplicationId appId;

    private final ConfigFactory<DeviceId, RouterAdvertisementDeviceConfig> deviceConfigFactory =
            new ConfigFactory<DeviceId, RouterAdvertisementDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    RouterAdvertisementDeviceConfig.class, "routeradvertisement") {
                @Override
                public RouterAdvertisementDeviceConfig createConfig() {

                    return new RouterAdvertisementDeviceConfig();
                }
            };

    // Listener for handling dynamic interface modifications.
    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            switch (event.type()) {
                case INTERFACE_ADDED:
                case INTERFACE_UPDATED:
                    clearTxWorkers();
                    loadGlobalPrefixConfig();
                    setupTxWorkers();
                    log.info("Configuration updated for {}", event.subject());
                    break;
                case INTERFACE_REMOVED:
                    Interface i = event.subject();
                    if (mastershipService.getLocalRole(i.connectPoint().deviceId())
                            == MastershipRole.MASTER) {
                        deactivateRouterAdvertisement(i.connectPoint());
                    }
                    break;
                default:
            }
        }
    }

    private final InterfaceListener interfaceListener = new InternalInterfaceListener();

    // Enables RA threads on 'connectPoint' with configured IPv6s
    private synchronized void activateRouterAdvertisement(ConnectPoint connectPoint,
                                                          List<InterfaceIpAddress> addresses) {
        RAWorkerThread worker = new RAWorkerThread(connectPoint, addresses, raThreadDelay, null, null);
        ScheduledFuture<?> handler = executors.scheduleAtFixedRate(worker, raThreadDelay,
                raThreadDelay, TimeUnit.SECONDS);
        transmitters.put(connectPoint, new AbstractMap.SimpleEntry<>(handler, addresses));
    }

    // Disables already activated RA threads on 'connectPoint'
    private synchronized List<InterfaceIpAddress> deactivateRouterAdvertisement(ConnectPoint connectPoint) {
        if (connectPoint != null) {
            Map.Entry<ScheduledFuture<?>, List<InterfaceIpAddress>> details = transmitters.get(connectPoint);
            details.getKey().cancel(false);
            transmitters.remove(connectPoint);
            return details.getValue();
        }
        return null;
    }

    private synchronized void setupThreadPool() {
        executors = Executors.newScheduledThreadPool(raPoolSize,
                groupedThreads("RouterAdvertisement", "event-%d", log));
    }

    private synchronized void clearThreadPool() {
        executors.shutdown();
    }

    // Start Tx threads for all configured interfaces.
    private synchronized void setupTxWorkers() {
        interfaceService.getInterfaces()
                .stream()
                .filter(i -> mastershipService.getLocalRole(i.connectPoint().deviceId())
                        == MastershipRole.MASTER)
                .map(prefixGenerator::apply)
                .filter(i -> i.getValue()
                        .stream()
                        .anyMatch(ia -> ia.ipAddress().version().equals(IpAddress.Version.INET6)))
                .forEach(j ->
                        activateRouterAdvertisement(j.getKey(), j.getValue())
                );
    }

    // Clear out Tx threads.
    private synchronized void clearTxWorkers() {
        transmitters.entrySet().stream().forEach(i -> i.getValue().getKey().cancel(false));
        transmitters.clear();
    }

    private synchronized void setupPoolAndTxWorkers() {
        setupThreadPool();
        setupTxWorkers();
    }

    private synchronized void clearPoolAndTxWorkers() {
        clearTxWorkers();
        clearThreadPool();
    }

    @SuppressWarnings("GuardedBy")
    // Loading global prefixes for devices from network configuration
    private synchronized void loadGlobalPrefixConfig() {
        globalPrefixes.clear();
        Set<DeviceId> deviceSubjects =
                networkConfigRegistry.getSubjects(DeviceId.class, RouterAdvertisementDeviceConfig.class);
        deviceSubjects.forEach(subject -> {
            RouterAdvertisementDeviceConfig config =
                    networkConfigRegistry.getConfig(subject, RouterAdvertisementDeviceConfig.class);
            if (config != null) {
                List<InterfaceIpAddress> ips = config.prefixes();
                globalPrefixes.put(subject, ips);
            }
        });
    }

    // Handler for network configuration updates
    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(RouterAdvertisementDeviceConfig.class)
                    || event.configClass().equals(InterfaceConfig.class)) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED:
                        clearTxWorkers();
                        loadGlobalPrefixConfig();
                        setupTxWorkers();
                        log.info("Configuration updated for {}", event.subject());
                        break;
                    default:
                }
            }
        }
    }

    private final InternalNetworkConfigListener networkConfigListener
            = new InternalNetworkConfigListener();

    // Handler for device updates
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                case PORT_UPDATED:
                case PORT_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_AVAILABILITY_CHANGED:
                    clearTxWorkers();
                    setupTxWorkers();
                    log.trace("Processed device event {} on {}", event.type(), event.subject());
                    break;
                default:
            }
        }
    }

    private class InternalMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            switch (event.type()) {
                case MASTER_CHANGED:
                    clearTxWorkers();
                    setupTxWorkers();
                    log.trace("Processed mastership event {} on {}", event.type(), event.subject());
                    break;
                case BACKUPS_CHANGED:
                case SUSPENDED:
                default:
                    break;
            }
        }
    }

    private final InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private final InternalMastershipListener internalMastershipListener = new InternalMastershipListener();

    // Processor for Solicited RA packets
    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            // Ensure packet is IPv6 Solicited RA
            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();
            if ((ethernet == null) || (ethernet.getEtherType() != Ethernet.TYPE_IPV6)) {
                return;
            }
            IPv6 ipv6Packet = (IPv6) ethernet.getPayload();
            if (ipv6Packet.getNextHeader() != IPv6.PROTOCOL_ICMP6) {
                return;
            }
            ICMP6 icmp6Packet = (ICMP6) ipv6Packet.getPayload();
            if (icmp6Packet.getIcmpType() != ICMP6.ROUTER_SOLICITATION) {
                return;
            }

            // Start solicited-RA handling thread
            SolicitedRAWorkerThread sraWorkerThread = new SolicitedRAWorkerThread(pkt);
            executors.schedule(sraWorkerThread, 0, TimeUnit.SECONDS);
        }
    }

    InternalPacketProcessor processor = null;

    @Activate
    protected void activate(ComponentContext context) {
        // Basic application registrations.
        appId = coreService.registerApplication(APP_NAME);
        componentConfigService.registerProperties(getClass());

        // Packet processor for handling Router Solicitations
        processor = new InternalPacketProcessor();
        packetService.addProcessor(processor, PacketProcessor.director(3));

        // Setup global prefix loading components
        networkConfigRegistry.addListener(networkConfigListener);
        networkConfigRegistry.registerConfigFactory(deviceConfigFactory);
        loadGlobalPrefixConfig();

        // Register device and mastership event listener
        deviceService.addListener(internalDeviceListener);
        mastershipService.addListener(internalMastershipListener);

        // Setup pool and worker threads for existing interfaces
        setupPoolAndTxWorkers();
    }


    @Modified
    protected void modified(ComponentContext context) {
        int newRaPoolSize, newRaThreadDelay;

        // Loading configured properties.
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            try {
                // Handle change in pool size
                String s = get(properties, RA_THREADS_POOL);
                newRaPoolSize = isNullOrEmpty(s) ?
                        RA_THREADS_POOL_SIZE_DEFAULT : Integer.parseInt(s.trim());
                if (newRaPoolSize != raPoolSize) {
                    raPoolSize = newRaPoolSize;
                    clearPoolAndTxWorkers();
                    setupPoolAndTxWorkers();
                    log.info("Thread pool size updated to {}", raPoolSize);
                }

                // Handle change in thread delay
                s = get(properties, RA_THREADS_DELAY);
                newRaThreadDelay = isNullOrEmpty(s) ?
                        RA_THREADS_DELAY_DEFAULT : Integer.parseInt(s.trim());
                if (newRaThreadDelay != raThreadDelay) {
                    raThreadDelay = newRaThreadDelay;
                    clearTxWorkers();
                    setupTxWorkers();
                    log.info("Thread delay updated to {}", raThreadDelay);
                }

                // Handle M-flag changes
                s = get(properties, RA_FLAG_MBIT_STATUS);
                if (!isNullOrEmpty(s)) {
                    raFlagMbitStatus = Boolean.parseBoolean(s.trim());
                    log.info("RA M-flag set {}", s);
                }

                // Handle O-flag changes
                s = get(properties, RA_FLAG_OBIT_STATUS);
                if (!isNullOrEmpty(s)) {
                    raFlagObitStatus = Boolean.parseBoolean(s.trim());
                    log.info("RA O-flag set {}", s);
                }

                // Handle prefix option configuration
                s = get(properties, RA_OPTION_PREFIX_STATUS);
                if (!isNullOrEmpty(s)) {
                    raOptionPrefixStatus = Boolean.parseBoolean(s.trim());
                    String status = raOptionPrefixStatus ? "enabled" : "disabled";
                    log.info("RA prefix option {}", status);
                }

                s = get(properties, RA_GLOBAL_PREFIX_CONF_STATUS);
                if (!isNullOrEmpty(s)) {
                    raGlobalPrefixConfStatus = Boolean.parseBoolean(s.trim());
                    clearTxWorkers();
                    setupTxWorkers();
                    String status = raOptionPrefixStatus ? "enabled" : "disabled";
                    log.info("RA global configuration file loading {}", status);
                }

            } catch (NumberFormatException e) {
                log.warn("Component configuration had invalid value, aborting changes loading.", e);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        // Unregister resources.
        componentConfigService.unregisterProperties(getClass(), false);
        interfaceService.removeListener(interfaceListener);
        networkConfigRegistry.removeListener(networkConfigListener);
        networkConfigRegistry.unregisterConfigFactory(deviceConfigFactory);
        packetService.removeProcessor(processor);
        deviceService.removeListener(internalDeviceListener);
        mastershipService.removeListener(internalMastershipListener);

        // Clear pool & threads
        clearPoolAndTxWorkers();
    }

    // Worker thread for actually sending ICMPv6 RA packets.
    private class RAWorkerThread implements Runnable {

        ConnectPoint connectPoint;
        List<InterfaceIpAddress> ipAddresses;
        int retransmitPeriod;
        MacAddress solicitHostMac;
        byte[] solicitHostAddress;

        // Various fixed values in RA packet
        public static final byte RA_HOP_LIMIT = (byte) 0xff;
        public static final short RA_ROUTER_LIFETIME = (short) 1800;
        public static final int RA_OPTIONS_BUFFER_SIZE = 500;
        public static final int RA_OPTION_MTU_VALUE = 1500;
        public static final int RA_OPTION_PREFIX_VALID_LIFETIME = 600;
        public static final int RA_OPTION_PREFIX_PREFERRED_LIFETIME = 600;
        public static final int RA_RETRANSMIT_CALIBRATION_PERIOD = 1;


        RAWorkerThread(ConnectPoint connectPoint, List<InterfaceIpAddress> ipAddresses, int period,
                       MacAddress macAddress, byte[] ipv6Address) {
            this.connectPoint = connectPoint;
            this.ipAddresses = ipAddresses;
            retransmitPeriod = period;
            solicitHostMac = macAddress;
            solicitHostAddress = ipv6Address;
        }

        @Override
        public void run() {
            // Router Advertisement header filling. Please refer RFC-2461.
            RouterAdvertisement ra = new RouterAdvertisement();
            ra.setCurrentHopLimit(RA_HOP_LIMIT);
            ra.setMFlag((byte) (raFlagMbitStatus ? 0x01 : 0x00));
            ra.setOFlag((byte) (raFlagObitStatus ? 0x01 : 0x00));
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
            if (raOptionPrefixStatus) {
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
            }

            // ICMPv6 header filling.
            ICMP6 icmpv6 = new ICMP6();
            icmpv6.setIcmpType(ICMP6.ROUTER_ADVERTISEMENT);
            icmpv6.setIcmpCode((byte) 0);
            icmpv6.setPayload(ra);

            // IPv6 header filling.
            byte[] ip6AllNodesAddress = Ip6Address.valueOf("ff02::1").toOctets();
            IPv6 ipv6 = new IPv6();
            ipv6.setDestinationAddress((solicitHostAddress == null) ? ip6AllNodesAddress : solicitHostAddress);
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
            // Provide unicast address for Solicit RA replays
            ethernet.setDestinationMACAddress((solicitHostMac == null) ?
                    MacAddress.valueOf(l2Ipv6MulticastAddress) : solicitHostMac);
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
            log.trace("Transmitted Unsolicited Router Advertisement on {}", connectPoint);
        }
    }

    // Worker thread for processing IPv6 Solicited RA packets.
    public class SolicitedRAWorkerThread implements Runnable {

        private InboundPacket packet;

        SolicitedRAWorkerThread(InboundPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {

            // TODO : Validate Router Solicitation

            // Pause already running unsolicited RA threads in received connect point
            ConnectPoint connectPoint = packet.receivedFrom();
            List<InterfaceIpAddress> addresses = deactivateRouterAdvertisement(connectPoint);

            /* Multicast RA(ie. Unsolicited RA) TX time is not preciously tracked so to make sure that
             * Unicast RA(ie. Router Solicitation Response) is TXed before Mulicast RA
             * logic adapted here is disable Mulicast RA, TX Unicast RA and then restore Multicast RA.
             */
            log.trace("Processing Router Solicitations from {}", connectPoint);
            try {
                Ethernet ethernet = packet.parsed();
                IPv6 ipv6 = (IPv6) ethernet.getPayload();
                RAWorkerThread worker = new RAWorkerThread(connectPoint,
                        addresses, raThreadDelay, ethernet.getSourceMAC(), ipv6.getSourceAddress());
                // TODO : Estimate TX time as in RFC 4861, Section 6.2.6 and schedule TX based on it
                CompletableFuture<Void> sraHandlerFuture = CompletableFuture.runAsync(worker, executors);
                sraHandlerFuture.get();
            } catch (Exception e) {
                log.error("Failed to respond to router solicitation. {}", e);
            } finally {
                activateRouterAdvertisement(connectPoint, addresses);
                log.trace("Restored Unsolicited Router Advertisements on {}", connectPoint);
            }
        }
    }

}
