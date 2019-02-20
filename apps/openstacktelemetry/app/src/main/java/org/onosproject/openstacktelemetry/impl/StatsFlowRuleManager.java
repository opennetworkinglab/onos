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
package org.onosproject.openstacktelemetry.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.host.HostService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacktelemetry.api.DefaultFlowInfo;
import org.onosproject.openstacktelemetry.api.DefaultStatsFlowRule;
import org.onosproject.openstacktelemetry.api.DefaultStatsInfo;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsFlowRuleAdminService;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.packet.Ethernet.TYPE_IPV4;
import static org.onlab.packet.IPv4.PROTOCOL_TCP;
import static org.onlab.packet.IPv4.PROTOCOL_UDP;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.IP_PROTO;
import static org.onosproject.net.flow.criteria.Criterion.Type.TCP_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.TCP_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.UDP_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.UDP_SRC;
import static org.onosproject.openstacknetworking.api.Constants.STAT_FLAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_INBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_FLAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_INBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_OUTBOUND_TABLE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_DATA_POINT_SIZE;
import static org.onosproject.openstacktelemetry.api.Constants.FLAT;
import static org.onosproject.openstacktelemetry.api.Constants.OPENSTACK_TELEMETRY_APP_ID;
import static org.onosproject.openstacktelemetry.api.Constants.VLAN;
import static org.onosproject.openstacktelemetry.api.Constants.VXLAN;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_EGRESS_STATS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_EGRESS_STATS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_MONITOR_OVERLAY;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_MONITOR_OVERLAY_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_MONITOR_UNDERLAY;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_MONITOR_UNDERLAY_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_PORT_STATS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_PORT_STATS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REVERSE_PATH_STATS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REVERSE_PATH_STATS_DEFAULT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;

/**
 * Flow rule manager for network statistics of a VM.
 */
@Component(
    immediate = true,
    service = StatsFlowRuleAdminService.class,
    property = {
        PROP_REVERSE_PATH_STATS + ":Boolean=" + PROP_REVERSE_PATH_STATS_DEFAULT,
        PROP_EGRESS_STATS  + ":Boolean=" + PROP_EGRESS_STATS_DEFAULT,
        PROP_PORT_STATS + ":Boolean=" + PROP_PORT_STATS_DEFAULT,
        PROP_MONITOR_OVERLAY  + ":Boolean=" + PROP_MONITOR_OVERLAY_DEFAULT,
        PROP_MONITOR_UNDERLAY  + ":Boolean=" + PROP_MONITOR_UNDERLAY_DEFAULT
    }
)
public class StatsFlowRuleManager implements StatsFlowRuleAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final byte FLOW_TYPE_SONA = 1;

    private static final long MILLISECONDS = 1000L;
    private static final long INITIAL_DELAY = 5L;
    private static final long REFRESH_INTERVAL = 5L;
    private static final TimeUnit TIME_UNIT_SECOND = TimeUnit.SECONDS;

    private static final String OVS_DRIVER_NAME = "ovs";

    private static final String ARBITRARY_IP = "0.0.0.0/32";
    private static final int ARBITRARY_PROTOCOL = 0x0;
    private static final int ARBITRARY_LENGTH = 32;
    private static final String ARBITRARY_MAC = "00:00:00:00:00:00";
    private static final IpAddress NO_HOST_IP = IpAddress.valueOf("255.255.255.255");
    private static final MacAddress NO_HOST_MAC = MacAddress.valueOf(ARBITRARY_MAC);
    private static final int ARBITRARY_IN_INTF = 0;
    private static final int ARBITRARY_OUT_INTF = 0;

    private static final boolean RECOVER_FROM_FAILURE = true;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackTelemetryService telemetryService;

    /** A flag which indicates whether to install the rules for collecting the flow-based stats for reversed path. */
    private boolean reversePathStats = PROP_REVERSE_PATH_STATS_DEFAULT;

    /** A flag which indicates whether to install the rules for collecting the flow-based stats for egress port. */
    private boolean egressStats = PROP_EGRESS_STATS_DEFAULT;

    /** A flag which indicates whether to collect port TX & RX stats. */
    private boolean portStats = PROP_PORT_STATS_DEFAULT;

    /** A flag which indicates whether to monitor overlay network port stats. */
    private boolean monitorOverlay = PROP_MONITOR_OVERLAY_DEFAULT;

    /** A flag which indicates whether to monitor underlay network port stats. */
    private boolean monitorUnderlay = PROP_MONITOR_UNDERLAY_DEFAULT;

    private ApplicationId telemetryAppId;
    private TelemetryCollector collector;
    private ScheduledFuture result;

    private final Set<FlowInfo> gFlowInfoSet = Sets.newHashSet();
    private final Map<String, Queue<FlowInfo>> flowInfoMap = Maps.newConcurrentMap();

    private static final int SOURCE_ID = 1;
    private static final int TARGET_ID = 2;
    private static final int PRIORITY_BASE = 10000;
    private static final int METRIC_PRIORITY_SOURCE  = SOURCE_ID * PRIORITY_BASE;
    private static final int METRIC_PRIORITY_TARGET  = TARGET_ID * PRIORITY_BASE;

    @Activate
    protected void activate() {
        telemetryAppId = coreService.registerApplication(OPENSTACK_TELEMETRY_APP_ID);

        componentConfigService.registerProperties(getClass());
        start();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        flowRuleService.removeFlowRulesById(telemetryAppId);
        stop();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        readComponentConfiguration(context);

        log.info("Modified");
    }

    @Override
    public void start() {
        log.info("Start publishing thread");
        collector = new TelemetryCollector();

        result = SharedScheduledExecutors.getSingleThreadExecutor()
                    .scheduleAtFixedRate(collector, INITIAL_DELAY,
                        REFRESH_INTERVAL, TIME_UNIT_SECOND, RECOVER_FROM_FAILURE);
    }

    @Override
    public void stop() {
        log.info("Stop data publishing thread");
        result.cancel(true);
        collector = null;
    }

    @Override
    public void createStatFlowRule(StatsFlowRule statsFlowRule) {
        setStatFlowRule(statsFlowRule, true);
    }

    @Override
    public void deleteStatFlowRule(StatsFlowRule statsFlowRule) {
        setStatFlowRule(statsFlowRule, false);
    }


    @Override
    public Map<String, Queue<FlowInfo>> getFlowInfoMap() {
        return flowInfoMap;
    }


    @Override
    public Set<FlowInfo> getUnderlayFlowInfos() {

        Set<FlowInfo> flowInfos = Sets.newConcurrentHashSet();

        for (Device device : getUnderlayDevices()) {

            if (!isEdgeSwitch(device.id())) {
                continue;
            }

            for (FlowEntry entry : flowRuleService.getFlowEntries(device.id())) {
                FlowInfo.Builder fBuilder = new DefaultFlowInfo.DefaultBuilder();
                TrafficSelector selector = entry.selector();
                Criterion inPort = selector.getCriterion(Criterion.Type.IN_PORT);
                Criterion dstIpCriterion = selector.getCriterion(Criterion.Type.IPV4_DST);
                if (inPort != null && dstIpCriterion != null) {
                    IpAddress srcIp = getIpAddress(device, (PortCriterion) inPort);
                    IpAddress dstIp = ((IPCriterion) dstIpCriterion).ip().address();

                    if (srcIp == null) {
                        continue;
                    }

                    fBuilder.withFlowType(FLOW_TYPE_SONA)
                            .withSrcIp(IpPrefix.valueOf(srcIp, ARBITRARY_LENGTH))
                            .withDstIp(IpPrefix.valueOf(dstIp, ARBITRARY_LENGTH))
                            .withSrcMac(getMacAddress(srcIp))
                            .withDstMac(getMacAddress(dstIp))
                            .withInputInterfaceId(getInterfaceId(srcIp))
                            .withOutputInterfaceId(getInterfaceId(dstIp))
                            .withDeviceId(entry.deviceId());

                    StatsInfo.Builder sBuilder = new DefaultStatsInfo.DefaultBuilder();

                    sBuilder.withStartupTime(System.currentTimeMillis())
                            .withFstPktArrTime(System.currentTimeMillis())
                            .withLstPktOffset((int) (REFRESH_INTERVAL * MILLISECONDS))
                            .withCurrAccPkts((int) entry.packets())
                            .withCurrAccBytes(entry.bytes())
                            .withErrorPkts((short) 0)
                            .withDropPkts((short) 0);

                    fBuilder.withStatsInfo(sBuilder.build());

                    FlowInfo flowInfo = mergeFlowInfo(fBuilder.build(), fBuilder, sBuilder);

                    flowInfos.add(flowInfo);
                }
            }
        }

        return flowInfos;
    }

    @Override
    public Set<FlowInfo> getOverlayFlowInfos() {

        Set<FlowInfo> flowInfos = Sets.newConcurrentHashSet();

        // obtain all flow rule entries installed by telemetry app
        for (FlowEntry entry : flowRuleService.getFlowEntriesById(telemetryAppId)) {
            FlowInfo.Builder fBuilder = new DefaultFlowInfo.DefaultBuilder();
            TrafficSelector selector = entry.selector();
            IPCriterion srcIp = (IPCriterion) selector.getCriterion(IPV4_SRC);
            IPCriterion dstIp = (IPCriterion) selector.getCriterion(IPV4_DST);
            IPProtocolCriterion ipProtocol =
                    (IPProtocolCriterion) selector.getCriterion(IP_PROTO);

            fBuilder.withFlowType(FLOW_TYPE_SONA)
                    .withSrcIp(srcIp.ip())
                    .withDstIp(dstIp.ip());

            if (ipProtocol != null) {
                fBuilder.withProtocol((byte) ipProtocol.protocol());

                if (ipProtocol.protocol() == PROTOCOL_TCP) {
                    TcpPortCriterion tcpSrc =
                            (TcpPortCriterion) selector.getCriterion(TCP_SRC);
                    TcpPortCriterion tcpDst =
                            (TcpPortCriterion) selector.getCriterion(TCP_DST);
                    fBuilder.withSrcPort(tcpSrc.tcpPort());
                    fBuilder.withDstPort(tcpDst.tcpPort());
                } else if (ipProtocol.protocol() == PROTOCOL_UDP) {
                    UdpPortCriterion udpSrc =
                            (UdpPortCriterion) selector.getCriterion(UDP_SRC);
                    UdpPortCriterion udpDst =
                            (UdpPortCriterion) selector.getCriterion(UDP_DST);
                    fBuilder.withSrcPort(udpSrc.udpPort());
                    fBuilder.withDstPort(udpDst.udpPort());
                } else {
                    log.debug("Other protocol: {}", ipProtocol.protocol());
                }
            }

            fBuilder.withSrcMac(getMacAddress(srcIp.ip().address()))
                    .withDstMac(getMacAddress(dstIp.ip().address()))
                    .withInputInterfaceId(getInterfaceId(srcIp.ip().address()))
                    .withOutputInterfaceId(getInterfaceId(dstIp.ip().address()))
                    .withVlanId(getVlanId(srcIp.ip().address()))
                    .withDeviceId(entry.deviceId());

            StatsInfo.Builder sBuilder = new DefaultStatsInfo.DefaultBuilder();

            sBuilder.withStartupTime(System.currentTimeMillis())
                    .withFstPktArrTime(System.currentTimeMillis())
                    .withLstPktOffset((int) (REFRESH_INTERVAL * MILLISECONDS))
                    .withCurrAccPkts((int) entry.packets())
                    .withCurrAccBytes(entry.bytes())
                    .withErrorPkts((short) 0)
                    .withDropPkts((short) 0);

            fBuilder.withStatsInfo(sBuilder.build());

            FlowInfo flowInfo = mergeFlowInfo(fBuilder.build(), fBuilder, sBuilder);

            flowInfos.add(flowInfo);

            log.debug("FlowInfo: \n{}", flowInfo.toString());
        }

        return flowInfos;
    }

    /**
     * Gets a set of flow infos by referring to overlay destination VM port.
     *
     * @return flow infos
     */
    private Set<FlowInfo> getOverlayDstPortBasedFlowInfos() {
        Set<FlowInfo> flowInfos = Sets.newConcurrentHashSet();
        Set<PortNumber> instPortNums = instPortService.instancePorts()
                                                .stream()
                                                .map(InstancePort::portNumber)
                                                .collect(Collectors.toSet());
        Set<DeviceId> deviceIds = osNodeService.completeNodes(COMPUTE)
                                                .stream()
                                                .map(OpenstackNode::intgBridge)
                                                .collect(Collectors.toSet());

        deviceIds.forEach(d -> {
            List<PortStatistics> stats =
                                deviceService.getPortStatistics(d)
                                .stream()
                                .filter(s -> instPortNums.contains(s.portNumber()))
                                .collect(Collectors.toList());

            stats.forEach(s -> {
                InstancePort instPort = getInstancePort(d, s.portNumber());
                if (instPort != null) {
                    flowInfos.add(buildTxFlowInfoFromInstancePort(instPort, s));
                    flowInfos.add(buildRxFlowInfoFromInstancePort(instPort, s));
                }
            });
        });

        return flowInfos;
    }

    /**
     * Gets a set of flow infos by referring to underlay destination port.
     *
     * @return flow infos
     */
    private Set<FlowInfo> getUnderlayDstPortBasedFlowInfos() {
        Set<FlowInfo> flowInfos = Sets.newConcurrentHashSet();

        for (Device d : getUnderlayDevices()) {
            List<PortStatistics> stats =
                    new ArrayList<>(deviceService.getPortStatistics(d.id()));
            stats.forEach(s -> {
                Host host = hostService.getConnectedHosts(new ConnectPoint(d.id(), s.portNumber()))
                        .stream().findFirst().orElse(null);
                if (host != null) {
                    flowInfos.add(buildTxFlowInfoFromHost(host, s));
                    flowInfos.add(buildRxFlowInfoFromHost(host, s));
                }
            });
        }

        return flowInfos;
    }

    /**
     * Obtains a set of device instances which construct underlay network.
     *
     * @return a set of device instances
     */
    private Set<Device> getUnderlayDevices() {

        Set<Device> underlayDevices = Sets.newConcurrentHashSet();

        Set<DeviceId> overlayDeviceIds = osNodeService.completeNodes()
                .stream()
                .filter(n -> n.type() != CONTROLLER)
                .map(OpenstackNode::intgBridge)
                .collect(Collectors.toSet());

        for (Device d : deviceService.getAvailableDevices(SWITCH)) {
            if (overlayDeviceIds.contains(d.id())) {
                continue;
            }

            underlayDevices.add(d);
        }

        return underlayDevices;
    }

    /**
     * Checks whether the given drivers contains OVS driver.
     *
     * @param drivers a set of drivers
     * @return true if the given drivers contain any OVS driver, false otherwise
     */
    private boolean hasOvsDriver(List<Driver> drivers) {

        for (Driver driver : drivers) {
            if (OVS_DRIVER_NAME.equals(driver.name())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtains the flow info generated by TX port from instance port.
     *
     * @param instPort instance port
     * @param stat port statistics
     * @return flow info
     */
    private FlowInfo buildTxFlowInfoFromInstancePort(InstancePort instPort,
                                                     PortStatistics stat) {
        return buildTxFlowInfo(instPort.ipAddress(), instPort.macAddress(),
                                                     instPort.deviceId(), stat);
    }

    /**
     * Obtains the flow info generated from RX port from instance port.
     *
     * @param instPort instance port
     * @param stat port statistics
     * @return flow info
     */
    private FlowInfo buildRxFlowInfoFromInstancePort(InstancePort instPort,
                                                     PortStatistics stat) {
        return buildRxFlowInfo(instPort.ipAddress(), instPort.macAddress(),
                instPort.deviceId(), stat);
    }

    /**
     * Obtains the flow info generated by TX port from host.
     *
     * @param host host
     * @param stat port statistics
     * @return flow info
     */
    private FlowInfo buildTxFlowInfoFromHost(Host host, PortStatistics stat) {
        IpAddress ip = host.ipAddresses().stream().findFirst().orElse(null);

        if (ip != null) {
            return buildTxFlowInfo(ip, host.mac(), host.location().deviceId(), stat);
        }
        return null;
    }

    /**
     * Obtains the flow info generated by RX @param host host.
     *
     * @param host host
     * @param stat port statistics
     * @return flow info
     */
    private FlowInfo buildRxFlowInfoFromHost(Host host, PortStatistics stat) {
        IpAddress ip = host.ipAddresses().stream().findFirst().orElse(null);

        if (ip != null) {
            return buildRxFlowInfo(ip, host.mac(), host.location().deviceId(), stat);
        }
        return null;
    }

    /**
     * Obtains the flow info generated from TX port.
     *
     * @param ipAddress         IP address
     * @param macAddress        MAC address
     * @param deviceId          device identifier
     * @param stat              port statistics
     * @return flow info
     */
    private FlowInfo buildTxFlowInfo(IpAddress ipAddress,
                                     MacAddress macAddress,
                                     DeviceId deviceId,
                                     PortStatistics stat) {
        FlowInfo.Builder fBuilder = new DefaultFlowInfo.DefaultBuilder();

        fBuilder.withFlowType(FLOW_TYPE_SONA)
                .withSrcIp(IpPrefix.valueOf(ipAddress, ARBITRARY_LENGTH))
                .withDstIp(IpPrefix.valueOf(ARBITRARY_IP))
                .withSrcMac(macAddress)
                .withDstMac(NO_HOST_MAC)
                .withDeviceId(deviceId)
                .withInputInterfaceId(getInterfaceId(ipAddress))
                .withOutputInterfaceId(ARBITRARY_OUT_INTF)
                .withVlanId(VlanId.vlanId());

        StatsInfo.Builder sBuilder = new DefaultStatsInfo.DefaultBuilder();
        sBuilder.withStartupTime(System.currentTimeMillis())
                .withFstPktArrTime(System.currentTimeMillis())
                .withLstPktOffset((int) (REFRESH_INTERVAL * MILLISECONDS))
                .withCurrAccPkts((int) stat.packetsSent())
                .withCurrAccBytes(stat.bytesSent())
                .withErrorPkts((short) stat.packetsTxErrors())
                .withDropPkts((short) stat.packetsTxDropped());

        fBuilder.withStatsInfo(sBuilder.build());

        return mergeFlowInfo(fBuilder.build(), fBuilder, sBuilder);
    }

    /**
     * Obtains the flow info generated from RX port.
     *
     * @param ipAddress         IP address
     * @param macAddress        MAC address
     * @param deviceId          Device identifier
     * @param stat port statistics
     * @return flow info
     */
    private FlowInfo buildRxFlowInfo(IpAddress ipAddress,
                                     MacAddress macAddress,
                                     DeviceId deviceId,
                                     PortStatistics stat) {
        FlowInfo.Builder fBuilder = new DefaultFlowInfo.DefaultBuilder();

        fBuilder.withFlowType(FLOW_TYPE_SONA)
                .withSrcIp(IpPrefix.valueOf(ARBITRARY_IP))
                .withDstIp(IpPrefix.valueOf(ipAddress, ARBITRARY_LENGTH))
                .withSrcMac(NO_HOST_MAC)
                .withDstMac(macAddress)
                .withDeviceId(deviceId)
                .withInputInterfaceId(ARBITRARY_IN_INTF)
                .withOutputInterfaceId(ARBITRARY_OUT_INTF)
                .withVlanId(VlanId.vlanId());

        StatsInfo.Builder sBuilder = new DefaultStatsInfo.DefaultBuilder();
        sBuilder.withStartupTime(System.currentTimeMillis())
                .withFstPktArrTime(System.currentTimeMillis())
                .withLstPktOffset((int) (REFRESH_INTERVAL * MILLISECONDS))
                .withCurrAccPkts((int) stat.packetsReceived())
                .withCurrAccBytes(stat.bytesReceived())
                .withErrorPkts((short) stat.packetsRxErrors())
                .withDropPkts((short) stat.packetsRxDropped());

        fBuilder.withStatsInfo(sBuilder.build());

        return mergeFlowInfo(fBuilder.build(), fBuilder, sBuilder);
    }

    /**
     * Obtains instance port which associated with the given device identifier
     * and port number.
     *
     * @param deviceId      device identifier
     * @param portNumber    port number
     * @return instance port
     */
    private InstancePort getInstancePort(DeviceId deviceId, PortNumber portNumber) {
        return instPortService.instancePorts().stream()
                                .filter(p -> p.deviceId().equals(deviceId))
                                .filter(p -> p.portNumber().equals(portNumber))
                                .findFirst().orElse(null);
    }

    /**
     * Installs a flow rule where the source table is fromTable, while destination
     * table is toTable.
     *
     * @param deviceId          device identifier
     * @param fromTable         source table
     * @param toTable           destination table
     * @param statsFlowRule     stats flow rule
     * @param rulePriority      rule priority
     * @param install           installation flag
     */
    private void connectTables(DeviceId deviceId, int fromTable, int toTable,
                               StatsFlowRule statsFlowRule, int rulePriority,
                               boolean install) {

        int srcPrefixLength = statsFlowRule.srcIpPrefix().prefixLength();
        int dstPrefixLength = statsFlowRule.dstIpPrefix().prefixLength();
        int prefixLength = rulePriority + srcPrefixLength + dstPrefixLength;
        byte protocol = statsFlowRule.ipProtocol();

        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder()
                        .matchEthType(TYPE_IPV4)
                        .matchIPSrc(statsFlowRule.srcIpPrefix())
                        .matchIPDst(statsFlowRule.dstIpPrefix());

        if (protocol == PROTOCOL_TCP) {
            selectorBuilder = selectorBuilder
                    .matchIPProtocol(statsFlowRule.ipProtocol())
                    .matchTcpSrc(statsFlowRule.srcTpPort())
                    .matchTcpDst(statsFlowRule.dstTpPort());

        } else if (protocol == PROTOCOL_UDP) {
            selectorBuilder = selectorBuilder
                    .matchIPProtocol(statsFlowRule.ipProtocol())
                    .matchUdpSrc(statsFlowRule.srcTpPort())
                    .matchUdpDst(statsFlowRule.dstTpPort());
        } else if (protocol == ARBITRARY_PROTOCOL) {
            log.debug("IP protocol type is not specified.");
        } else {
            log.warn("Unsupported protocol {}", statsFlowRule.ipProtocol());
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        treatmentBuilder.transition(toTable);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(prefixLength)
                .fromApp(telemetryAppId)
                .makePermanent()
                .forTable(fromTable)
                .build();

        applyRule(flowRule, install);
    }

    /**
     * Installs stats related flow rule to switch.
     *
     * @param flowRule flow rule
     * @param install flag to install or not
     */
    private void applyRule(FlowRule flowRule, boolean install) {
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();
        flowOpsBuilder = install ?
                flowOpsBuilder.add(flowRule) : flowOpsBuilder.remove(flowRule);

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Install rules for telemetry stats: \n {}",
                                                                ops.toString());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.debug("Failed to install rules for telemetry stats: \n {}",
                                                                ops.toString());
            }
        }));
    }

    /**
     * Merges old FlowInfo.StatsInfo and current FlowInfo.StatsInfo.
     *
     * @param flowInfo current FlowInfo object
     * @param fBuilder Builder for FlowInfo
     * @param sBuilder Builder for StatsInfo
     * @return Merged FlowInfo object
     */
    private FlowInfo mergeFlowInfo(FlowInfo flowInfo,
                                   FlowInfo.Builder fBuilder,
                                   StatsInfo.Builder sBuilder) {
        for (FlowInfo gFlowInfo : gFlowInfoSet) {
            log.debug("Old FlowInfo:\n{}", gFlowInfo.toString());
            if (gFlowInfo.roughEquals(flowInfo)) {

                // Get old StatsInfo object and merge the value to current object.
                StatsInfo oldStatsInfo = gFlowInfo.statsInfo();
                sBuilder.withPrevAccPkts(oldStatsInfo.currAccPkts());
                sBuilder.withPrevAccBytes(oldStatsInfo.currAccBytes());
                FlowInfo newFlowInfo = fBuilder.withStatsInfo(sBuilder.build())
                        .build();

                gFlowInfoSet.remove(gFlowInfo);
                gFlowInfoSet.add(newFlowInfo);
                log.debug("Old FlowInfo found, Merge this {}", newFlowInfo.toString());
                return newFlowInfo;
            }
        }

        // No such record, then build the FlowInfo object and return this object.
        log.debug("No FlowInfo found, add new FlowInfo {}", flowInfo.toString());
        FlowInfo newFlowInfo = fBuilder.withStatsInfo(sBuilder.build()).build();
        gFlowInfoSet.add(newFlowInfo);
        return newFlowInfo;
    }

    /**
     * Installs flow rules for collecting both normal and reverse path flow stats.
     *
     * @param statsFlowRule flow rule used for collecting stats
     * @param install flow rule installation flag
     */
    private void setStatFlowRule(StatsFlowRule statsFlowRule, boolean install) {
        setStatFlowRuleBase(statsFlowRule, install);

        // if reverse path stats is enabled, we will install flow rules for
        // collecting reverse path vFlow stats
        if (reversePathStats) {
            StatsFlowRule reverseFlowRule = DefaultStatsFlowRule.builder()
                                            .srcIpPrefix(statsFlowRule.dstIpPrefix())
                                            .dstIpPrefix(statsFlowRule.srcIpPrefix())
                                            .ipProtocol(statsFlowRule.ipProtocol())
                                            .srcTpPort(statsFlowRule.dstTpPort())
                                            .dstTpPort(statsFlowRule.srcTpPort())
                                            .build();
            setStatFlowRuleBase(reverseFlowRule, install);
        }
    }

    /**
     * A base method which is for installing flow rules for collecting stats.
     *
     * @param statsFlowRule flow rule used for collecting stats
     * @param install flow rule installation flag
     */
    private void setStatFlowRuleBase(StatsFlowRule statsFlowRule, boolean install) {

        IpPrefix srcIp = statsFlowRule.srcIpPrefix();
        IpPrefix dstIp = statsFlowRule.dstIpPrefix();
        DeviceId srcDeviceId = getDeviceId(srcIp.address());
        DeviceId dstDeviceId = getDeviceId(dstIp.address());

        if (srcDeviceId == null && dstDeviceId == null) {
            return;
        }

        if (srcDeviceId != null) {
            connectTables(srcDeviceId, STAT_INBOUND_TABLE, VTAP_INBOUND_TABLE,
                    statsFlowRule, METRIC_PRIORITY_SOURCE, install);

            if (install) {
                log.info("Install ingress stat flow rule for SrcIp:{} DstIp:{}",
                                            srcIp.toString(), dstIp.toString());
            } else {
                log.info("Remove ingress stat flow rule for SrcIp:{} DstIp:{}",
                                            srcIp.toString(), dstIp.toString());
            }
        }

        Set<IpPrefix> vxlanIps = osNetworkService.getFixedIpsByNetworkType(VXLAN);
        Set<IpPrefix> vlanIps = osNetworkService.getFixedIpsByNetworkType(VLAN);
        Set<IpPrefix> flatIps = osNetworkService.getFixedIpsByNetworkType(FLAT);

        int fromTable, toTable;

        if (dstDeviceId != null && egressStats) {

            IpPrefix dstIpPrefix = statsFlowRule.dstIpPrefix();

            if (vxlanIps.contains(dstIpPrefix) || vlanIps.contains(dstIpPrefix)) {
                fromTable = STAT_OUTBOUND_TABLE;
                toTable = VTAP_OUTBOUND_TABLE;
            } else if (flatIps.contains(dstIpPrefix)) {
                fromTable = STAT_FLAT_OUTBOUND_TABLE;
                toTable = VTAP_FLAT_OUTBOUND_TABLE;
            } else {
                return;
            }

            connectTables(dstDeviceId, fromTable, toTable,
                    statsFlowRule, METRIC_PRIORITY_TARGET, install);

            if (install) {
                log.info("Install egress stat flow rule for SrcIp:{} DstIp:{}",
                                            srcIp.toString(), dstIp.toString());
            } else {
                log.info("Remove egress stat flow rule for SrcIp:{} DstIp:{}",
                                            srcIp.toString(), dstIp.toString());
            }
        }
    }

    /**
     * Gets Device ID which the VM is located.
     *
     * @param ipAddress IP Address of host
     * @return Device ID
     */
    private DeviceId getDeviceId(IpAddress ipAddress) {
        if (!hostService.getHostsByIp(ipAddress).isEmpty()) {
            Optional<Host> host = hostService.getHostsByIp(ipAddress).stream().findAny();
            return host.map(host1 -> host1.location().deviceId()).orElse(null);
        } else {
            log.debug("No DeviceID is associated to {}", ipAddress.toString());
            return null;
        }
    }

    /**
     * Gets VLAN ID with respect to IP Address.
     *
     * @param ipAddress IP Address of host
     * @return VLAN ID
     */
    private VlanId getVlanId(IpAddress ipAddress) {
        if (!hostService.getHostsByIp(ipAddress).isEmpty()) {
            Host host = hostService.getHostsByIp(ipAddress).stream().findAny().get();
            return host.vlan();
        }
        return VlanId.vlanId();
    }

    /**
     * Gets Interface ID of Switch which is connected to a host.
     *
     * @param ipAddress IP Address of host
     * @return Interface ID of Switch
     */
    private int getInterfaceId(IpAddress ipAddress) {
        if (!hostService.getHostsByIp(ipAddress).isEmpty()) {
            Host host = hostService.getHostsByIp(ipAddress).stream().findAny().get();
            return (int) host.location().port().toLong();
        }
        return -1;
    }

    /**
     * Gets MAC Address of host.
     *
     * @param ipAddress IP Address of host
     * @return MAC Address of host
     */
    private MacAddress getMacAddress(IpAddress ipAddress) {
        if (!hostService.getHostsByIp(ipAddress).isEmpty()) {
            Host host = hostService.getHostsByIp(ipAddress).stream().findAny().get();
            return host.mac();
        }

        return NO_HOST_MAC;
    }

    /**
     * Gets IP address of the host which is attached to the given device and port.
     *
     * @param device    device
     * @param inPort    IN port number
     * @return IP address
     */
    private IpAddress getIpAddress(Device device, PortCriterion inPort) {

        Host host = hostService.getConnectedHosts(device.id()).stream()
                .filter(h -> h.location().port().equals(inPort.port()))
                .findAny().orElse(null);

        if (host != null) {
            return host.ipAddresses().stream().findAny().get();
        }

        return NO_HOST_IP;
    }

    private void enqFlowInfo(FlowInfo flowInfo) {
        String key = flowInfo.uniqueFlowInfoKey();
        Queue<FlowInfo> queue = flowInfoMap.get(key);
        if (queue == null) {
            Queue<FlowInfo> newQueue = new LinkedList<FlowInfo>();
            newQueue.offer(flowInfo);
            flowInfoMap.put(key, newQueue);
            return;
        }
        queue.offer(flowInfo);

        while (queue.size() > DEFAULT_DATA_POINT_SIZE) {
            queue.remove(); // Removes a garbage data in the queue.
        }
    }

    /**
     * Checks whether the given device is edge switch or not.
     *
     * @param id device identifier
     * @return true if the given device is edge switch, false otherwise
     */
    private boolean isEdgeSwitch(DeviceId id) {

        return !hostService.getConnectedHosts(id).isEmpty();
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        Boolean reversePathStatsConfigured =
                            getBooleanProperty(properties, PROP_REVERSE_PATH_STATS);
        if (reversePathStatsConfigured == null) {
            reversePathStats = PROP_REVERSE_PATH_STATS_DEFAULT;
            log.info("Reversed path stats flag is NOT " +
                     "configured, default value is {}", reversePathStats);
        } else {
            reversePathStats = reversePathStatsConfigured;
            log.info("Configured. Reversed path stats flag is {}", reversePathStats);
        }

        Boolean egressStatsConfigured = getBooleanProperty(properties, PROP_EGRESS_STATS);
        if (egressStatsConfigured == null) {
            egressStats = PROP_EGRESS_STATS_DEFAULT;
            log.info("Egress stats flag is NOT " +
                     "configured, default value is {}", egressStats);
        } else {
            egressStats = egressStatsConfigured;
            log.info("Configured. Egress stats flag is {}", egressStats);
        }

        Boolean portStatsConfigured = getBooleanProperty(properties, PROP_PORT_STATS);
        if (portStatsConfigured == null) {
            portStats = PROP_PORT_STATS_DEFAULT;
            log.info("Port stats flag is NOT " +
                    "configured, default value is {}", portStats);
        } else {
            portStats = portStatsConfigured;
            log.info("Configured. Port stats flag is {}", portStats);
        }

        Boolean monitorOverlayConfigured = getBooleanProperty(properties, PROP_MONITOR_OVERLAY);
        if (monitorOverlayConfigured == null) {
            monitorOverlay = PROP_MONITOR_OVERLAY_DEFAULT;
            log.info("Monitor overlay flag is NOT " +
                    "configured, default value is {}", monitorOverlay);
        } else {
            monitorOverlay = monitorOverlayConfigured;
            log.info("Configured. Monitor overlay flag is {}", monitorOverlay);
        }

        Boolean monitorUnderlayConfigured = getBooleanProperty(properties, PROP_MONITOR_UNDERLAY);
        if (monitorUnderlayConfigured == null) {
            monitorUnderlay = PROP_MONITOR_UNDERLAY_DEFAULT;
            log.info("Monitor underlay flag is NOT " +
                    "configured, default value is {}", monitorUnderlay);
        } else {
            monitorUnderlay = monitorUnderlayConfigured;
            log.info("Configured. Monitor underlay flag is {}", monitorUnderlay);
        }
    }

    private class TelemetryCollector implements Runnable {
        @Override
        public void run() {
            Set<FlowInfo> filteredOverlayFlowInfos = Sets.newConcurrentHashSet();
            Set<FlowInfo> filteredUnderlayFlowInfos = Sets.newConcurrentHashSet();

            // we only let the master controller of the device where the
            // stats flow rules are installed send stats message
            if (monitorOverlay) {
                getOverlayFlowInfos().forEach(f -> {
                    if (checkSrcDstLocalMaster(f)) {
                        filteredOverlayFlowInfos.add(f);
                    }
                });
            }
            if (monitorUnderlay) {
                getUnderlayFlowInfos().forEach(f -> {
                    if (checkSrcDstLocalMaster(f)) {
                        filteredUnderlayFlowInfos.add(f);
                    }
                });
            }

            // we only let the master controller of the device where the port
            // is located to send stats message
            if (portStats) {
                if (monitorOverlay) {
                    getOverlayDstPortBasedFlowInfos().forEach(f -> {
                        if (checkSrcDstLocalMaster(f)) {
                            filteredOverlayFlowInfos.add(f);
                        }
                    });
                }

                if (monitorUnderlay) {
                    getUnderlayDstPortBasedFlowInfos().forEach(f -> {
                        if (checkSrcDstLocalMaster(f)) {
                            filteredUnderlayFlowInfos.add(f);
                        }
                    });
                }
            }


            if (monitorOverlay) {
                telemetryService.publish(filteredOverlayFlowInfos);

                // TODO: Refactor the following code to "TelemetryService" style.
                filteredOverlayFlowInfos.forEach(StatsFlowRuleManager.this::enqFlowInfo);
            }

            if (monitorUnderlay) {
                telemetryService.publish(filteredUnderlayFlowInfos);
            }
        }

        private boolean checkSrcDstLocalMaster(FlowInfo info) {
            DeviceId srcDeviceId = getDeviceId(info.srcIp().address());
            DeviceId dstDeviceId = getDeviceId(info.dstIp().address());

            boolean isSrcLocalMaster = srcDeviceId != null &&
                    mastershipService.isLocalMaster(srcDeviceId);
            boolean isDstLocalMaster = dstDeviceId != null &&
                    mastershipService.isLocalMaster(dstDeviceId);

            return isSrcLocalMaster || isDstLocalMaster;
        }
    }
}
