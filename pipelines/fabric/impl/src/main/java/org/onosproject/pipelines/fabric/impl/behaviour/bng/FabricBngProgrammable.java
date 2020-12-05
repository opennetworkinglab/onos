/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.bng;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.core.ApplicationId;
import org.onosproject.drivers.p4runtime.AbstractP4RuntimeHandlerBehaviour;
import org.onosproject.net.behaviour.BngProgrammable;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellHandle;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implementation of BngProgrammable for fabric.p4.
 */
public class FabricBngProgrammable extends AbstractP4RuntimeHandlerBehaviour
        implements BngProgrammable {

    // Default priority of the inserted BNG rules.
    private static final int DEFAULT_PRIORITY = 10;
    // The index at which control plane packets are counted before the attachment is created.
    private static final int DEFAULT_CONTROL_INDEX = 0;

    private static final ImmutableBiMap<BngCounterType, PiCounterId> COUNTER_MAP =
            ImmutableBiMap.<BngCounterType, PiCounterId>builder()
                    .put(BngCounterType.DOWNSTREAM_RX, FabricConstants.FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_C_LINE_RX)
                    .put(BngCounterType.DOWNSTREAM_TX, FabricConstants.FABRIC_EGRESS_BNG_EGRESS_DOWNSTREAM_C_LINE_TX)
                    .put(BngCounterType.UPSTREAM_TX, FabricConstants.FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_C_TERMINATED)
                    .put(BngCounterType.UPSTREAM_DROPPED, FabricConstants.FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_C_DROPPED)
                    .put(BngCounterType.CONTROL_PLANE, FabricConstants.FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_C_CONTROL)
                    .build();

    // FIXME: add these counters to the BNG pipeline
    private static final ImmutableSet<BngCounterType> UNSUPPORTED_COUNTER =
            ImmutableSet.of(BngCounterType.UPSTREAM_RX, BngCounterType.DOWNSTREAM_DROPPED);

    private FlowRuleService flowRuleService;
    private FabricBngProgrammableService bngProgService;
    private FabricCapabilities capabilities;

    @Override
    protected boolean setupBehaviour(String opName) {
        if (!super.setupBehaviour(opName)) {
            return false;
        }
        flowRuleService = handler().get(FlowRuleService.class);
        bngProgService = handler().get(FabricBngProgrammableService.class);
        capabilities = new FabricCapabilities(pipeconf);

        if (!capabilities.supportBng()) {
            log.warn("Pipeconf {} on {} does not support BNG capabilities, " +
                            "cannot perform {}",
                    pipeconf.id(), deviceId, opName);
            return false;
        }

        return true;
    }

    @Override
    public boolean init(ApplicationId appId) {
        if (setupBehaviour("init()")) {
            this.setupPuntToCpu(appId);
            return true;
        }
        return false;
    }

    @Override
    public void cleanUp(ApplicationId appId) {
        if (!setupBehaviour("cleanUp()")) {
            return;
        }
        // Remove flow rules.
        var flowEntries = flowRuleService.getFlowEntriesById(appId);
        flowRuleService.removeFlowRules(
                Iterables.toArray(flowEntries, FlowRule.class));
        // Release line IDs found in removed flow rules.
        getLineIdsFromFlowRules(flowEntries)
                .forEach(this::releaseLineId);
        // Reset counters.
        this.resetControlTrafficCounter();
    }

    @Override
    public void setupAttachment(Attachment attachment) throws BngProgrammableException {
        if (!setupBehaviour("setupAttachment()")) {
            return;
        }
        checkAttachment(attachment);
        List<FlowRule> lstFlowRules = Lists.newArrayList();
        lstFlowRules.add(buildTLineMapFlowRule(attachment));
        // If the line is not active do not generate the rule for the table
        // t_pppoe_term_v4 since term_disabled is @defaultonly action
        if (attachment.lineActive()) {
            lstFlowRules.add(buildTPppoeTermV4FlowRule(attachment));
        }
        lstFlowRules.add(buildTLineSessionMapFlowRule(attachment));

        lstFlowRules.forEach(flowRule -> flowRuleService.applyFlowRules(flowRule));
    }

    @Override
    public void removeAttachment(Attachment attachment) throws BngProgrammableException {
        if (!setupBehaviour("removeAttachment()")) {
            return;
        }
        checkAttachment(attachment);
        List<FlowRule> lstFlowRules = Lists.newArrayList();
        lstFlowRules.add(buildTLineMapFlowRule(attachment));
        lstFlowRules.add(buildTPppoeTermV4FlowRule(attachment));
        lstFlowRules.add(buildTLineSessionMapFlowRule(attachment));

        lstFlowRules.forEach(flowRule -> flowRuleService.removeFlowRules(flowRule));

        releaseLineId(attachment);
    }

    @Override
    public Map<BngCounterType, PiCounterCellData> readCounters(Attachment attachment)
            throws BngProgrammableException {
        if (!setupBehaviour("readCounters()")) {
            return Maps.newHashMap();
        }
        checkAttachment(attachment);
        return readCounters(lineId(attachment), Set.of(BngCounterType.values()));
    }

    @Override
    public PiCounterCellData readCounter(Attachment attachment, BngCounterType counter)
            throws BngProgrammableException {
        if (!setupBehaviour("readCounter()")) {
            return null;
        }
        checkAttachment(attachment);
        return readCounters(lineId(attachment), Set.of(counter))
                .getOrDefault(counter, null);
    }

    @Override
    public void resetCounters(Attachment attachment)
            throws BngProgrammableException {
        if (!setupBehaviour("resetCounters()")) {
            return;
        }
        checkAttachment(attachment);
        resetCounters(lineId(attachment), Set.of(BngCounterType.values()));
    }

    @Override
    public PiCounterCellData readControlTrafficCounter()
            throws BngProgrammableException {
        if (!setupBehaviour("readControlTrafficCounter()")) {
            return null;
        }
        return readCounters(DEFAULT_CONTROL_INDEX, Set.of(BngCounterType.CONTROL_PLANE))
                .get(BngCounterType.CONTROL_PLANE);
    }

    @Override
    public void resetCounter(Attachment attachment, BngCounterType counter)
            throws BngProgrammableException {
        if (!setupBehaviour("resetCounter()")) {
            return;
        }
        checkAttachment(attachment);
        resetCounters(lineId(attachment), Set.of(counter));
    }

    @Override
    public void resetControlTrafficCounter() {
        if (!setupBehaviour("resetControlTrafficCounter()")) {
            return;
        }
        resetCounters(DEFAULT_CONTROL_INDEX, Set.of((BngCounterType.CONTROL_PLANE)));
    }

    /**
     * Read the specified counter at a specific index.
     *
     * @param index    The index of the counter.
     * @param counters The set of counters to read.
     */
    private Map<BngCounterType, PiCounterCellData> readCounters(
            long index,
            Set<BngCounterType> counters) throws BngProgrammableException {
        Map<BngCounterType, PiCounterCellData> readValues = Maps.newHashMap();
        Set<PiCounterCellId> counterCellIds = counters.stream()
                .filter(c -> !UNSUPPORTED_COUNTER.contains(c))
                .map(c -> PiCounterCellId.ofIndirect(COUNTER_MAP.get(c), index))
                .collect(Collectors.toSet());
        // Check if there is any counter to read.
        if (counterCellIds.size() != 0) {
            Set<PiCounterCellHandle> counterCellHandles = counterCellIds.stream()
                    .map(cId -> PiCounterCellHandle.of(this.deviceId, cId))
                    .collect(Collectors.toSet());

            // Query the device.
            Collection<PiCounterCell> counterEntryResponse = client.read(
                    p4DeviceId, pipeconf)
                    .handles(counterCellHandles).submitSync()
                    .all(PiCounterCell.class);

            if (counterEntryResponse.size() == 0) {
                throw new BngProgrammableException(
                        String.format("Error in reading counters %s", counters.toString()));
            }
            readValues.putAll(counterEntryResponse.stream().collect(
                    Collectors.toMap(counterCell -> COUNTER_MAP.inverse()
                                    .get(counterCell.cellId().counterId()),
                            PiCounterCell::data)));
        }
        return readValues;
    }

    /**
     * Reset the specified counters at a specific index.
     *
     * @param index    The index of the counter.
     * @param counters The set of counters to reset.
     */
    private void resetCounters(long index, Set<BngCounterType> counters) {
        Set<PiCounterCellId> counterCellIds = counters.stream()
                .filter(c -> !UNSUPPORTED_COUNTER.contains(c))
                .map(c -> PiCounterCellId.ofIndirect(COUNTER_MAP.get(c), index))
                .collect(Collectors.toSet());
        if (counterCellIds.isEmpty()) {
            // No counters to reset
            log.info("No counters to reset.");
            return;
        }
        Set<PiCounterCell> counterCellData = counterCellIds.stream()
                .map(cId -> new PiCounterCell(cId, 0, 0))
                .collect(Collectors.toSet());

        // Query the device.
        Collection<P4RuntimeWriteClient.EntityUpdateResponse> counterEntryResponse = client.write(
                p4DeviceId, pipeconf)
                .modify(counterCellData).submitSync()
                .all();
        counterEntryResponse.stream().filter(counterEntryResp -> !counterEntryResp.isSuccess())
                .forEach(counterEntryResp -> log.warn("A counter was not reset correctly: {}",
                        counterEntryResp.explanation()));
    }

    /**
     * Preliminary check on the submitted attachment.
     */
    private void checkAttachment(Attachment attachment) throws BngProgrammableException {
        if (attachment.type() != Attachment.AttachmentType.PPPoE) {
            throw new BngProgrammableException(
                    "Attachment {} is not a PPPoE Attachment");
        }
    }

    /**
     * Set the punt to CPU rules of the BNG from a specific Application ID.
     *
     * @param appId Application ID asking to recive BNG control plane packets.
     */
    private void setupPuntToCpu(ApplicationId appId) {
        for (Criterion c : PuntCpuCriterionFactory.getAllPuntCriterion()) {
            FlowRule flPuntCpu = buildTPppoeCpFlowRule((PiCriterion) c, appId);
            flowRuleService.applyFlowRules(flPuntCpu);
        }
    }

    /**
     * Build the Flow Rule for the table t_pppoe_term_v4 of the ingress
     * upstream.
     */
    private FlowRule buildTPppoeTermV4FlowRule(Attachment attachment)
            throws BngProgrammableException {
        PiCriterion criterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_LINE_ID,
                        lineId(attachment))
                .matchExact(FabricConstants.HDR_IPV4_SRC,
                        attachment.ipAddress().toOctets())
                .matchExact(FabricConstants.HDR_PPPOE_SESSION_ID,
                        attachment.pppoeSessionId())
                // TODO: match on MAC SRC address (antispoofing)
//                    .matchExact(FabricConstants.HDR_ETH_SRC,
//                                attachment.macAddress.toBytes())
                .build();
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder()
                .matchPi(criterion)
                .build();
        PiAction action = PiAction.builder()
                .withId(attachment.lineActive() ?
                        FabricConstants.FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_TERM_ENABLED_V4 :
                        FabricConstants.FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_TERM_DISABLED)
                .build();
        TrafficTreatment instTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(action)
                .build();
        return buildFlowRule(trafficSelector,
                instTreatment,
                FabricConstants.FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_T_PPPOE_TERM_V4,
                attachment.appId());
    }

    /**
     * Build the Flow Rule for the table t_line_session_map of the ingress
     * downstream.
     */
    private FlowRule buildTLineSessionMapFlowRule(Attachment attachment)
            throws BngProgrammableException {
        PiCriterion criterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_LINE_ID,
                        lineId(attachment))
                .build();
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder()
                .matchPi(criterion)
                .build();
        PiAction action;
        if (attachment.lineActive()) {
            action = PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_SET_SESSION)
                    .withParameter(new PiActionParam(FabricConstants.PPPOE_SESSION_ID,
                            attachment.pppoeSessionId()))
                    .build();
        } else {
            action = PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_DROP)
                    .build();
        }
        TrafficTreatment instTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(action)
                .build();
        return buildFlowRule(trafficSelector,
                instTreatment,
                FabricConstants.FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_T_LINE_SESSION_MAP,
                attachment.appId());
    }

    /**
     * Build the flow rule for the table t_line_map of the BNG-U (common to both
     * upstream and downstream).
     */
    private FlowRule buildTLineMapFlowRule(Attachment attachment)
            throws BngProgrammableException {
        PiCriterion criterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_S_TAG,
                        attachment.sTag().toShort())
                .matchExact(FabricConstants.HDR_C_TAG,
                        attachment.cTag().toShort())
                .build();
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder()
                .matchPi(criterion)
                .build();
        PiAction action = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_BNG_INGRESS_SET_LINE)
                .withParameter(new PiActionParam(FabricConstants.LINE_ID,
                        lineId(attachment)))
                .build();
        TrafficTreatment instTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(action)
                .build();
        return buildFlowRule(trafficSelector,
                instTreatment,
                FabricConstants.FABRIC_INGRESS_BNG_INGRESS_T_LINE_MAP,
                attachment.appId());
    }

    /**
     * Build the flow rule for the table t_pppoe_cp of the ingress upstream.
     */
    private FlowRule buildTPppoeCpFlowRule(PiCriterion criterion, ApplicationId appId) {
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder()
                .matchPi(criterion)
                .build();
        TrafficTreatment instTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(PiAction.builder()
                        .withId(FabricConstants.FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_PUNT_TO_CPU)
                        .build()
                )
                .build();
        return buildFlowRule(trafficSelector,
                instTreatment,
                FabricConstants.FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_T_PPPOE_CP,
                appId);
    }

    private FlowRule buildFlowRule(TrafficSelector trafficSelector,
                                   TrafficTreatment trafficTreatment,
                                   TableId tableId,
                                   ApplicationId appId) {
        return DefaultFlowRule.builder()
                .forDevice(data().deviceId())
                .withSelector(trafficSelector)
                .withTreatment(trafficTreatment)
                .withPriority(DEFAULT_PRIORITY)
                .forTable(tableId)
                .fromApp(appId)
                .makePermanent()
                .build();
    }

    private long lineId(Attachment attachment) throws BngProgrammableException {
        try {
            return bngProgService.getLineIdAllocator(deviceId, capabilities.bngMaxLineCount()).allocate(attachment);
        } catch (FabricBngLineIdAllocator.IdExhaustedException e) {
            throw new BngProgrammableException("Line IDs exhausted, unable to allocate a new one");
        }
    }

    private void releaseLineId(Attachment attachment) {
        bngProgService.getLineIdAllocator(deviceId, capabilities.bngMaxLineCount()).release(attachment);
    }

    private void releaseLineId(long id) {
        bngProgService.getLineIdAllocator(deviceId, capabilities.bngMaxLineCount()).release(id);
    }

    private Set<Long> getLineIdsFromFlowRules(Iterable<? extends FlowRule> rules) {
        // Extract the line ID found in the flow rule selector.
        return StreamSupport.stream(rules.spliterator(), true)
                .map(f -> (PiCriterion) f.selector().getCriterion(Criterion.Type.PROTOCOL_INDEPENDENT))
                .filter(Objects::nonNull)
                .map(c -> c.fieldMatch(FabricConstants.HDR_LINE_ID))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(m -> m.type() == PiMatchType.EXACT)
                .map(m -> ((PiExactFieldMatch) m).value())
                .map(b -> {
                    try {
                        return b.fit(Long.BYTES * 8);
                    } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
                        log.error("Invalid line ID found in flow rule: {} is bigger than a long! BUG?", b);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(b -> b.asReadOnlyBuffer().getLong())
                .collect(Collectors.toSet());
    }
}
