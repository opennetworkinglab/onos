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
package org.onosproject.pipelines.fabric.impl.behaviour;

import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.behaviour.inbandtelemetry.IntMetadataType;
import org.onosproject.net.behaviour.inbandtelemetry.IntDeviceConfig;
import org.onosproject.net.behaviour.inbandtelemetry.IntObjective;
import org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;
import org.onosproject.pipelines.fabric.impl.FabricPipeconfLoader;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.onlab.util.ImmutableByteSequence.copyFrom;

/**
 * Implementation of INT programmable behavior for fabric.p4. Currently supports
 * only SOURCE and TRANSIT functionalities.
 */
public class FabricIntProgrammable extends AbstractFabricHandlerBehavior
        implements IntProgrammable {

    // TODO: change this value to the value of diameter of a network.
    private static final int DEFAULT_PRIORITY = 10000;
    private static final int MAXHOP = 64;
    private static final int PORTMASK = 0xffff;
    private static final int PKT_INSTANCE_TYPE_INGRESS_CLONE = 1;

    private static final Set<Criterion.Type> SUPPORTED_CRITERION = Sets.newHashSet(
            Criterion.Type.IPV4_DST, Criterion.Type.IPV4_SRC,
            Criterion.Type.UDP_SRC, Criterion.Type.UDP_DST,
            Criterion.Type.TCP_SRC, Criterion.Type.TCP_DST,
            Criterion.Type.IP_PROTO);

    private static final Set<PiTableId> TABLES_TO_CLEANUP = Sets.newHashSet(
            FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_TB_INT_INSERT,
            FabricConstants.FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_TB_SET_SOURCE,
            FabricConstants.FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_TB_SET_SINK,
            FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_TB_INT_SOURCE,
            FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_REPORT_TB_GENERATE_REPORT
    );

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowRuleService flowRuleService;

    private CoreService coreService;
    private NetworkConfigService cfgService;
    private DeviceId deviceId;
    private ApplicationId appId;

    /**
     * Creates a new instance of this behavior with the given capabilities.
     *
     * @param capabilities capabilities
     */
    protected FabricIntProgrammable(FabricCapabilities capabilities) {
        super(capabilities);
    }

    /**
     * Create a new instance of this behaviour. Used by the abstract projectable
     * model (i.e., {@link org.onosproject.net.Device#as(Class)}.
     */
    public FabricIntProgrammable() {
        super();
    }

    private boolean setupBehaviour() {
        deviceId = this.data().deviceId();
        flowRuleService = handler().get(FlowRuleService.class);
        coreService = handler().get(CoreService.class);
        cfgService = handler().get(NetworkConfigService.class);
        appId = coreService.getAppId(FabricPipeconfLoader.PIPELINE_APP_NAME);
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

        // FIXME: create config class for INT to allow specifying arbitrary
        //  switch IDs. The one for the GeneralDeviceProvider was temporary and
        //  now has been removed. For now we use the chassis ID.
        // final GeneralProviderDeviceConfig cfg = cfgService.getConfig(
        //         deviceId, GeneralProviderDeviceConfig.class);
        // if (cfg == null) {
        //     log.warn("Missing GeneralProviderDevice config for {}", deviceId);
        //     return false;
        // }
        // final String switchId = cfg.protocolsInfo().containsKey("int") ?
        //         cfg.protocolsInfo().get("int").configValues().get("switchId")
        //         : null;
        // if (switchId == null || switchId.isEmpty()) {
        //     log.warn("Missing INT device config for {}", deviceId);
        //     return false;
        // }

        PiActionParam transitIdParam = new PiActionParam(
                FabricConstants.SWITCH_ID,
                copyFrom(handler().get(DeviceService.class)
                                 .getDevice(deviceId).chassisId().id()));

        PiAction transitAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INIT_METADATA)
                .withParameter(transitIdParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(transitAction)
                .build();
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(PiCriterion.builder().matchExact(
                        FabricConstants.HDR_INT_IS_VALID, (byte) 0x01)
                                 .build())
                .build();

        FlowRule transitFlowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(appId)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .forDevice(deviceId)
                .forTable(FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_TB_INT_INSERT)
                .build();

        flowRuleService.applyFlowRules(transitFlowRule);
        return true;
    }

    @Override
    public boolean setSourcePort(PortNumber port) {

        if (!setupBehaviour()) {
            return false;
        }

        PiCriterion ingressCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_IG_PORT, port.toLong())
                .build();
        TrafficSelector srcSelector = DefaultTrafficSelector.builder()
                .matchPi(ingressCriterion)
                .build();
        PiAction setSourceAct = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_INT_SET_SOURCE)
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
                .forTable(FabricConstants.FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_TB_SET_SOURCE)
                .build();
        flowRuleService.applyFlowRules(srcFlowRule);
        return true;
    }

    @Override
    public boolean setSinkPort(PortNumber port) {

        if (!setupBehaviour()) {
            return false;
        }

        PiCriterion egressCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_EG_PORT, port.toLong())
                .build();
        TrafficSelector sinkSelector = DefaultTrafficSelector.builder()
                .matchPi(egressCriterion)
                .build();
        PiAction setSinkAct = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_INT_SET_SINK)
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
                .forTable(FabricConstants.FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_TB_SET_SINK)
                .build();
        flowRuleService.applyFlowRules(sinkFlowRule);
        return true;
    }

    @Override
    public boolean addIntObjective(IntObjective obj) {

        if (!setupBehaviour()) {
            return false;
        }

        return processIntObjective(obj, true);
    }

    @Override
    public boolean removeIntObjective(IntObjective obj) {

        if (!setupBehaviour()) {
            return false;
        }

        return processIntObjective(obj, false);
    }

    @Override
    public boolean setupIntConfig(IntDeviceConfig config) {

        if (!setupBehaviour()) {
            return false;
        }

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
        // Sink not fully supported yet.
        return functionality == IntFunctionality.SOURCE || functionality == IntFunctionality.TRANSIT;
    }

    private FlowRule buildWatchlistEntry(IntObjective obj) {
        int instructionBitmap = buildInstructionBitmap(obj.metadataTypes());
        PiActionParam maxHopParam = new PiActionParam(
                FabricConstants.MAX_HOP,
                copyFrom(MAXHOP));
        PiActionParam instCntParam = new PiActionParam(
                FabricConstants.INS_CNT,
                copyFrom(Integer.bitCount(instructionBitmap)));
        PiActionParam inst0003Param = new PiActionParam(
                FabricConstants.INS_MASK0003,
                copyFrom((instructionBitmap >> 12) & 0xF));
        PiActionParam inst0407Param = new PiActionParam(
                FabricConstants.INS_MASK0407,
                copyFrom((instructionBitmap >> 8) & 0xF));

        PiAction intSourceAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_INT_SOURCE_DSCP)
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
                                    FabricConstants.HDR_L4_SPORT,
                                    ((TcpPortCriterion) criterion).tcpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case UDP_SRC:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    FabricConstants.HDR_L4_SPORT,
                                    ((UdpPortCriterion) criterion).udpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case TCP_DST:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    FabricConstants.HDR_L4_DPORT,
                                    ((TcpPortCriterion) criterion).tcpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                case UDP_DST:
                    sBuilder.matchPi(
                            PiCriterion.builder().matchTernary(
                                    FabricConstants.HDR_L4_DPORT,
                                    ((UdpPortCriterion) criterion).udpPort().toInt(), PORTMASK)
                                    .build());
                    break;
                default:
                    log.warn("Unsupported criterion type: {}", criterion.type());
            }
        }

        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(sBuilder.build())
                .withTreatment(instTreatment)
                .withPriority(DEFAULT_PRIORITY)
                .forTable(FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_TB_INT_SOURCE)
                .fromApp(appId)
                .makePermanent()
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
        if (install && !unsupportedSelectors(obj.selector()).isEmpty()) {
            log.warn("Criteria {} not supported by {} for INT watchlist",
                     unsupportedSelectors(obj.selector()), deviceId);
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
        // Report not fully supported yet.
        return true;
        // FlowRule reportRule = buildReportEntry(cfg, PKT_INSTANCE_TYPE_INGRESS_CLONE);
        // if (reportRule != null) {
        //     flowRuleService.applyFlowRules(reportRule);
        //     log.info("Report entry {} has been added to {}", reportRule, this.data().deviceId());
        //     return true;
        // } else {
        //     log.warn("Failed to add report entry on {}", this.data().deviceId());
        //     return false;
        // }
    }

    private FlowRule buildReportEntry(IntDeviceConfig cfg, int type) {

        if (!setupBehaviour()) {
            return null;
        }

        PiActionParam srcMacParam = new PiActionParam(
                FabricConstants.SRC_MAC,
                copyFrom(cfg.sinkMac().toBytes()));
        PiActionParam nextHopMacParam = new PiActionParam(
                FabricConstants.MON_MAC,
                copyFrom(cfg.collectorNextHopMac().toBytes()));
        PiActionParam srcIpParam = new PiActionParam(
                FabricConstants.SRC_IP,
                copyFrom(cfg.sinkIp().toOctets()));
        PiActionParam monIpParam = new PiActionParam(
                FabricConstants.MON_IP,
                copyFrom(cfg.collectorIp().toOctets()));
        PiActionParam monPortParam = new PiActionParam(
                FabricConstants.MON_PORT,
                copyFrom(cfg.collectorPort().toInt()));
        PiAction reportAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_REPORT_DO_REPORT_ENCAPSULATION)
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
                .withTreatment(treatment)
                .fromApp(appId)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .forDevice(this.data().deviceId())
                .forTable(FabricConstants.FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_REPORT_TB_GENERATE_REPORT)
                .build();
    }

}
