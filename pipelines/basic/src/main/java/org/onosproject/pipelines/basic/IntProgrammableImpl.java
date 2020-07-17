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

import com.google.common.collect.Sets;
import org.onosproject.net.behaviour.inbandtelemetry.IntMetadataType;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.behaviour.inbandtelemetry.IntDeviceConfig;
import org.onosproject.net.behaviour.inbandtelemetry.IntObjective;
import org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.slf4j.LoggerFactory.getLogger;

public class IntProgrammableImpl extends AbstractHandlerBehaviour implements IntProgrammable {

    // TODO: change this value to the value of diameter of a network.
    private static final int MAXHOP = 64;
    private static final int PORTMASK = 0xffff;
    private static final int IDLE_TIMEOUT = 100;
    // Application name of the pipeline which adds this implementation to the pipeconf
    private static final String PIPELINE_APP_NAME = "org.onosproject.pipelines.basic";
    private final Logger log = getLogger(getClass());
    private ApplicationId appId;

    private static final Set<Criterion.Type> SUPPORTED_CRITERION = Sets.newHashSet(
            Criterion.Type.IPV4_DST, Criterion.Type.IPV4_SRC,
            Criterion.Type.UDP_SRC, Criterion.Type.UDP_DST,
            Criterion.Type.TCP_SRC, Criterion.Type.TCP_DST,
            Criterion.Type.IP_PROTO);

    private static final Set<PiTableId> TABLES_TO_CLEANUP = Sets.newHashSet(
            IntConstants.INGRESS_PROCESS_INT_SOURCE_TB_INT_SOURCE,
            IntConstants.INGRESS_PROCESS_INT_SOURCE_SINK_TB_SET_SOURCE,
            IntConstants.INGRESS_PROCESS_INT_SOURCE_SINK_TB_SET_SINK,
            IntConstants.EGRESS_PROCESS_INT_TRANSIT_TB_INT_INSERT,
            IntConstants.EGRESS_PROCESS_INT_REPORT_TB_GENERATE_REPORT);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    private DeviceId deviceId;
    private static final int DEFAULT_PRIORITY = 10000;

    private boolean setupBehaviour() {
        deviceId = this.data().deviceId();
        flowRuleService = handler().get(FlowRuleService.class);
        coreService = handler().get(CoreService.class);
        appId = coreService.getAppId(PIPELINE_APP_NAME);
        if (appId == null) {
            log.warn("Application ID is null. Cannot initialize behaviour.");
            return false;
        }
        return true;
    }

    @Override
    public boolean init() {
        if (!setupBehaviour()) {
            return false;
        }

        PiActionParam transitIdParam = new PiActionParam(
                IntConstants.SWITCH_ID,
                ImmutableByteSequence.copyFrom(
                        Integer.parseInt(deviceId.toString().substring(
                                deviceId.toString().length() - 2))));
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(PiCriterion.builder().matchExact(
                        IntConstants.HDR_INT_IS_VALID, (byte) 0x01)
                         .build())
                .build();
        PiAction transitAction = PiAction.builder()
                .withId(IntConstants.EGRESS_PROCESS_INT_TRANSIT_INIT_METADATA)
                .withParameter(transitIdParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(transitAction)
                .build();

        FlowRule transitFlowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(appId)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .forDevice(deviceId)
                .forTable(IntConstants.EGRESS_PROCESS_INT_TRANSIT_TB_INT_INSERT)
                .build();

        flowRuleService.applyFlowRules(transitFlowRule);

        return true;
    }

    @Override
    public boolean setSourcePort(PortNumber port) {
        if (!setupBehaviour()) {
            return false;
        }

        // process_int_source_sink.tb_set_source for each host-facing port
        PiCriterion ingressCriterion = PiCriterion.builder()
                .matchExact(IntConstants.HDR_STANDARD_METADATA_INGRESS_PORT, port.toLong())
                .build();
        TrafficSelector srcSelector = DefaultTrafficSelector.builder()
                .matchPi(ingressCriterion)
                .build();
        PiAction setSourceAct = PiAction.builder()
                .withId(IntConstants.INGRESS_PROCESS_INT_SOURCE_SINK_INT_SET_SOURCE)
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
                .forTable(IntConstants.INGRESS_PROCESS_INT_SOURCE_SINK_TB_SET_SOURCE)
                .build();
        flowRuleService.applyFlowRules(srcFlowRule);
        return true;
    }

    @Override
    public boolean setSinkPort(PortNumber port) {
        if (!setupBehaviour()) {
            return false;
        }

        // process_set_source_sink.tb_set_sink
        PiCriterion egressCriterion = PiCriterion.builder()
                .matchExact(IntConstants.HDR_STANDARD_METADATA_EGRESS_SPEC, port.toLong())
                .build();
        TrafficSelector sinkSelector = DefaultTrafficSelector.builder()
                .matchPi(egressCriterion)
                .build();
        PiAction setSinkAct = PiAction.builder()
                .withId(IntConstants.INGRESS_PROCESS_INT_SOURCE_SINK_INT_SET_SINK)
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
                .forTable(IntConstants.INGRESS_PROCESS_INT_SOURCE_SINK_TB_SET_SINK)
                .build();
        flowRuleService.applyFlowRules(sinkFlowRule);
        return true;
    }

    @Override
    public boolean addIntObjective(IntObjective obj) {
        // TODO: support different types of watchlist other than flow watchlist

        return processIntObjective(obj, true);
    }

    @Override
    public boolean removeIntObjective(IntObjective obj) {
        return processIntObjective(obj, false);
    }

    @Override
    public boolean setupIntConfig(IntDeviceConfig config) {
        return setupIntReportInternal(config);
    }

    @Override
    public void cleanup() {
        if (!setupBehaviour()) {
            return;
        }

        StreamSupport.stream(flowRuleService.getFlowEntries(
                data().deviceId()).spliterator(), false)
                .filter(f -> f.table().type() == TableId.Type.PIPELINE_INDEPENDENT)
                .filter(f -> TABLES_TO_CLEANUP.contains((PiTableId) f.table()))
                .forEach(flowRuleService::removeFlowRules);
    }

    @Override
    public boolean supportsFunctionality(IntFunctionality functionality) {
        switch (functionality) {
            case SOURCE:
            case SINK:
            case TRANSIT:
                return true;
            default:
                log.warn("Unknown functionality {}", functionality);
                return false;
        }
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
        int instructionBitmap = buildInstructionBitmap(obj.metadataTypes());
        PiActionParam hopMetaLenParam = new PiActionParam(
                IntConstants.HOP_METADATA_LEN,
                ImmutableByteSequence.copyFrom(Integer.bitCount(instructionBitmap)));
        PiActionParam hopCntParam = new PiActionParam(
                IntConstants.REMAINING_HOP_CNT,
                ImmutableByteSequence.copyFrom(MAXHOP));
        PiActionParam inst0003Param = new PiActionParam(
                IntConstants.INS_MASK0003,
                ImmutableByteSequence.copyFrom((instructionBitmap >> 12) & 0xF));
        PiActionParam inst0407Param = new PiActionParam(
                IntConstants.INS_MASK0407,
                ImmutableByteSequence.copyFrom((instructionBitmap >> 8) & 0xF));

        PiAction intSourceAction = PiAction.builder()
                .withId(IntConstants.INGRESS_PROCESS_INT_SOURCE_INT_SOURCE_DSCP)
                .withParameter(hopMetaLenParam)
                .withParameter(hopCntParam)
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
                                    IntConstants.HDR_LOCAL_METADATA_L4_SRC_PORT,
                                    ((TcpPortCriterion) criterion).tcpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case UDP_SRC:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    IntConstants.HDR_LOCAL_METADATA_L4_SRC_PORT,
                                    ((UdpPortCriterion) criterion).udpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case TCP_DST:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    IntConstants.HDR_LOCAL_METADATA_L4_DST_PORT,
                                    ((TcpPortCriterion) criterion).tcpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case UDP_DST:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    IntConstants.HDR_LOCAL_METADATA_L4_DST_PORT,
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
                .forTable(IntConstants.INGRESS_PROCESS_INT_SOURCE_TB_INT_SOURCE)
                .fromApp(appId)
                .withIdleTimeout(IDLE_TIMEOUT)
                .build();
    }

    private int buildInstructionBitmap(Set<IntMetadataType> metadataTypes) {
        int instBitmap = 0;
        for (IntMetadataType metadataType : metadataTypes) {
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
     * Returns a subset of Criterion from given selector, which is unsupported
     * by this INT pipeline.
     *
     * @param selector a traffic selector
     * @return a subset of Criterion from given selector, unsupported by this
     * INT pipeline, empty if all criteria are supported.
     */
    private Set<Criterion> unsupportedSelectors(TrafficSelector selector) {
        return selector.criteria().stream()
                .filter(criterion -> !SUPPORTED_CRITERION.contains(criterion.type()))
                .collect(Collectors.toSet());
    }

    private boolean processIntObjective(IntObjective obj, boolean install) {
        if (!setupBehaviour()) {
            return false;
        }
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

    private boolean setupIntReportInternal(IntDeviceConfig cfg) {
        if (!setupBehaviour()) {
            return false;
        }

        FlowRule reportRule = buildReportEntry(cfg);
        if (reportRule != null) {
            flowRuleService.applyFlowRules(reportRule);
            log.info("Report entry {} has been added to {}", reportRule, this.data().deviceId());
            return true;
        } else {
            log.warn("Failed to add report entry on {}", this.data().deviceId());
            return false;
        }
    }

    private FlowRule buildReportEntry(IntDeviceConfig cfg) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(PiCriterion.builder().matchExact(
                        IntConstants.HDR_INT_IS_VALID, (byte) 0x01)
                                 .build())
                .build();
        PiActionParam srcMacParam = new PiActionParam(
                IntConstants.SRC_MAC,
                ImmutableByteSequence.copyFrom(cfg.sinkMac().toBytes()));
        PiActionParam nextHopMacParam = new PiActionParam(
                IntConstants.MON_MAC,
                ImmutableByteSequence.copyFrom(cfg.collectorNextHopMac().toBytes()));
        PiActionParam srcIpParam = new PiActionParam(
                IntConstants.SRC_IP,
                ImmutableByteSequence.copyFrom(cfg.sinkIp().toOctets()));
        PiActionParam monIpParam = new PiActionParam(
                IntConstants.MON_IP,
                ImmutableByteSequence.copyFrom(cfg.collectorIp().toOctets()));
        PiActionParam monPortParam = new PiActionParam(
                IntConstants.MON_PORT,
                ImmutableByteSequence.copyFrom(cfg.collectorPort().toInt()));
        PiAction reportAction = PiAction.builder()
                .withId(IntConstants.EGRESS_PROCESS_INT_REPORT_DO_REPORT_ENCAPSULATION)
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
                .forTable(IntConstants.EGRESS_PROCESS_INT_REPORT_TB_GENERATE_REPORT)
                .build();
    }

}
