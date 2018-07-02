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

import com.google.common.collect.Sets;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
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
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.host.HostService;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsFlowRuleAdminService;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.onlab.packet.Ethernet.TYPE_IPV4;
import static org.onlab.packet.IPv4.PROTOCOL_TCP;
import static org.onlab.packet.IPv4.PROTOCOL_UDP;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.IP_PROTO;
import static org.onosproject.net.flow.criteria.Criterion.Type.TCP_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.TCP_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.UDP_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.UDP_SRC;
import static org.onosproject.openstacknetworking.api.Constants.STAT_INBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_INBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_OUTBOUND_TABLE;
import static org.onosproject.openstacktelemetry.api.Constants.OPENSTACK_TELEMETRY_APP_ID;

/**
 * Flow rule manager for network statistics of a VM.
 */
@Component(immediate = true)
@Service
public class StatsFlowRuleManager implements StatsFlowRuleAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final byte FLOW_TYPE_SONA = 1; // VLAN

    private static final long MILLISECONDS = 1000L;
    private static final long REFRESH_INTERVAL = 5L;

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackTelemetryService telemetryService;

    private Timer timer;
    private TimerTask task;

    private final Set<FlowInfo> gFlowInfoSet = Sets.newHashSet();
    private int loopCount = 0;

    private static final int SOURCE_ID = 1;
    private static final int TARGET_ID = 2;
    private static final int PRIORITY_BASE = 10000;
    private static final int METRIC_PRIORITY_SOURCE  = SOURCE_ID * PRIORITY_BASE;
    private static final int METRIC_PRIORITY_TARGET  = TARGET_ID * PRIORITY_BASE;

    private static final MacAddress NO_HOST_MAC = MacAddress.valueOf("00:00:00:00:00:00");

    public StatsFlowRuleManager() {
        this.timer = new Timer("openstack-telemetry-sender");
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_TELEMETRY_APP_ID);

        this.start();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void start() {
        log.info("Start publishing thread");
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

    @Override
    public void createStatFlowRule(StatsFlowRule statsFlowRule) {

        setStatFlowRule(statsFlowRule, true);

        log.info("Install stat flow rule for SrcIp:{} DstIp:{}",
                                        statsFlowRule.srcIpPrefix().toString(),
                                        statsFlowRule.dstIpPrefix().toString());
    }

    @Override
    public void deleteStatFlowRule(StatsFlowRule statsFlowRule) {
        // FIXME: following code might not be necessary
        flowRuleService.removeFlowRulesById(appId);

        setStatFlowRule(statsFlowRule, false);

        log.info("Remove stat flow rule for SrcIp:{} DstIp:{}",
                                        statsFlowRule.srcIpPrefix().toString(),
                                        statsFlowRule.dstIpPrefix().toString());
    }

    private void connectTables(DeviceId deviceId, int fromTable, int toTable,
                               StatsFlowRule statsFlowRule, int rulePriority,
                               boolean install) {

        log.debug("Table Transition: {} -> {}", fromTable, toTable);
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
                                        .fromApp(appId)
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
                log.debug("Provisioned vni or forwarding table: \n {}", ops.toString());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.debug("Failed to provision vni or forwarding table: \n {}", ops.toString());
            }
        }));
    }

    /**
     * Gets a set of the flow infos.
     *
     * @return a set of flow infos
     */
    public Set<FlowInfo> getFlowInfo() {
        Set<FlowInfo> flowInfos = Sets.newConcurrentHashSet();

        // obtain all flow rule entries installed by telemetry app
        for (FlowEntry entry : flowRuleService.getFlowEntriesById(appId)) {
            FlowInfo.Builder fBuilder = new DefaultFlowInfo.DefaultBuilder();
            TrafficSelector selector = entry.selector();

            IPCriterion srcIp = (IPCriterion) selector.getCriterion(IPV4_SRC);
            IPCriterion dstIp = (IPCriterion) selector.getCriterion(IPV4_DST);
            IPProtocolCriterion ipProtocol =
                                (IPProtocolCriterion) selector.getCriterion(IP_PROTO);

            log.debug("[FlowInfo]  TableID:{}  SRC_IP:{}  DST_IP:{}  Pkt:{}  Byte:{}",
                                                ((IndexTableId) entry.table()).id(),
                                                srcIp.ip().toString(),
                                                dstIp.ip().toString(),
                                                entry.packets(),
                                                entry.bytes());

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

                    log.debug("TCP SRC Port: {}, DST Port: {}",
                            tcpSrc.tcpPort().toInt(),
                            tcpDst.tcpPort().toInt());

                    fBuilder.withSrcPort(tcpSrc.tcpPort());
                    fBuilder.withDstPort(tcpDst.tcpPort());

                } else if (ipProtocol.protocol() == PROTOCOL_UDP) {

                    UdpPortCriterion udpSrc =
                            (UdpPortCriterion) selector.getCriterion(UDP_SRC);
                    UdpPortCriterion udpDst =
                            (UdpPortCriterion) selector.getCriterion(UDP_DST);

                    log.debug("UDP SRC Port: {}, DST Port: {}",
                            udpSrc.udpPort().toInt(),
                            udpDst.udpPort().toInt());

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

            // TODO: need to collect error and drop packets stats
            // TODO: need to make the refresh interval configurable
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

    private void setStatFlowRule(StatsFlowRule statsFlowRule, boolean install) {
        StatsFlowRule inverseFlowRule = DefaultStatsFlowRule.builder()
                                        .srcIpPrefix(statsFlowRule.dstIpPrefix())
                                        .dstIpPrefix(statsFlowRule.srcIpPrefix())
                                        .ipProtocol(statsFlowRule.ipProtocol())
                                        .srcTpPort(statsFlowRule.dstTpPort())
                                        .dstTpPort(statsFlowRule.srcTpPort())
                                        .build();

        DeviceId srcDeviceId = getDeviceId(statsFlowRule.srcIpPrefix().address());
        DeviceId dstDeviceId = getDeviceId(statsFlowRule.dstIpPrefix().address());

        if (srcDeviceId == null && dstDeviceId == null) {
            return;
        }

        if (srcDeviceId != null) {
            connectTables(srcDeviceId, STAT_INBOUND_TABLE, VTAP_INBOUND_TABLE,
                    statsFlowRule, METRIC_PRIORITY_SOURCE, install);
        }

        if (dstDeviceId != null) {
            connectTables(dstDeviceId, STAT_OUTBOUND_TABLE, VTAP_OUTBOUND_TABLE,
                    inverseFlowRule, METRIC_PRIORITY_TARGET, install);
        }
    }

    /**
     * Get Device ID which the VM is located.
     *
     * @param ipAddress IP Address of host
     * @return Device ID
     */
    private DeviceId getDeviceId(IpAddress ipAddress) {
        if (!hostService.getHostsByIp(ipAddress).isEmpty()) {
            Optional<Host> host = hostService.getHostsByIp(ipAddress).stream().findAny();
            return host.map(host1 -> host1.location().deviceId()).orElse(null);
        } else {
            log.warn("Failed to get DeviceID which is connected to {}. " +
                            "The destination is either a bare-metal or located out of DC",
                    ipAddress.toString());
            return null;
        }
    }

    /**
     * Get VLAN ID with respect to IP Address.
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
     * Get Interface ID of Switch which is connected to a host.
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
     * Get MAC Address of host.
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

    private class InternalTimerTask extends TimerTask {
        @Override
        public void run() {
            log.debug("Timer Task Thread Starts ({})", loopCount++);

            Set<FlowInfo> filteredFlowInfos = Sets.newConcurrentHashSet();

            // we only let the master controller of the device where the
            // stats flow rules are installed send kafka message
            getFlowInfo().forEach(f -> {
                DeviceId deviceId = getDeviceId(f.srcIp().address());
                if (mastershipService.isLocalMaster(deviceId)) {
                    filteredFlowInfos.add(f);
                }
            });

            try {
                telemetryService.publish(filteredFlowInfos);
            } catch (Exception ex) {
                log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
            }
        }
    }
}
