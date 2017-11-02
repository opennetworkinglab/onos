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
import org.apache.felix.scr.annotations.Modified;
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
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.ra.config.RouterAdvertisementDeviceConfig;
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
import java.util.Set;
import java.util.AbstractMap;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    private static final String PROP_RA_FLAG_MBIT_STATUS = "raFlagMbitStatus";
    private static final boolean DEFAULT_RA_FLAG_MBIT_STATUS = false;
    private static final String PROP_RA_FLAG_OBIT_STATUS = "raFlagObitStatus";
    private static final boolean DEFAULT_RA_FLAG_OBIT_STATUS = false;
    private static final String PROP_RA_OPTION_PREFIX_STATUS = "raOptionPrefixStatus";
    private static final boolean DEFAULT_RA_OPTION_PREFIX_STATUS = false;
    private static final String PROP_RA_GLOBAL_PREFIX_CONF_STATUS = "raGlobalPrefixConfStatus";
    private static final boolean DEFAULT_RA_GLOBAL_PREFIX_CONF_STATUS = true;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfigRegistry;

    @Property(name = PROP_RA_THREADS_POOL, intValue = DEFAULT_RA_THREADS_POOL_SIZE,
            label = "Thread pool capacity")
    protected int raPoolSize = DEFAULT_RA_THREADS_POOL_SIZE;

    @Property(name = PROP_RA_THREADS_DELAY, intValue = DEFAULT_RA_THREADS_DELAY,
            label = "Thread delay in seconds")
    protected int raThreadDelay = DEFAULT_RA_THREADS_DELAY;

    @Property(name = PROP_RA_FLAG_MBIT_STATUS, boolValue = DEFAULT_RA_FLAG_MBIT_STATUS,
            label = "Turn M-bit flag on/off")
    protected boolean raFlagMbitStatus = DEFAULT_RA_FLAG_MBIT_STATUS;

    @Property(name = PROP_RA_FLAG_OBIT_STATUS, boolValue = DEFAULT_RA_FLAG_OBIT_STATUS,
            label = "Turn O-bit flag on/off")
    protected boolean raFlagObitStatus = DEFAULT_RA_FLAG_OBIT_STATUS;

    @Property(name = PROP_RA_OPTION_PREFIX_STATUS, boolValue = DEFAULT_RA_OPTION_PREFIX_STATUS,
            label = "Prefix option support needed or not")
    protected boolean raOptionPrefixStatus = DEFAULT_RA_OPTION_PREFIX_STATUS;

    @Property(name = PROP_RA_GLOBAL_PREFIX_CONF_STATUS, boolValue = DEFAULT_RA_GLOBAL_PREFIX_CONF_STATUS,
            label = "Global prefix configuration support on/off")
    protected boolean raGlobalConfigStatus = DEFAULT_RA_GLOBAL_PREFIX_CONF_STATUS;

    @GuardedBy(value = "this")
    private final Map<ConnectPoint, ScheduledFuture<?>> transmitters = new LinkedHashMap<>();

    @GuardedBy(value = "this")
    private final Map<DeviceId, List<InterfaceIpAddress>> globalPrefixes = new LinkedHashMap<>();

    private Function<Interface, Map.Entry<ConnectPoint, List<InterfaceIpAddress>>> prefixGenerator =
            i -> {
                Map.Entry<ConnectPoint, List<InterfaceIpAddress>> prefixEntry;
                if (raGlobalConfigStatus && globalPrefixes.containsKey(i.connectPoint().deviceId())) {
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
                    if (mastershipService.getLocalRole(i.connectPoint().deviceId())
                            == MastershipRole.MASTER) {
                        deactivateRouterAdvertisement(i.connectPoint(),
                                i.ipAddressesList());
                        activateRouterAdvertisement(i.connectPoint(),
                                i.ipAddressesList());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private final InterfaceListener interfaceListener = new InternalInterfaceListener();

    // Enables RA threads on 'connectPoint' with configured IPv6s
    private synchronized void activateRouterAdvertisement(ConnectPoint connectPoint,
                                                          List<InterfaceIpAddress> addresses) {
        RAWorkerThread worker = new RAWorkerThread(connectPoint, addresses, raThreadDelay);
        ScheduledFuture<?> handler = executors.scheduleAtFixedRate(worker, raThreadDelay,
                raThreadDelay, TimeUnit.SECONDS);
        transmitters.put(connectPoint, handler);
    }

    // Disables already activated RA threads on 'connectPoint'
    private synchronized void deactivateRouterAdvertisement(ConnectPoint connectPoint,
                                                            List<InterfaceIpAddress> addresses) {
        // Note : Parameter addresses not used now, kept for future.
        if (connectPoint != null) {
            ScheduledFuture<?> handler = transmitters.get(connectPoint);
            handler.cancel(false);
            transmitters.remove(connectPoint);
        }
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
        transmitters.entrySet().stream().forEach(i -> i.getValue().cancel(false));
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

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(RouterAdvertisementDeviceConfig.class)) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                        log.info("Router Advertisement device Config added for {}", event.subject());
                        clearTxWorkers();
                        loadGlobalPrefixConfig();
                        setupTxWorkers();
                        break;
                    case CONFIG_UPDATED:
                        log.info("Router Advertisement device updated for {}", event.subject());
                        clearTxWorkers();
                        loadGlobalPrefixConfig();
                        setupTxWorkers();
                        break;
                    default :
                        break;
                }
            }
        }

    }
    private final InternalNetworkConfigListener networkConfigListener
                = new InternalNetworkConfigListener();


    @Activate
    protected void activate(ComponentContext context) {
        // Basic application registrations.
        appId = coreService.registerApplication(APP_NAME);
        componentConfigService.registerProperties(getClass());

        // Setup global prefix loading components
        networkConfigRegistry.addListener(networkConfigListener);
        networkConfigRegistry.registerConfigFactory(deviceConfigFactory);
        loadGlobalPrefixConfig();

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
                String s = get(properties, PROP_RA_THREADS_POOL);
                newRaPoolSize = isNullOrEmpty(s) ?
                        DEFAULT_RA_THREADS_POOL_SIZE : Integer.parseInt(s.trim());
                if (newRaPoolSize != raPoolSize) {
                    raPoolSize = newRaPoolSize;
                    clearPoolAndTxWorkers();
                    setupPoolAndTxWorkers();
                    log.info("Thread pool size updated to {}", raPoolSize);
                }

                // Handle change in thread delay
                s = get(properties, PROP_RA_THREADS_DELAY);
                newRaThreadDelay = isNullOrEmpty(s) ?
                        DEFAULT_RA_THREADS_DELAY : Integer.parseInt(s.trim());
                if (newRaThreadDelay != raThreadDelay) {
                    raThreadDelay = newRaThreadDelay;
                    clearTxWorkers();
                    setupTxWorkers();
                    log.info("Thread delay updated to {}", raThreadDelay);
                }

                // Handle M-flag changes
                s = get(properties, PROP_RA_FLAG_MBIT_STATUS);
                if (!isNullOrEmpty(s)) {
                    raFlagMbitStatus = Boolean.parseBoolean(s.trim());
                    log.info("RA M-flag set {}", s);
                }

                // Handle O-flag changes
                s = get(properties, PROP_RA_FLAG_OBIT_STATUS);
                if (!isNullOrEmpty(s)) {
                    raFlagObitStatus = Boolean.parseBoolean(s.trim());
                    log.info("RA O-flag set {}", s);
                }

                // Handle prefix option configuration
                s = get(properties, PROP_RA_OPTION_PREFIX_STATUS);
                if (!isNullOrEmpty(s)) {
                    raOptionPrefixStatus = Boolean.parseBoolean(s.trim());
                    String status = raOptionPrefixStatus ? "enabled" : "disabled";
                    log.info("RA prefix option {}", status);
                }

                s = get(properties, PROP_RA_GLOBAL_PREFIX_CONF_STATUS);
                if (!isNullOrEmpty(s)) {
                    raGlobalConfigStatus = Boolean.parseBoolean(s.trim());
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

        // Clear pool & threads
        clearPoolAndTxWorkers();
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
