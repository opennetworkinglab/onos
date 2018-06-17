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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.host.HostService;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsFlowRuleAdminService;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.onosproject.openstacktelemetry.api.Constants.OPENSTACK_TELEMETRY_APP_ID;


/**
 * Flow rule manager for network statistics of a VM.
 */
@Component(immediate = true)
@Service
public class StatsFlowRuleManager implements StatsFlowRuleAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final byte FLOW_TYPE_SONA = 1; // VLAN

    public static final int MILLISECONDS = 1000;
    private static final int REFRESH_INTERVAL = 5;

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private Timer timer;
    private TimerTask task;
    private OpenstackTelemetryManager osTelemetryManager;

    Set<FlowInfo> gFlowInfoSet = new HashSet<>();
    private int loopCount = 0;

    private static final int SOURCE_ID = 1;
    private static final int TARGET_ID = 2;
    private static final int PRIORITY_BASE = 10000;
    private static final int METRIC_PRIORITY_SOURCE  = SOURCE_ID * PRIORITY_BASE;
    private static final int METRIC_PRIORITY_TARGET  = TARGET_ID * PRIORITY_BASE;

    public static final int    FLOW_TABLE_VM_SOURCE  =  0; // STAT_INBOUND_TABLE
    public static final int    FLOW_TABLE_DHCP_ARP   =  1; // DHCP_ARP_TABLE
    public static final int    FLOW_TABLE_VM_TARGET  = 49; // STAT_OUTBOUND_TABLE
    public static final int    FLOW_TABLE_FORWARDING = 50; // FORWARDING_TABLE

    static final MacAddress NO_HOST_MAC = MacAddress.valueOf("00:00:00:00:00:00");

    public StatsFlowRuleManager() {
        log.info("Object is instantiated");
        this.timer = new Timer("openstack-telemetry-sender");
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_TELEMETRY_APP_ID);
        log.info("Application is activated");
        osTelemetryManager = new OpenstackTelemetryManager();
        this.start();
    }

    @Deactivate
    protected void deactivate() {
        log.info("Application is deactivated");
    }

    private class InternalTimerTask extends TimerTask {
        @Override
        public void run() {
            log.debug("Timger Task Thread Starts ({})", loopCount++);
            try {
                Set<FlowInfo> flowInfoSet = getFlowRule();
                for (FlowInfo flowInfo: flowInfoSet) {
                    log.info("Publish FlowInfo to NMS: {}", flowInfo.toString());
                    osTelemetryManager.publish(flowInfo);
                }
            } catch (Exception ex) {
                log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
            }
        }
    }

    @Override
    public void start() {
        log.info("Start publishing thread");
        Set<FlowInfo>  gFlowInfoSet = getFlowRule();
        task = new InternalTimerTask();
        timer.scheduleAtFixedRate(task, MILLISECONDS * REFRESH_INTERVAL,
                                  MILLISECONDS * REFRESH_INTERVAL);
    }

    @Override
    public void stop() {
        log.info("Stop data publishing thread");
        task.cancel();
        task = null;
    }

    public void connectTables(
                            DeviceId deviceId,
                            int fromTable,
                            int toTable,
                            StatsFlowRule statsFlowRule,
                            int rulePriority,
                            boolean installFlag) {
        try {
            log.debug("Table Transition: {} -> {}", fromTable, toTable);
            int srcPrefixLength = statsFlowRule.srcIpPrefix().prefixLength();
            int dstPrefixLength = statsFlowRule.dstIpPrefix().prefixLength();
            int prefixLength = rulePriority + srcPrefixLength + dstPrefixLength;

            TrafficSelector.Builder selector;
            if (statsFlowRule == null) {
                selector = DefaultTrafficSelector.builder();
            } else {
                selector = DefaultTrafficSelector.builder()
                                       .matchEthType(Ethernet.TYPE_IPV4)
                                       .matchIPSrc(statsFlowRule.srcIpPrefix())
                                       .matchIPDst(statsFlowRule.dstIpPrefix());
                if (statsFlowRule.ipProtocol() == IPv4.PROTOCOL_TCP) {
                    selector = selector.matchIPProtocol(statsFlowRule.ipProtocol())
                                       .matchTcpSrc(statsFlowRule.srcTpPort())
                                       .matchTcpDst(statsFlowRule.dstTpPort());
                } else if (statsFlowRule.ipProtocol() == IPv4.PROTOCOL_UDP) {
                    selector = selector.matchIPProtocol(statsFlowRule.ipProtocol())
                                       .matchUdpSrc(statsFlowRule.srcTpPort())
                                       .matchUdpDst(statsFlowRule.dstTpPort());
                }
            }

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            treatment.transition(toTable);
            FlowRule flowRule = DefaultFlowRule.builder()
                                               .forDevice(deviceId)
                                               .withSelector(selector.build())
                                               .withTreatment(treatment.build())
                                               .withPriority(prefixLength)
                                               .fromApp(appId)
                                               .makePermanent()
                                               .forTable(fromTable)
                                               .build();
            applyRule(flowRule, installFlag);
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * Apply FlowRule to switch.
     *
     * @param flowRule FlowRule
     * @param install Flag to install or not
     */
    private void applyRule(FlowRule flowRule, boolean install) {
        log.debug("Apply flow rule to bridge device");
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();
        flowOpsBuilder = install ? flowOpsBuilder.add(flowRule) : flowOpsBuilder.remove(flowRule);

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Provisioned vni or forwarding table: \n {}", ops.toString());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.debug("Failed to provision vni or forwarding table: \n {}", ops.toString());
            }
        }));
    }

    /**
     * Craete a flow rule.
     *
     * @param flowRule  flow rule for Openstack VMs
     */
    @Override
    public void createFlowRule(StatsFlowRule flowRule) {
        try {
            log.debug("Create Flow Rule. SrcIp:{} DstIp:{}",
                      flowRule.srcIpPrefix().toString(),
                      flowRule.dstIpPrefix().toString());

            // To make a inversed flow rule.
            DefaultStatsFlowRule.Builder inverseFlowRuleBuilder
                                        = DefaultStatsFlowRule
                                            .builder()
                                            .srcIpPrefix(flowRule.dstIpPrefix())
                                            .dstIpPrefix(flowRule.srcIpPrefix())
                                            .ipProtocol(flowRule.ipProtocol())
                                            .srcTpPort(flowRule.dstTpPort())
                                            .dstTpPort(flowRule.srcTpPort());
            StatsFlowRule inverseFlowRule = inverseFlowRuleBuilder.build();
            DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
            Iterable<Device> devices = deviceService.getDevices();
            for (Device d : devices) {
                log.debug("Device: {}", d.toString());
                if (d.type() == Device.Type.CONTROLLER) {
                    log.info("Don't create flow rule for 'DeviceType=CONTROLLER' ({})",
                             d.id().toString());
                    continue;
                }
                connectTables(d.id(), FLOW_TABLE_VM_SOURCE, FLOW_TABLE_DHCP_ARP,
                              flowRule, METRIC_PRIORITY_SOURCE, true);
                connectTables(d.id(), FLOW_TABLE_VM_TARGET, FLOW_TABLE_FORWARDING,
                              inverseFlowRule, METRIC_PRIORITY_TARGET, true);
            }
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * Get FlowRule.
     *
     * @param flowRule Flow rule for a VM
     * @return Set of FlowInfo
     */
    public Set<FlowInfo> getFlowRule(StatsFlowRule flowRule) {
        Set<FlowInfo> flowInfoSet = new HashSet<>();
        log.info("Get flow rule: {}", flowRule.toString());
        // TODO  Make a implementation here.
        return flowInfoSet;
    }

    /**
     * Delete FlowRule for StatsInfo.
     *
     * @param flowRule  Flow rule for Openstack VM
     */
    @Override
    public void deleteFlowRule(StatsFlowRule flowRule) {
        log.debug("Delete Flow Rule: {}", flowRule.toString());
        flowRuleService = DefaultServiceDirectory.getService(FlowRuleService.class);
        flowRuleService.removeFlowRulesById(appId);
        // TODO  Write a implementation code here

        try {
            log.debug("Delete Flow Rule. SrcIp:{} DstIp:{}",
                      flowRule.srcIpPrefix().toString(),
                      flowRule.dstIpPrefix().toString());

            // To make a inversed flow rule.
            DefaultStatsFlowRule.Builder inverseFlowRuleBuilder
                                        = DefaultStatsFlowRule
                                            .builder()
                                            .srcIpPrefix(flowRule.dstIpPrefix())
                                            .dstIpPrefix(flowRule.srcIpPrefix())
                                            .ipProtocol(flowRule.ipProtocol())
                                            .srcTpPort(flowRule.dstTpPort())
                                            .dstTpPort(flowRule.srcTpPort());
            StatsFlowRule inverseFlowRule = inverseFlowRuleBuilder.build();
            DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
            Iterable<Device> devices = deviceService.getDevices();
            for (Device d : devices) {
                log.debug("Device: {}", d.toString());
                if (d.type() == Device.Type.CONTROLLER) {
                    log.info("Don't care for 'DeviceType=CONTROLLER' ({})",
                             d.id().toString());
                    continue;
                }
                connectTables(d.id(), FLOW_TABLE_VM_SOURCE, FLOW_TABLE_DHCP_ARP,
                              flowRule, METRIC_PRIORITY_SOURCE, false);
                connectTables(d.id(), FLOW_TABLE_VM_TARGET, FLOW_TABLE_FORWARDING,
                              inverseFlowRule, METRIC_PRIORITY_TARGET, false);
            }
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * Get a list of the FlowRule Store.
     *
     * @return list of Flow Rule
     */
    public Set<FlowInfo> getFlowRule() {
        log.debug("Get Flow Information List");
        Set<FlowInfo> flowInfoSet = new HashSet<>();
        try {
            flowRuleService = DefaultServiceDirectory.getService(FlowRuleService.class);
            Iterable<FlowEntry> flowEntries = flowRuleService.getFlowEntriesById(appId);

            for (FlowEntry entry : flowEntries) {
                FlowInfo.Builder fBuilder = new DefaultFlowInfo.DefaultBuilder();
                IPCriterion srcIpCriterion =
                        (IPCriterion) entry.selector().getCriterion(Criterion.Type.IPV4_SRC);
                IPCriterion dstIpCriterion =
                        (IPCriterion) entry.selector().getCriterion(Criterion.Type.IPV4_DST);
                IPProtocolCriterion ipProtocolCriterion =
                        (IPProtocolCriterion) entry.selector().getCriterion(Criterion.Type.IP_PROTO);

                log.debug("[FlowInfo]  TableID:{}  SRC_IP:{}  DST_IP:{}  Pkt:{}  Byte:{}",
                         ((IndexTableId) entry.table()).id(),
                         srcIpCriterion.ip().toString(), dstIpCriterion.ip().toString(),
                         entry.packets(), entry.bytes());

                fBuilder.withFlowType(FLOW_TYPE_SONA).withSrcIp(srcIpCriterion.ip())
                        .withDstIp(dstIpCriterion.ip())
                        .withProtocol((byte) ipProtocolCriterion.protocol());

                if (ipProtocolCriterion.protocol() == IPv4.PROTOCOL_TCP) {
                    TcpPortCriterion tcpSrcCriterion =
                            (TcpPortCriterion) entry.selector().getCriterion(Criterion.Type.TCP_SRC);
                    TcpPortCriterion tcpDstCriterion =
                            (TcpPortCriterion) entry.selector().getCriterion(Criterion.Type.TCP_DST);
                    log.debug("TCP SRC Port: {}   Dst Port: {}",
                                tcpSrcCriterion.tcpPort().toInt(), tcpDstCriterion.tcpPort().toInt());
                    fBuilder.withSrcPort(tcpSrcCriterion.tcpPort());
                    fBuilder.withDstPort(tcpDstCriterion.tcpPort());
                } else if (ipProtocolCriterion.protocol() == IPv4.PROTOCOL_UDP) {
                    UdpPortCriterion udpSrcCriterion =
                            (UdpPortCriterion) entry.selector().getCriterion(Criterion.Type.UDP_SRC);
                    UdpPortCriterion udpDstCriterion =
                            (UdpPortCriterion) entry.selector().getCriterion(Criterion.Type.UDP_DST);
                    log.debug("UDP SRC Port: {}   Dst Port: {}",
                                udpSrcCriterion.udpPort().toInt(), udpDstCriterion.udpPort().toInt());
                    fBuilder.withSrcPort(udpSrcCriterion.udpPort());
                    fBuilder.withDstPort(udpDstCriterion.udpPort());
                } else {
                    log.debug("Other protocol: {}", ipProtocolCriterion.protocol());
                }

                fBuilder.withSrcMac(getMacAddress(srcIpCriterion.ip().address()))
                        .withDstMac(getMacAddress(dstIpCriterion.ip().address()))
                        .withInputInterfaceId(getInterfaceId(srcIpCriterion.ip().address()))
                        .withOutputInterfaceId(getInterfaceId(dstIpCriterion.ip().address()))
                        .withVlanId(getVlanId(srcIpCriterion.ip().address()))
                        .withDeviceId(entry.deviceId());

                StatsInfo.Builder sBuilder = new DefaultStatsInfo.DefaultBuilder();
                sBuilder.withStartupTime(0)
                        .withCurrAccPkts((int) entry.packets()).withCurrAccBytes(entry.bytes())
                        .withErrorPkts((short) 0).withDropPkts((short) 0)
                        .withLstPktOffset(REFRESH_INTERVAL * MILLISECONDS);

                fBuilder.withStatsInfo(sBuilder.build());

                FlowInfo flowInfo = mergeFlowInfo(fBuilder.build(), fBuilder, sBuilder);
                flowInfoSet.add(flowInfo);
                log.debug("FlowInfo: \n{}", flowInfo.toString());
            }
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
        return flowInfoSet;
    }

    /**
     * Merge old FlowInfo.StatsInfo and current FlowInfo.StatsInfo.
     *
     * @param flowInfo current FlowInfo object
     * @param fBuilder Builder for FlowInfo
     * @param sBuilder Builder for StatsInfo
     * @return Merged FlowInfo object
     */
    private FlowInfo mergeFlowInfo(FlowInfo flowInfo,
                                   FlowInfo.Builder fBuilder,
                                   StatsInfo.Builder sBuilder) {
        try {
            log.debug("Current FlowInfo:\n{}", flowInfo.toString());
            for (FlowInfo gFlowInfo: gFlowInfoSet) {
                log.debug("Old FlowInfo:\n{}", gFlowInfo.toString());
                if (gFlowInfo.deviceId().equals(flowInfo.deviceId()) &&
                        gFlowInfo.srcIp().equals(flowInfo.srcIp()) &&
                        gFlowInfo.dstIp().equals(flowInfo.dstIp()) &&
                        gFlowInfo.srcPort().equals(flowInfo.srcPort()) &&
                        gFlowInfo.dstPort().equals(flowInfo.dstPort()) &&
                        (gFlowInfo.protocol() == flowInfo.protocol())
                        ) {
                    // Get old StatsInfo object and merge the value to current object.
                    StatsInfo oldStatsInfo = gFlowInfo.statsInfo();
                    sBuilder.withPrevAccPkts(oldStatsInfo.currAccPkts());
                    sBuilder.withPrevAccBytes(oldStatsInfo.currAccBytes());
                    FlowInfo newFlowInfo = fBuilder.withStatsInfo(sBuilder.build()).build();
                    gFlowInfoSet.remove(gFlowInfo);
                    gFlowInfoSet.add(newFlowInfo);
                    log.info("Old FlowInfo found, Merge this {}", newFlowInfo.toString());
                    return newFlowInfo;
                }
            }
            // No such record, then build the FlowInfo object and return this object.
            log.info("No FlowInfo found, add new FlowInfo {}", flowInfo.toString());
            FlowInfo newFlowInfo = fBuilder.withStatsInfo(sBuilder.build()).build();
            gFlowInfoSet.add(newFlowInfo);
            return newFlowInfo;
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
        log.debug("Add this FlowInfo {}", flowInfo.toString());
        gFlowInfoSet.add(flowInfo);
        return flowInfo;
    }

    /**
     * Get VLAN ID with respect to IP Address.
     *
     * @param ipAddress IP Address of host
     * @return VLAN ID
     */
    public VlanId getVlanId(IpAddress ipAddress) {
        try {
            if (!hostService.getHostsByIp(ipAddress).isEmpty()) {
                Host host = hostService.getHostsByIp(ipAddress).stream().findAny().get();
                return host.vlan();
            }
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
        return VlanId.vlanId();
    }

    /**
     * Get Interface ID of Switch which is connected to a host.
     *
     * @param ipAddress IP Address of host
     * @return Interface ID of Switch
     */
    public int getInterfaceId(IpAddress ipAddress) {
        try {
            if (!hostService.getHostsByIp(ipAddress).isEmpty()) {
                Host host = hostService.getHostsByIp(ipAddress).stream().findAny().get();
                return (int) host.location().port().toLong();
            }
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
        return -1;
    }

    /**
     * Get MAC Address of host.
     *
     * @param ipAddress IP Address of host
     * @return MAC Address of host
     */
    public MacAddress getMacAddress(IpAddress ipAddress) {
        try {
            if (!hostService.getHostsByIp(ipAddress).isEmpty()) {
                Host host = hostService.getHostsByIp(ipAddress).stream().findAny().get();
                return host.mac();
            }
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
        return NO_HOST_MAC;
    }
}
