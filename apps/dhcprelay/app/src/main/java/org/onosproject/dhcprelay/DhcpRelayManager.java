/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.dhcprelay;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import org.onlab.packet.ARP;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCP6;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcprelay.api.DhcpHandler;
import org.onosproject.dhcprelay.api.DhcpRelayService;
import org.onosproject.dhcprelay.api.DhcpServerInfo;
import org.onosproject.dhcprelay.config.DefaultDhcpRelayConfig;
import org.onosproject.dhcprelay.config.DhcpServerConfig;
import org.onosproject.dhcprelay.config.EnableDhcpFpmConfig;
import org.onosproject.dhcprelay.config.HostAutoRelearnConfig;
import org.onosproject.dhcprelay.config.IgnoreDhcpConfig;
import org.onosproject.dhcprelay.config.IndirectDhcpRelayConfig;
import org.onosproject.mastership.MastershipService;
import org.onosproject.dhcprelay.store.DhcpFpmPrefixStore;
import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.dhcprelay.store.DhcpRelayStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.Port;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.fpm.api.FpmRecord;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.ARP_ENABLED;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.ARP_ENABLED_DEFAULT;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.DHCP_FPM_ENABLED;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.DHCP_FPM_ENABLED_DEFAULT;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.DHCP_POLL_INTERVAL;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.DHCP_POLL_INTERVAL_DEFAULT;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.DHCP_PROBE_INTERVAL;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.DHCP_PROBE_INTERVAL_DEFAULT;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.DHCP_PROBE_COUNT;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.DHCP_PROBE_COUNT_DEFAULT;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * DHCP Relay Agent Application Component.
 */
@Component(
    immediate = true,
    service = DhcpRelayService.class,
    property = {
        ARP_ENABLED + ":Boolean=" + ARP_ENABLED_DEFAULT,
        DHCP_POLL_INTERVAL + ":Integer=" + DHCP_POLL_INTERVAL_DEFAULT,
        DHCP_FPM_ENABLED + ":Boolean=" + DHCP_FPM_ENABLED_DEFAULT,
        DHCP_PROBE_INTERVAL + ":Integer=" + DHCP_PROBE_INTERVAL_DEFAULT,
        DHCP_PROBE_COUNT + ":Integer=" + DHCP_PROBE_COUNT_DEFAULT
    }
)
public class DhcpRelayManager implements DhcpRelayService {
    public static final String DHCP_RELAY_APP = "org.onosproject.dhcprelay";
    public static final String ROUTE_STORE_IMPL = "org.onosproject.routeservice.store.RouteStoreImpl";

    private static final int DEFAULT_POOL_SIZE = 32;

    private static final TrafficSelector ARP_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_ARP)
            .build();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final InternalConfigListener cfgListener = new InternalConfigListener();
    private CopyOnWriteArraySet<DeviceId> hostAutoRelearnEnabledDevices = new CopyOnWriteArraySet<DeviceId>();

    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<ApplicationId, DefaultDhcpRelayConfig>(APP_SUBJECT_FACTORY,
                    DefaultDhcpRelayConfig.class,
                    DefaultDhcpRelayConfig.KEY,
                    true) {
                @Override
                public DefaultDhcpRelayConfig createConfig() {
                    return new DefaultDhcpRelayConfig();
                }
            },
            new ConfigFactory<ApplicationId, IndirectDhcpRelayConfig>(APP_SUBJECT_FACTORY,
                    IndirectDhcpRelayConfig.class,
                    IndirectDhcpRelayConfig.KEY,
                    true) {
                @Override
                public IndirectDhcpRelayConfig createConfig() {
                    return new IndirectDhcpRelayConfig();
                }
            },
            new ConfigFactory<ApplicationId, IgnoreDhcpConfig>(APP_SUBJECT_FACTORY,
                    IgnoreDhcpConfig.class,
                    IgnoreDhcpConfig.KEY,
                    true) {
                @Override
                public IgnoreDhcpConfig createConfig() {
                    return new IgnoreDhcpConfig();
                }
            },
            new ConfigFactory<ApplicationId, EnableDhcpFpmConfig>(APP_SUBJECT_FACTORY,
                    EnableDhcpFpmConfig.class,
                    EnableDhcpFpmConfig.KEY,
                    false) {
                @Override
                public EnableDhcpFpmConfig createConfig() {
                    return new EnableDhcpFpmConfig();
                }
            },
            new ConfigFactory<ApplicationId, HostAutoRelearnConfig>(APP_SUBJECT_FACTORY,
                    HostAutoRelearnConfig.class,
                    HostAutoRelearnConfig.KEY,
                    true) {
                @Override
                public HostAutoRelearnConfig createConfig() {
                    return new HostAutoRelearnConfig();
                }
            }
    );


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DhcpRelayStore dhcpRelayStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService compCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DhcpFpmPrefixStore dhcpFpmPrefixStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY,
            target = "(_version=4)")
    protected DhcpHandler v4Handler;

    @Reference(cardinality = ReferenceCardinality.MANDATORY,
            target = "(_version=6)")
    protected DhcpHandler v6Handler;

    /** Enable Address resolution protocol. */
    protected boolean arpEnabled = ARP_ENABLED_DEFAULT;

    /** dhcp relay poll interval. */
    protected int dhcpPollInterval = DHCP_POLL_INTERVAL_DEFAULT;

    /** Enable DhcpRelay Fpm. */
    protected boolean dhcpFpmEnabled = DHCP_FPM_ENABLED_DEFAULT;

    /** Dhcp host relearn probe interval in millis. */
    protected int dhcpHostRelearnProbeInterval = DHCP_PROBE_INTERVAL_DEFAULT;

    /** Dhcp host relearn probe count. */
    protected int dhcpHostRelearnProbeCount = DHCP_PROBE_COUNT_DEFAULT;

    private ScheduledExecutorService timerExecutor;
    private ScheduledExecutorService executorService = null;
    protected ExecutorService devEventExecutor;
    private ExecutorService packetExecutor;

    protected DeviceListener deviceListener = new InternalDeviceListener();
    private DhcpRelayPacketProcessor dhcpRelayPacketProcessor = new DhcpRelayPacketProcessor();
    private ApplicationId appId;
    private static final int POOL_SIZE = 10;
    private static final int HOST_PROBE_INIT_DELAY = 500;

    /**
     *   One second timer.
     */
    class Dhcp6Timer implements Runnable {
        @Override
        public void run() {
            v6Handler.timeTick();
        }
    };

    @Activate
    protected void activate(ComponentContext context) {
        //start the dhcp relay agent
        appId = coreService.registerApplication(DHCP_RELAY_APP);

        cfgService.addListener(cfgListener);
        factories.forEach(cfgService::registerConfigFactory);
        //update the dhcp server configuration.
        updateConfig();

        //add the packet processor
        packetService.addProcessor(dhcpRelayPacketProcessor, PacketProcessor.director(0));

        timerExecutor = Executors.newScheduledThreadPool(1,
                groupedThreads("onos/dhcprelay", "config-reloader-%d", log));
        timerExecutor.scheduleAtFixedRate(new Dhcp6Timer(), 0, dhcpPollInterval, TimeUnit.SECONDS);
        packetExecutor = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE,
                groupedThreads("onos/dhcprelay", "packet-%d", log));

        devEventExecutor = newSingleThreadScheduledExecutor(
                             groupedThreads("onos/dhcprelay-dev-events", "events-%d", log));

        modified(context);

        // Enable distribute route store
        compCfgService.preSetProperty(ROUTE_STORE_IMPL,
                               "distributed", Boolean.TRUE.toString());
        compCfgService.registerProperties(getClass());

        executorService = Executors.newScheduledThreadPool(POOL_SIZE);

        deviceService.addListener(deviceListener);

        log.info("DHCP-RELAY Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        factories.forEach(cfgService::unregisterConfigFactory);
        packetService.removeProcessor(dhcpRelayPacketProcessor);
        cancelArpPackets();
        compCfgService.unregisterProperties(getClass(), false);
        deviceService.removeListener(deviceListener);
        timerExecutor.shutdown();
        devEventExecutor.shutdownNow();
        devEventExecutor = null;
        packetExecutor.shutdown();
        timerExecutor = null;
        packetExecutor = null;
        executorService.shutdown();

        log.info("DHCP-RELAY Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, ARP_ENABLED);
        if (flag != null) {
            arpEnabled = flag;
            log.info("Address resolution protocol is {}",
                    arpEnabled ? "enabled" : "disabled");
        }

        if (arpEnabled) {
            requestArpPackets();
        } else {
            cancelArpPackets();
        }

        int intervalVal = Tools.getIntegerProperty(properties, DHCP_POLL_INTERVAL);
        log.info("DhcpRelay poll interval new {} old {}", intervalVal, dhcpPollInterval);
        if (intervalVal !=  dhcpPollInterval) {
            timerExecutor.shutdown();
            dhcpPollInterval = intervalVal;
            timerExecutor = Executors.newScheduledThreadPool(1,
                    groupedThreads("dhcpRelay",
                            "config-reloader-%d", log));
            timerExecutor.scheduleAtFixedRate(new Dhcp6Timer(),
                        0,
                        dhcpPollInterval > 1 ? dhcpPollInterval : 1,
                        TimeUnit.SECONDS);
            v6Handler.setDhcp6PollInterval(dhcpPollInterval);
        }

        flag = Tools.isPropertyEnabled(properties, DHCP_FPM_ENABLED);
        if (flag != null) {
            boolean oldValue = dhcpFpmEnabled;
            dhcpFpmEnabled = flag;
            log.info("DhcpRelay FPM is {}",
                    dhcpFpmEnabled ? "enabled" : "disabled");

            if (dhcpFpmEnabled && !oldValue) {
                log.info("Dhcp Fpm is enabled.");
                processDhcpFpmRoutes(true);
            }
            if (!dhcpFpmEnabled && oldValue) {
                log.info("Dhcp Fpm is disabled.");
                processDhcpFpmRoutes(false);
            }
            v6Handler.setDhcpFpmEnabled(dhcpFpmEnabled);
        }
    }

    private static List<TrafficSelector> buildClientDhcpSelectors() {
        return Streams.concat(Dhcp4HandlerImpl.DHCP_SELECTORS.stream(),
                              Dhcp6HandlerImpl.DHCP_SELECTORS.stream())
                .collect(Collectors.toList());
    }

    /**
     * Updates DHCP relay app configuration.
     */
    private void updateConfig() {
        DefaultDhcpRelayConfig defaultConfig =
                cfgService.getConfig(appId, DefaultDhcpRelayConfig.class);
        IndirectDhcpRelayConfig indirectConfig =
                cfgService.getConfig(appId, IndirectDhcpRelayConfig.class);
        IgnoreDhcpConfig ignoreDhcpConfig =
                cfgService.getConfig(appId, IgnoreDhcpConfig.class);
        HostAutoRelearnConfig hostAutoRelearnConfig =
                cfgService.getConfig(appId, HostAutoRelearnConfig.class);

        if (defaultConfig != null) {
            updateConfig(defaultConfig);
        }
        if (indirectConfig != null) {
            updateConfig(indirectConfig);
        }
        if (ignoreDhcpConfig != null) {
            updateConfig(ignoreDhcpConfig);
        }
        if (hostAutoRelearnConfig != null) {
            updateConfig(hostAutoRelearnConfig);
        }
    }

    /**
     * Updates DHCP relay app configuration with given configuration.
     *
     * @param config the configuration ot update
     */
    protected void updateConfig(Config config) {
        if (config instanceof IndirectDhcpRelayConfig) {
            IndirectDhcpRelayConfig indirectConfig = (IndirectDhcpRelayConfig) config;
            v4Handler.setIndirectDhcpServerConfigs(indirectConfig.dhcpServerConfigs());
            v6Handler.setIndirectDhcpServerConfigs(indirectConfig.dhcpServerConfigs());
        } else if (config instanceof DefaultDhcpRelayConfig) {
            DefaultDhcpRelayConfig defaultConfig = (DefaultDhcpRelayConfig) config;
            v4Handler.setDefaultDhcpServerConfigs(defaultConfig.dhcpServerConfigs());
            v6Handler.setDefaultDhcpServerConfigs(defaultConfig.dhcpServerConfigs());
        }
        if (config instanceof IgnoreDhcpConfig) {
            v4Handler.updateIgnoreVlanConfig((IgnoreDhcpConfig) config);
            v6Handler.updateIgnoreVlanConfig((IgnoreDhcpConfig) config);
        }
        if (config instanceof HostAutoRelearnConfig) {
            setHostAutoRelearnConfig((HostAutoRelearnConfig) config);
        }
    }

    protected void removeConfig(Config config) {
        if (config instanceof IndirectDhcpRelayConfig) {
            v4Handler.setIndirectDhcpServerConfigs(Collections.emptyList());
            v6Handler.setIndirectDhcpServerConfigs(Collections.emptyList());
        } else if (config instanceof DefaultDhcpRelayConfig) {
            v4Handler.setDefaultDhcpServerConfigs(Collections.emptyList());
            v6Handler.setDefaultDhcpServerConfigs(Collections.emptyList());
        }
        if (config instanceof IgnoreDhcpConfig) {
            v4Handler.updateIgnoreVlanConfig(null);
            v6Handler.updateIgnoreVlanConfig(null);
        }
    }

    private void processDhcpFpmRoutes(Boolean add) {
        // needs to restore/remove fpm
    }

    public boolean isDhcpFpmEnabled() {
        return dhcpFpmEnabled;
    }

    /**
     * Request ARP packet in via PacketService.
     */
    private void requestArpPackets() {
        packetService.requestPackets(ARP_SELECTOR, PacketPriority.CONTROL, appId);
    }

    /**
     * Cancel requested ARP packets in via packet service.
     */
    private void cancelArpPackets() {
        packetService.cancelPackets(ARP_SELECTOR, PacketPriority.CONTROL, appId);
    }

    @Override
    public Optional<DhcpRecord> getDhcpRecord(HostId hostId) {
        return dhcpRelayStore.getDhcpRecord(hostId);
    }

    @Override
    public Collection<DhcpRecord> getDhcpRecords() {
        return dhcpRelayStore.getDhcpRecords();
    }
    @Override
    public void updateDhcpRecord(HostId hostId, DhcpRecord dhcpRecord) {
        dhcpRelayStore.updateDhcpRecord(hostId, dhcpRecord);
    }
    @Override
    public Optional<MacAddress> getDhcpServerMacAddress() {
        // TODO: depreated it
        DefaultDhcpRelayConfig config = cfgService.getConfig(appId, DefaultDhcpRelayConfig.class);
        DhcpServerConfig serverConfig = config.dhcpServerConfigs().get(0);
        Ip4Address serverip = serverConfig.getDhcpServerIp4().get();
        return hostService.getHostsByIp(serverip)
                .stream()
                .map(Host::mac)
                .findFirst();
    }

    @Override
    public List<DhcpServerInfo> getDefaultDhcpServerInfoList() {
        return ImmutableList.<DhcpServerInfo>builder()
                .addAll(v4Handler.getDefaultDhcpServerInfoList())
                .addAll(v6Handler.getDefaultDhcpServerInfoList())
                .build();
    }

    @Override
    public List<DhcpServerInfo> getIndirectDhcpServerInfoList() {
        return ImmutableList.<DhcpServerInfo>builder()
                .addAll(v4Handler.getIndirectDhcpServerInfoList())
                .addAll(v6Handler.getIndirectDhcpServerInfoList())
                .build();
    }

    /**
     * Gets DHCP data from a packet.
     *
     * @param packet the packet
     * @return the DHCP data; empty if it is not a DHCP packet
     */
    private Optional<DHCP> findDhcp(Ethernet packet) {
        return Stream.of(packet)
                .filter(Objects::nonNull)
                .map(Ethernet::getPayload)
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

    /**
     * Gets DHCPv6 data from a packet.
     *
     * @param packet the packet
     * @return the DHCPv6 data; empty if it is not a DHCPv6 packet
     */
    private Optional<DHCP6> findDhcp6(Ethernet packet) {
        return Stream.of(packet)
                .filter(Objects::nonNull)
                .map(Ethernet::getPayload)
                .filter(p -> p instanceof IPv6)
                .map(IPacket::getPayload)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof UDP)
                .map(IPacket::getPayload)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof DHCP6)
                .map(p -> (DHCP6) p)
                .findFirst();
    }


    private class DhcpRelayPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            packetExecutor.execute(() -> processInternal(context));
        }

        private void processInternal(PacketContext context) {
            // process the packet and get the payload
            Ethernet packet = context.inPacket().parsed();
            if (packet == null) {
                return;
            }

            findDhcp(packet).ifPresent(dhcpPayload -> {
                v4Handler.processDhcpPacket(context, dhcpPayload);
            });

            findDhcp6(packet).ifPresent(dhcp6Payload -> {
                v6Handler.processDhcpPacket(context, dhcp6Payload);
            });

            if (packet.getEtherType() == Ethernet.TYPE_ARP && arpEnabled) {
                ARP arpPacket = (ARP) packet.getPayload();
                VlanId vlanId = VlanId.vlanId(packet.getVlanID());
                Set<Interface> interfaces = interfaceService.
                        getInterfacesByPort(context.inPacket().receivedFrom());
                //ignore the packets if dhcp server interface is not configured on onos.
                if (interfaces.isEmpty()) {
                    log.warn("server virtual interface not configured");
                    return;
                }
                if ((arpPacket.getOpCode() != ARP.OP_REQUEST)) {
                    // handle request only
                    return;
                }
                MacAddress interfaceMac = interfaces.stream()
                        .filter(iface -> iface.vlan().equals(vlanId))
                        .map(Interface::mac)
                        .filter(mac -> !mac.equals(MacAddress.NONE))
                        .findFirst()
                        .orElse(MacAddress.ONOS);
                if (interfaceMac == null) {
                    // can't find interface mac address
                    return;
                }
                processArpPacket(context, packet, interfaceMac);
            }
        }

        /**
         * Processes the ARP Payload and initiates a reply to the client.
         *
         * @param context the packet context
         * @param packet the ethernet payload
         * @param replyMac mac address to be replied
         */
        private void processArpPacket(PacketContext context, Ethernet packet, MacAddress replyMac) {
            ARP arpPacket = (ARP) packet.getPayload();
            ARP arpReply = arpPacket.duplicate();
            arpReply.setOpCode(ARP.OP_REPLY);

            arpReply.setTargetProtocolAddress(arpPacket.getSenderProtocolAddress());
            arpReply.setTargetHardwareAddress(arpPacket.getSenderHardwareAddress());
            arpReply.setSenderProtocolAddress(arpPacket.getTargetProtocolAddress());
            arpReply.setSenderHardwareAddress(replyMac.toBytes());

            // Ethernet Frame.
            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(replyMac.toBytes());
            ethReply.setDestinationMACAddress(packet.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_ARP);
            ethReply.setVlanID(packet.getVlanID());
            ethReply.setPayload(arpReply);

            ConnectPoint targetPort = context.inPacket().receivedFrom();
            TrafficTreatment t = DefaultTrafficTreatment.builder()
                    .setOutput(targetPort.port()).build();
            OutboundPacket o = new DefaultOutboundPacket(
                    targetPort.deviceId(), t, ByteBuffer.wrap(ethReply.serialize()));
            if (log.isTraceEnabled()) {
                log.trace("Relaying ARP packet {} to {}", packet, targetPort);
            }
            packetService.emit(o);
        }
    }

    /**
     * Listener for network config events.
     */
    private class InternalConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
                case CONFIG_UPDATED:
                case CONFIG_ADDED:
                    event.config().ifPresent(config -> {
                        updateConfig(config);
                        log.info("{} updated", config.getClass().getSimpleName());
                    });
                    break;
                case CONFIG_REMOVED:
                    event.prevConfig().ifPresent(config -> {
                        removeConfig(config);
                        log.info("{} removed", config.getClass().getSimpleName());
                    });
                    break;
                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                    break;
                default:
                    log.warn("Unsupported event type {}", event.type());
                    break;
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            if (event.configClass().equals(DefaultDhcpRelayConfig.class) ||
                    event.configClass().equals(IndirectDhcpRelayConfig.class) ||
                    event.configClass().equals(IgnoreDhcpConfig.class) ||
                    event.configClass().equals(HostAutoRelearnConfig.class)) {
                return true;
            }
            log.debug("Ignore irrelevant event class {}", event.configClass().getName());
            return false;
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
          if (devEventExecutor != null) {
            final Device device = event.subject();
            switch (event.type()) {
                case DEVICE_ADDED:
                    devEventExecutor.execute(this::updateIgnoreVlanConfigs);
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    devEventExecutor.execute(() -> deviceAvailabilityChanged(device));
                    break;
                case PORT_UPDATED:
                    Port port = event.port();
                    devEventExecutor.execute(() -> portUpdatedEventHandler(device, port));
                    break;
                default:
                    break;
            }
          }
        }

        private void portUpdatedEventHandler(Device device, Port port) {
            if (!mastershipService.isLocalMaster(device.id())) {
                log.warn("This instance is not the master for the device {}", device.id());
                return;
            }
            if (hostAutoRelearnEnabledDevices.contains(device.id()) && port.isEnabled()) {
                ConnectPoint cp = new ConnectPoint(device.id(), port.number());
                HostLocation hostLocation = new HostLocation(cp, 0);
                Set<DhcpRecord> records = dhcpRelayStore.getDhcpRecords()
                                          .stream()
                                          .filter(i -> i.directlyConnected())
                                          .filter(i -> i.locations().contains(hostLocation))
                                          .collect(Collectors.toSet());

                for (DhcpRecord i : records) {
                    //found a dhcprecord matching the connect point of the port event
                    log.debug("portUpdatedEventHandler:DHCP record {}, sending msg on CP {} Mac {} Vlan{} DeviceId {}",
                            i, cp, i.macAddress(), i.vlanId(), device.id());
                    if (i.ip4Address().isPresent()) {
                        log.warn("Sending host relearn probe for v4 not supported for Mac {} Vlan{} ip {}",
                             i.macAddress(), i.vlanId(), i.ip4Address());
                    } else if (i.ip6Address().isPresent()) {
                        sendHostRelearnProbe(cp, i.macAddress(), i.vlanId(), i.ip6Address());
                    }
                 }
            }
        }

        private void deviceAvailabilityChanged(Device device) {
            if (deviceService.isAvailable(device.id())) {
                updateIgnoreVlanConfigs();
            } else {
                removeIgnoreVlanState();
            }
        }

        private void updateIgnoreVlanConfigs() {
            IgnoreDhcpConfig config = cfgService.getConfig(appId, IgnoreDhcpConfig.class);
            v4Handler.updateIgnoreVlanConfig(config);
            v6Handler.updateIgnoreVlanConfig(config);
        }

        private void removeIgnoreVlanState() {
            IgnoreDhcpConfig config = cfgService.getConfig(appId, IgnoreDhcpConfig.class);
            v4Handler.removeIgnoreVlanState(config);
            v6Handler.removeIgnoreVlanState(config);
        }
    }

    private void setHostAutoRelearnConfig(HostAutoRelearnConfig config) {
        hostAutoRelearnEnabledDevices.clear();
        if (config == null) {
            return;
        }
        hostAutoRelearnEnabledDevices.addAll(config.hostAutoRelearnEnabledDevices());
    }

    //  Packet transmission class.
    private class PktTransmitter implements Runnable {

        MacAddress mac;
        VlanId vlanId;
        Ip6Address ipv6Address;
        ConnectPoint connectPoint;

        PktTransmitter(MacAddress mac, VlanId vlanId, Ip6Address ipv6Address, ConnectPoint connectPoint) {
            this.mac = mac;
            this.vlanId = vlanId;
            this.ipv6Address = ipv6Address;
            this.connectPoint = connectPoint;
        }

        @Override
        public void run() {
            log.debug("Host Relearn probe packet transmission activated for Mac {} Vlan {} Ip {} ConnectPt {}",
                                     mac, vlanId, ipv6Address, connectPoint);
            if (mac == null || vlanId == null || ipv6Address == null || connectPoint == null) {
                return;
            }

            Interface senderInterface = interfaceService.getInterfacesByPort(connectPoint)
                    .stream().filter(iface -> Dhcp6HandlerUtil.interfaceContainsVlan(iface, vlanId))
                    .findFirst().orElse(null);
            if (senderInterface == null) {
                log.warn("Cannot get sender interface for from packet, abort... vlan {}", vlanId.toString());
                return;
            }
            MacAddress senderMacAddress = senderInterface.mac();
            byte[] senderIpAddress = IPv6.getLinkLocalAddress(senderMacAddress.toBytes());
            byte[] destIp = IPv6.getSolicitNodeAddress(ipv6Address.toOctets());

            Ethernet ethernet = NeighborSolicitation.buildNdpSolicit(
                    this.ipv6Address,
                    Ip6Address.valueOf(senderIpAddress),
                    Ip6Address.valueOf(destIp), //destip
                    senderMacAddress,
                    this.mac,
                    this.vlanId);
            sendHostRelearnProbeToConnectPoint(ethernet, connectPoint);

            log.debug("Host Relearn Probe transmission completed.");
        }
    }

    //Create packet and schedule transmitter thread.
    private void sendHostRelearnProbe(ConnectPoint connectPoint, MacAddress mac, VlanId vlanId,
                                      Optional<Ip6Address> ipv6Address) {
        PktTransmitter nsTransmitter = new PktTransmitter(mac, vlanId, ipv6Address.get(), connectPoint);
        executorService.schedule(nsTransmitter, HOST_PROBE_INIT_DELAY, TimeUnit.MILLISECONDS);
    }

    // Send Host Relearn Probe packets to ConnectPoint
    private void sendHostRelearnProbeToConnectPoint(Ethernet nsPacket, ConnectPoint connectPoint) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(connectPoint.port()).build();
        OutboundPacket outboundPacket = new DefaultOutboundPacket(connectPoint.deviceId(),
                treatment, ByteBuffer.wrap(nsPacket.serialize()));
        int counter = 0;
        try {
            while (counter < dhcpHostRelearnProbeCount) {
              packetService.emit(outboundPacket);
              counter++;
              Thread.sleep(dhcpHostRelearnProbeInterval);
            }
        } catch (Exception e) {
            log.error("Exception while emmiting packet {}", e.getMessage(), e);
        }
    }


    public Optional<FpmRecord> getFpmRecord(IpPrefix prefix) {
        return dhcpFpmPrefixStore.getFpmRecord(prefix);
    }

    public Collection<FpmRecord> getFpmRecords() {
        return dhcpFpmPrefixStore.getFpmRecords();
    }

    @Override
    public void addFpmRecord(IpPrefix prefix, FpmRecord fpmRecord) {
        dhcpFpmPrefixStore.addFpmRecord(prefix, fpmRecord);
    }

    @Override
    public Optional<FpmRecord> removeFpmRecord(IpPrefix prefix) {
        return dhcpFpmPrefixStore.removeFpmRecord(prefix);
    }


}
