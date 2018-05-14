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
package org.onosproject.pipelines.basic;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.ImmutableByteSequence;
import org.onlab.util.SharedExecutors;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.inbandtelemetry.api.IntConfig;
import org.onosproject.inbandtelemetry.api.IntIntent;
import org.onosproject.inbandtelemetry.api.IntObjective;
import org.onosproject.inbandtelemetry.api.IntProgrammable;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.host.HostService;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class IntProgrammableImpl extends AbstractHandlerBehaviour implements IntProgrammable {

    // TODO: change this value to the value of diameter of a network.
    private static final int MAXHOP = 64;
    private static final int PORTMASK = 0xffff;
    private static final int IDLE_TIMEOUT = 100;
    private static final int PKT_INSTANCE_TYPE_INGRESS_CLONE = 1;
    // Application name of the pipeline which adds this implementation to the pipeconf
    private static final String PIPELINE_APP_NAME = "org.onosproject.pipelines.basic";
    private final Logger log = getLogger(getClass());
    private ApplicationId appId;

    private static final Set<Criterion.Type> SUPPORTED_CRITERION = Sets.newHashSet(
            Criterion.Type.IPV4_DST, Criterion.Type.IPV4_SRC,
            Criterion.Type.UDP_SRC, Criterion.Type.UDP_DST,
            Criterion.Type.TCP_SRC, Criterion.Type.TCP_DST,
            Criterion.Type.IP_PROTO);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    private DeviceId deviceId;
    private static final int DEFAULT_PRIORITY = 10000;
    private static final ImmutableBiMap<Integer, PiActionId> INST_0003_ACTION_MAP =
            ImmutableBiMap.<Integer, PiActionId>builder()
                    .put(0, IntConstants.ACT_INT_SET_HEADER_0003_I0_ID)
                    .put(1, IntConstants.ACT_INT_SET_HEADER_0003_I1_ID)
                    .put(2, IntConstants.ACT_INT_SET_HEADER_0003_I2_ID)
                    .put(3, IntConstants.ACT_INT_SET_HEADER_0003_I3_ID)
                    .put(4, IntConstants.ACT_INT_SET_HEADER_0003_I4_ID)
                    .put(5, IntConstants.ACT_INT_SET_HEADER_0003_I5_ID)
                    .put(6, IntConstants.ACT_INT_SET_HEADER_0003_I6_ID)
                    .put(7, IntConstants.ACT_INT_SET_HEADER_0003_I7_ID)
                    .put(8, IntConstants.ACT_INT_SET_HEADER_0003_I8_ID)
                    .put(9, IntConstants.ACT_INT_SET_HEADER_0003_I9_ID)
                    .put(10, IntConstants.ACT_INT_SET_HEADER_0003_I10_ID)
                    .put(11, IntConstants.ACT_INT_SET_HEADER_0003_I11_ID)
                    .put(12, IntConstants.ACT_INT_SET_HEADER_0003_I12_ID)
                    .put(13, IntConstants.ACT_INT_SET_HEADER_0003_I13_ID)
                    .put(14, IntConstants.ACT_INT_SET_HEADER_0003_I14_ID)
                    .put(15, IntConstants.ACT_INT_SET_HEADER_0003_I15_ID)
                    .build();

    private static final ImmutableBiMap<Integer, PiActionId> INST_0407_ACTION_MAP =
            ImmutableBiMap.<Integer, PiActionId>builder()
                    .put(0, IntConstants.ACT_INT_SET_HEADER_0407_I0_ID)
                    .put(1, IntConstants.ACT_INT_SET_HEADER_0407_I1_ID)
                    .put(2, IntConstants.ACT_INT_SET_HEADER_0407_I2_ID)
                    .put(3, IntConstants.ACT_INT_SET_HEADER_0407_I3_ID)
                    .put(4, IntConstants.ACT_INT_SET_HEADER_0407_I4_ID)
                    .put(5, IntConstants.ACT_INT_SET_HEADER_0407_I5_ID)
                    .put(6, IntConstants.ACT_INT_SET_HEADER_0407_I6_ID)
                    .put(7, IntConstants.ACT_INT_SET_HEADER_0407_I7_ID)
                    .put(8, IntConstants.ACT_INT_SET_HEADER_0407_I8_ID)
                    .put(9, IntConstants.ACT_INT_SET_HEADER_0407_I9_ID)
                    .put(10, IntConstants.ACT_INT_SET_HEADER_0407_I10_ID)
                    .put(11, IntConstants.ACT_INT_SET_HEADER_0407_I11_ID)
                    .put(12, IntConstants.ACT_INT_SET_HEADER_0407_I12_ID)
                    .put(13, IntConstants.ACT_INT_SET_HEADER_0407_I13_ID)
                    .put(14, IntConstants.ACT_INT_SET_HEADER_0407_I14_ID)
                    .put(15, IntConstants.ACT_INT_SET_HEADER_0407_I15_ID)
                    .build();

    @Override
    public void init() {
        deviceId = this.data().deviceId();
        flowRuleService = handler().get(FlowRuleService.class);
        deviceService = handler().get(DeviceService.class);
        hostService = handler().get(HostService.class);
        coreService = handler().get(CoreService.class);
        appId = coreService.getAppId(PIPELINE_APP_NAME);
        if (appId == null) {
            log.warn("Application ID is null. Cannot initialize INT-pipeline.");
            return;
        }

        Set<PortNumber> hostPorts = deviceService.getPorts(deviceId).stream().filter(port ->
             hostService.getConnectedHosts(new ConnectPoint(deviceId, port.number())).size() > 0
        ).map(Port::number).collect(Collectors.toSet());
        List<FlowRule> flowRules = new ArrayList<>();

        // process_int_transit.tb_int_insert
        PiActionParam transitIdParam = new PiActionParam(
                IntConstants.ACT_PRM_SWITCH_ID,
                ImmutableByteSequence.copyFrom(
                        Integer.parseInt(deviceId.toString().substring(
                                deviceId.toString().length() - 2))));
        PiAction transitAction = PiAction.builder()
                .withId(IntConstants.ACT_INT_TRANSIT_ID)
                .withParameter(transitIdParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(transitAction)
                .build();

        FlowRule transitFlowRule = DefaultFlowRule.builder()
                .withTreatment(treatment)
                .fromApp(appId)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .forDevice(deviceId)
                .forTable(IntConstants.TBL_INT_INSERT_ID)
                .build();
        flowRules.add(transitFlowRule);

        for (PortNumber portNumber: hostPorts) {
            // process_set_source_sink.tb_set_source for each host-facing port
            PiCriterion ingressCriterion = PiCriterion.builder()
                    .matchExact(BasicConstants.HDR_IN_PORT_ID, portNumber.toLong())
                    .build();
            TrafficSelector srcSelector = DefaultTrafficSelector.builder()
                    .matchPi(ingressCriterion)
                    .build();
            PiAction setSourceAct = PiAction.builder()
                    .withId(IntConstants.ACT_INT_SET_SOURCE_ID)
                    .build();
            TrafficTreatment srcTreatment = DefaultTrafficTreatment.builder()
                    .piTableAction(setSourceAct)
                    .build();
            FlowRule srcFlowRule = DefaultFlowRule.builder()
                    .withSelector(srcSelector)
                    .withTreatment(srcTreatment)
                    .fromApp(appId)
                    .withPriority(DEFAULT_PRIORITY)
                    .makePermanent()
                    .forDevice(deviceId)
                    .forTable(IntConstants.TBL_SET_SOURCE_ID)
                    .build();
            flowRules.add(srcFlowRule);

            // process_set_source_sink.tb_set_sink
            PiCriterion egressCriterion = PiCriterion.builder()
                    .matchExact(IntConstants.HDR_OUT_PORT_ID, portNumber.toLong())
                    .build();
            TrafficSelector sinkSelector = DefaultTrafficSelector.builder()
                    .matchPi(egressCriterion)
                    .build();
            PiAction setSinkAct = PiAction.builder()
                    .withId(IntConstants.ACT_INT_SET_SINK_ID)
                    .build();
            TrafficTreatment sinkTreatment = DefaultTrafficTreatment.builder()
                    .piTableAction(setSinkAct)
                    .build();
            FlowRule sinkFlowRule = DefaultFlowRule.builder()
                    .withSelector(sinkSelector)
                    .withTreatment(sinkTreatment)
                    .fromApp(appId)
                    .withPriority(DEFAULT_PRIORITY)
                    .makePermanent()
                    .forDevice(deviceId)
                    .forTable(IntConstants.TBL_SET_SINK_ID)
                    .build();
            flowRules.add(sinkFlowRule);
        }
        flowRules.forEach(flowRule -> flowRuleService.applyFlowRules(flowRule));

        // Populate tb_int_inst_0003 table
        INST_0003_ACTION_MAP.forEach((matchValue, actionId) ->
                                             populateInstTableEntry(IntConstants.TBL_INT_INST_0003_ID,
                                                                    IntConstants.INT_HDR_INST_MASK_0003_ID,
                                                                    matchValue,
                                                                    actionId,
                                                                    appId));
        // Populate tb_int_inst_0407 table
        INST_0407_ACTION_MAP.forEach((matchValue, actionId) ->
                                             populateInstTableEntry(IntConstants.TBL_INT_INST_0407_ID,
                                                                    IntConstants.INT_HDR_INST_MASK_0407_ID,
                                                                    matchValue,
                                                                    actionId,
                                                                    appId));
    }

    @Override
    public CompletableFuture<Boolean> addIntObjective(IntObjective obj) {
        // TODO: support different types of watchlist other than flow watchlist

        return CompletableFuture.supplyAsync(
                () -> processIntObjective(obj, true),
                SharedExecutors.getPoolThreadExecutor()
        );
    }

    @Override
    public CompletableFuture<Boolean> removeIntObjective(IntObjective obj) {
        return CompletableFuture.supplyAsync(
                () -> processIntObjective(obj, false),
                SharedExecutors.getPoolThreadExecutor()
        );
    }

    @Override
    public CompletableFuture<Boolean> setupIntConfig(IntConfig config) {
        return CompletableFuture.supplyAsync(
                () -> setupIntReportInternal(config),
                SharedExecutors.getPoolThreadExecutor()
        );
    }

    private void populateInstTableEntry(PiTableId tableId, PiMatchFieldId matchFieldId,
                                        int matchValue, PiActionId actionId, ApplicationId appId) {
        PiCriterion instCriterion = PiCriterion.builder()
                .matchExact(matchFieldId, matchValue)
                .build();
        TrafficSelector instSelector = DefaultTrafficSelector.builder()
                .matchPi(instCriterion)
                .build();
        PiAction instAction = PiAction.builder()
                .withId(actionId)
                .build();
        TrafficTreatment instTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(instAction)
                .build();

        FlowRule instFlowRule = DefaultFlowRule.builder()
                .withSelector(instSelector)
                .withTreatment(instTreatment)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .forDevice(deviceId)
                .forTable(tableId)
                .fromApp(appId)
                .build();

        flowRuleService.applyFlowRules(instFlowRule);
    }

    private FlowRule buildWatchlistEntry(IntObjective obj) {
        coreService = handler().get(CoreService.class);
        appId = coreService.getAppId(PIPELINE_APP_NAME);
        if (appId == null) {
            log.warn("Application ID is null. Cannot initialize INT-pipeline.");
            return null;
        }
        int instructionBitmap = buildInstructionBitmap(obj.metadataTypes());
        PiActionParam maxHopParam = new PiActionParam(
                IntConstants.ACT_PRM_MAX_HOP_ID,
                ImmutableByteSequence.copyFrom(MAXHOP));
        PiActionParam instCntParam = new PiActionParam(
                IntConstants.ACT_PRM_INS_CNT_ID,
                ImmutableByteSequence.copyFrom(Integer.bitCount(instructionBitmap)));
        PiActionParam inst0003Param = new PiActionParam(
                IntConstants.ACT_PRM_INS_MASK0003_ID,
                ImmutableByteSequence.copyFrom((instructionBitmap >> 12) & 0xF));
        PiActionParam inst0407Param = new PiActionParam(
                IntConstants.ACT_PRM_INS_MASK0407_ID,
                ImmutableByteSequence.copyFrom((instructionBitmap >> 8) & 0xF));

        PiAction intSourceAction = PiAction.builder()
                .withId(IntConstants.ACT_INT_SOURCE_DSCP_ID)
                .withParameter(maxHopParam)
                .withParameter(instCntParam)
                .withParameter(inst0003Param)
                .withParameter(inst0407Param)
                .build();

        TrafficTreatment instTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(intSourceAction)
                .build();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        for (Criterion criterion : obj.selector().criteria()) {
            switch (criterion.type()) {
                case IPV4_SRC:
                    sBuilder.matchIPSrc(((IPCriterion) criterion).ip());
                    break;
                case IPV4_DST:
                    sBuilder.matchIPDst(((IPCriterion) criterion).ip());
                    break;
                case TCP_SRC:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    IntConstants.LOCAL_META_SRC_PORT_ID,
                                    ((TcpPortCriterion) criterion).tcpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case UDP_SRC:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    IntConstants.LOCAL_META_SRC_PORT_ID,
                                    ((UdpPortCriterion) criterion).udpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case TCP_DST:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    IntConstants.LOCAL_META_DST_PORT_ID,
                                    ((TcpPortCriterion) criterion).tcpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case UDP_DST:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    IntConstants.LOCAL_META_DST_PORT_ID,
                                    ((UdpPortCriterion) criterion).udpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                default:
                    log.warn("Unsupported criterion type: {}", criterion.type());
            }
        }

        return DefaultFlowRule.builder()
                .forDevice(this.data().deviceId())
                .withSelector(sBuilder.build())
                .withTreatment(instTreatment)
                .withPriority(DEFAULT_PRIORITY)
                .forTable(IntConstants.TBL_INT_SOURCE_ID)
                .fromApp(appId)
                .withIdleTimeout(IDLE_TIMEOUT)
                .build();
    }

    private int buildInstructionBitmap(Set<IntIntent.IntMetadataType> metadataTypes) {
        int instBitmap = 0;
        for (IntIntent.IntMetadataType metadataType : metadataTypes) {
            switch (metadataType) {
                case SWITCH_ID:
                    instBitmap |= (1 << 15);
                    break;
                case L1_PORT_ID:
                    instBitmap |= (1 << 14);
                    break;
                case HOP_LATENCY:
                    instBitmap |= (1 << 13);
                    break;
                case QUEUE_OCCUPANCY:
                    instBitmap |= (1 << 12);
                    break;
                case INGRESS_TIMESTAMP:
                    instBitmap |= (1 << 11);
                    break;
                case EGRESS_TIMESTAMP:
                    instBitmap |= (1 << 10);
                    break;
                case L2_PORT_ID:
                    instBitmap |= (1 << 9);
                    break;
                case EGRESS_TX_UTIL:
                    instBitmap |= (1 << 8);
                    break;
                default:
                    log.info("Unsupported metadata type {}. Ignoring...", metadataType);
                    break;
            }
        }
        return instBitmap;
    }

    /**
     * Returns a subset of Criterion from given selector,
     * which is unsupported by this INT pipeline.
     *
     * @param selector a traffic selector
     * @return a subset of Criterion from given selector, unsupported by this INT pipeline,
     *  empty if all criteria are supported.
     */
    private Set<Criterion> unsupportedSelectors(TrafficSelector selector) {
        return selector.criteria().stream()
                .filter(criterion -> !SUPPORTED_CRITERION.contains(criterion.type()))
                .collect(Collectors.toSet());
    }

    private boolean processIntObjective(IntObjective obj, boolean install) {
        flowRuleService = handler().get(FlowRuleService.class);
        deviceId = this.data().deviceId();
        if (install && !unsupportedSelectors(obj.selector()).isEmpty()) {
            log.warn("Device {} does not support criteria {} for INT.",
                     deviceId, unsupportedSelectors(obj.selector()));
            return false;
        }

        FlowRule flowRule = buildWatchlistEntry(obj);
        if (flowRule != null) {
            if (install) {
                flowRuleService.applyFlowRules(flowRule);
            } else {
                flowRuleService.removeFlowRules(flowRule);
            }
            log.debug("IntObjective {} has been {} {}",
                      obj, install ? "installed to" : "removed from", deviceId);
            return true;
        } else {
            log.warn("Failed to {} IntObjective {} on {}",
                     install ? "install" : "remove", obj, deviceId);
            return false;
        }
    }

    private boolean setupIntReportInternal(IntConfig cfg) {
        flowRuleService = handler().get(FlowRuleService.class);

        FlowRule reportRule = buildReportEntry(cfg, PKT_INSTANCE_TYPE_INGRESS_CLONE);
        if (reportRule != null) {
            flowRuleService.applyFlowRules(reportRule);
            log.info("Report entry {} has been added to {}", reportRule, this.data().deviceId());
            return true;
        } else {
            log.warn("Failed to add report entry on {}", this.data().deviceId());
            return false;
        }
    }

    private FlowRule buildReportEntry(IntConfig cfg, int type) {
        coreService = handler().get(CoreService.class);
        appId = coreService.getAppId(PIPELINE_APP_NAME);
        if (appId == null) {
            log.warn("Application ID is null. Cannot build report entry.");
            return null;
        }

        PiCriterion instTypeCriterion = PiCriterion.builder()
                .matchExact(IntConstants.STD_META_INSTANCE_TYPE_ID, type)
                .build();
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(instTypeCriterion)
                .build();
        PiActionParam srcMacParam = new PiActionParam(
                IntConstants.ACT_PRM_SRC_MAC_ID,
                ImmutableByteSequence.copyFrom(cfg.sinkMac().toBytes()));
        PiActionParam nextHopMacParam = new PiActionParam(
                IntConstants.ACT_PRM_MON_MAC_ID,
                ImmutableByteSequence.copyFrom(cfg.collectorNextHopMac().toBytes()));
        PiActionParam srcIpParam = new PiActionParam(
                IntConstants.ACT_PRM_SRC_IP_ID,
                ImmutableByteSequence.copyFrom(cfg.sinkIp().toOctets()));
        PiActionParam monIpParam = new PiActionParam(
                IntConstants.ACT_PRM_MON_IP_ID,
                ImmutableByteSequence.copyFrom(cfg.collectorIp().toOctets()));
        PiActionParam monPortParam = new PiActionParam(
                IntConstants.ACT_PRM_MON_PORT_ID,
                ImmutableByteSequence.copyFrom(cfg.collectorPort().toInt()));
        PiAction reportAction = PiAction.builder()
                .withId(IntConstants.ACT_DO_REPORT_ENCAP_ID)
                .withParameter(srcMacParam)
                .withParameter(nextHopMacParam)
                .withParameter(srcIpParam)
                .withParameter(monIpParam)
                .withParameter(monPortParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(reportAction)
                .build();

        return DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(appId)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .forDevice(this.data().deviceId())
                .forTable(IntConstants.TBL_GENERATE_REPORT_ID)
                .build();
    }

}
