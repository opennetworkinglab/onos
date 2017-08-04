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
package org.onosproject.driver.pipeline;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.Group;
import org.slf4j.Logger;

/**
 * Spring-open driver implementation for Dell hardware switches.
 */
public class SpringOpenTTPDell extends SpringOpenTTP {

    /* Table IDs to be used for Dell Open Segment Routers*/
    private static final int DELL_TABLE_VLAN = 17;
    private static final int DELL_TABLE_TMAC = 18;
    private static final int DELL_TABLE_IPV4_UNICAST = 30;
    private static final int DELL_TABLE_MPLS = 25;
    private static final int DELL_TABLE_ACL = 40;

    private final Logger log = getLogger(getClass());

    //TODO: Store this info in the distributed store.
    private MacAddress deviceTMac = null;

    public SpringOpenTTPDell() {
        super();
        vlanTableId = DELL_TABLE_VLAN;
        tmacTableId = DELL_TABLE_TMAC;
        ipv4UnicastTableId = DELL_TABLE_IPV4_UNICAST;
        mplsTableId = DELL_TABLE_MPLS;
        aclTableId = DELL_TABLE_ACL;
    }

    @Override
    protected void setTableMissEntries() {
        // No need to set table-miss-entries in Dell switches
        return;
    }

    @Override
    //Dell switches need ETH_DST based match condition in all IP table entries.
    //So this method overrides the default spring-open behavior and adds
    //ETH_DST match condition while pushing IP table flow rules
    protected Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType = (EthTypeCriterion) selector
                .getCriterion(Criterion.Type.ETH_TYPE);
        if ((ethType == null) ||
                (ethType.ethType().toShort() != Ethernet.TYPE_IPV4) &&
                (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST)) {
            log.debug("processSpecific: Unsupported "
                    + "forwarding objective criteraia");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

        TrafficSelector.Builder filteredSelectorBuilder =
                DefaultTrafficSelector.builder();
        int forTableId = -1;
        if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
            if (deviceTMac == null) {
                log.debug("processSpecific: ETH_DST filtering "
                        + "objective is not set which is required "
                        + "before sending a IPv4 forwarding objective");
                //TODO: Map the error to more appropriate error code.
                fail(fwd, ObjectiveError.DEVICEMISSING);
                return Collections.emptySet();
            }
            filteredSelectorBuilder = filteredSelectorBuilder
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(deviceTMac)
                .matchIPDst(((IPCriterion) selector
                        .getCriterion(Criterion.Type.IPV4_DST))
                        .ip());
            forTableId = ipv4UnicastTableId;
            log.debug("processing IPv4 specific forwarding objective");
        } else {
            filteredSelectorBuilder = filteredSelectorBuilder
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(((MplsCriterion)
                   selector.getCriterion(Criterion.Type.MPLS_LABEL)).label());
            if (selector.getCriterion(Criterion.Type.MPLS_BOS) != null) {
                filteredSelectorBuilder.matchMplsBos(((MplsBosCriterion)
                        selector.getCriterion(Criterion.Type.MPLS_BOS)).mplsBos());
            }
            forTableId = mplsTableId;
            log.debug("processing MPLS specific forwarding objective");
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment
                .builder();
        if (fwd.treatment() != null) {
            for (Instruction i : fwd.treatment().allInstructions()) {
                treatmentBuilder.add(i);
            }
        }

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());

            if (next != null) {
                SpringOpenGroup soGroup = appKryo.deserialize(next.data());
                if (soGroup.dummy()) {
                    log.debug("Adding {} flow-actions for fwd. obj. {} -> next:{} "
                            + "in dev: {}", soGroup.treatment().allInstructions().size(),
                            fwd.id(), fwd.nextId(), deviceId);
                    for (Instruction ins : soGroup.treatment().allInstructions()) {
                        treatmentBuilder.add(ins);
                    }
                } else {
                    Group group = groupService.getGroup(deviceId, soGroup.key());

                    if (group == null) {
                        log.warn("The group left!");
                        fail(fwd, ObjectiveError.GROUPMISSING);
                        return Collections.emptySet();
                    }
                    treatmentBuilder.group(group.id());
                    log.debug("Adding OUTGROUP action to group:{} for fwd. obj. {} "
                            + "for next:{} in dev: {}", group.id(), fwd.id(),
                            fwd.nextId(), deviceId);
                }
            } else {
                log.warn("processSpecific: No associated next objective object");
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
        }

        TrafficSelector filteredSelector = filteredSelectorBuilder.build();
        TrafficTreatment treatment = treatmentBuilder.transition(aclTableId)
                .build();

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId()).withPriority(fwd.priority())
                .forDevice(deviceId).withSelector(filteredSelector)
                .withTreatment(treatment);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        ruleBuilder.forTable(forTableId);
        return Collections.singletonList(ruleBuilder.build());

    }

    @Override
    //Dell switches need ETH_DST based match condition in all IP table entries.
    //So while processing the ETH_DST based filtering objective, store
    //the device MAC to be used locally to use it while pushing the IP rules.
    protected List<FlowRule> processEthDstFilter(EthCriterion ethCriterion,
                                                 VlanIdCriterion vlanIdCriterion,
                                                 FilteringObjective filt,
                                                 VlanId assignedVlan,
                                                 ApplicationId applicationId) {
        // Store device termination Mac to be used in IP flow entries
        deviceTMac = ethCriterion.mac();

        log.debug("For now not adding any TMAC rules "
                + "into Dell switches as it is ignoring");

        return Collections.emptyList();
    }

    @Override
    protected List<FlowRule> processVlanIdFilter(VlanIdCriterion vlanIdCriterion,
                                                 FilteringObjective filt,
                                                 VlanId assignedVlan, VlanId modifiedVlan, VlanId pushedVlan,
                                                 boolean popVlan, boolean pushVlan,
                                                 ApplicationId applicationId) {
        log.debug("For now not adding any VLAN rules "
                + "into Dell switches as it is ignoring");

        return Collections.emptyList();
    }
}