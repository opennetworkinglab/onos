/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.driver.pipeline.ofdpa;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupKey;
import org.slf4j.Logger;


/**
 * Driver for software switch emulation of the OFDPA 2.0 pipeline.
 * The software switch is the CPqD OF 1.3 switch. Unfortunately the CPqD switch
 * does not handle vlan tags and mpls labels simultaneously, which requires us
 * to do some workarounds in the driver. This driver is meant for the use of
 * the cpqd switch when MPLS is not a requirement from the ofdpa pipeline. As a
 * result this driver correctly handles both incoming untagged and vlan-tagged
 * packets.
 *
 */
public class CpqdOfdpa2VlanPipeline extends CpqdOfdpa2Pipeline {

    private final Logger log = getLogger(getClass());

    @Override
    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.CpqdOfdpa2VlanPipeline");
    }

    @Override
    protected void initGroupHander(PipelinerContext context) {
        groupHandler = new CpqdOfdpa2GroupHandler();
        groupHandler.init(deviceId, context);
    }

    /*
     * Cpqd emulation does not handle vlan tags and mpls labels correctly.
     * Since this driver does not deal with MPLS, there is no need for
     * working around VLAN tags. In particular we do not pop off vlan tags in
     * the middle of the pipeline.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processEthDstFilter
     */
    @Override
    protected List<FlowRule> processEthDstFilter(PortCriterion portCriterion,
                                                 EthCriterion ethCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 VlanId assignedVlan,
                                                 ApplicationId applicationId) {
        // Consider PortNumber.ANY as wildcard. Match ETH_DST only
        if (portCriterion != null && portCriterion.port() == PortNumber.ANY) {
            return processEthDstOnlyFilter(ethCriterion, applicationId);
        }

        // Multicast MAC
        if (ethCriterion.mask() != null) {
            return processMcastEthDstFilter(ethCriterion, applicationId);
        }

        //handling untagged packets via assigned VLAN
        if (vidCriterion.vlanId() == VlanId.NONE) {
            vidCriterion = (VlanIdCriterion) Criteria.matchVlanId(assignedVlan);
        }
        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<PortNumber>();
        if (portCriterion.port() == PortNumber.ALL) {
            for (Port port : deviceService.getPorts(deviceId)) {
                if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                    portnums.add(port.number());
                }
            }
        } else {
            portnums.add(portCriterion.port());
        }

        List<FlowRule> rules = new ArrayList<FlowRule>();
        for (PortNumber pnum : portnums) {
            // for unicast IP packets
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.matchVlanId(vidCriterion.vlanId());
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchEthDst(ethCriterion.mac());

            treatment.transition(UNICAST_ROUTING_TABLE);
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(TMAC_TABLE).build();
            rules.add(rule);
        }
        return rules;
    }

    /*
     * In the OF-DPA 2.0 pipeline, versatile forwarding objectives go to the
     * ACL table. Since we do not pop off vlans in the TMAC table we can continue
     * to match on vlans in the ACL table if necessary.
     */
    @Override
    protected Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.info("Processing versatile forwarding objective");

        EthTypeCriterion ethType =
                (EthTypeCriterion) fwd.selector().getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        if (fwd.nextId() == null && fwd.treatment() == null) {
            log.error("Forwarding objective {} from {} must contain "
                    + "nextId or Treatment", fwd.selector(), fwd.appId());
            return Collections.emptySet();
        }

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        fwd.selector().criteria().forEach(criterion -> {
            if (criterion instanceof VlanIdCriterion) {
                VlanId vlanId = ((VlanIdCriterion) criterion).vlanId();
                // ensure that match does not include vlan = NONE as OF-DPA does not
                // match untagged packets this way in the ACL table.
                if (vlanId.equals(VlanId.NONE)) {
                    return;
                }
            }
            sbuilder.add(criterion);
        });

        // XXX driver does not currently do type checking as per Tables 65-67 in
        // OFDPA 2.0 spec. The only allowed treatment is a punt to the controller.
        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            for (Instruction ins : fwd.treatment().allInstructions()) {
                if (ins instanceof OutputInstruction) {
                    OutputInstruction o = (OutputInstruction) ins;
                    if (o.port() == PortNumber.CONTROLLER) {
                        ttBuilder.add(o);
                    } else {
                        log.warn("Only allowed treatments in versatile forwarding "
                                + "objectives are punts to the controller");
                    }
                } else {
                    log.warn("Cannot process instruction in versatile fwd {}", ins);
                }
            }
        }
        if (fwd.nextId() != null) {
            // overide case
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
            // we only need the top level group's key to point the flow to it
            Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
            if (group == null) {
                log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                         gkeys.get(0).peekFirst(), fwd.nextId(), deviceId);
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
            ttBuilder.deferred().group(group.id());
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(ttBuilder.build())
                .makePermanent()
                .forTable(ACL_TABLE);
        return Collections.singletonList(ruleBuilder.build());
    }


}
