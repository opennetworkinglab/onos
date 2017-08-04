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
package org.onosproject.drivers.corsa;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * OpenvSwitch emulation of the Corsa pipeline handler.
 */
public class OvsCorsaPipeline extends AbstractCorsaPipeline {

    private final Logger log = getLogger(getClass());

    protected static final int MAC_TABLE = 0;
    protected static final int VLAN_MPLS_TABLE = 1;
    protected static final int VLAN_TABLE = 2;
    //protected static final int MPLS_TABLE = 3;
    protected static final int ETHER_TABLE = 4;
    protected static final int COS_MAP_TABLE = 5;
    protected static final int FIB_TABLE = 6;
    protected static final int LOCAL_TABLE = 9;

    @Override
    protected Collection<FlowRule> processArpTraffic(ForwardingObjective fwd, FlowRule.Builder rule) {
        log.warn("Driver automatically handles ARP packets by punting to controller "
                + " from ETHER table");
        pass(fwd);
        return Collections.emptyList();
    }

    @Override
    protected Collection<FlowRule> processLinkDiscovery(ForwardingObjective fwd, FlowRule.Builder rule) {
        log.warn("Driver currently does not currently handle LLDP packets");
        fail(fwd, ObjectiveError.UNSUPPORTED);
        return Collections.emptyList();
    }

    @Override
    protected Collection<FlowRule> processIpTraffic(ForwardingObjective fwd, FlowRule.Builder rule) {
        IPCriterion ipSrc = (IPCriterion) fwd.selector()
                .getCriterion(Criterion.Type.IPV4_SRC);
        if (ipSrc != null) {
            log.warn("Driver does not currently handle matching Src IP");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }
        IPCriterion ipDst = (IPCriterion) fwd.selector()
                .getCriterion(Criterion.Type.IPV4_DST);
        if (ipDst != null) {
            log.error("Driver handles Dst IP matching as specific forwarding "
                    + "objective, not versatile");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }
        IPProtocolCriterion ipProto = (IPProtocolCriterion) fwd.selector()
                .getCriterion(Criterion.Type.IP_PROTO);
        if (ipProto != null && ipProto.protocol() == IPv4.PROTOCOL_TCP) {
            log.warn("Driver automatically punts all packets reaching the "
                    + "LOCAL table to the controller");
            pass(fwd);
            return Collections.emptySet();
        }
        return Collections.emptySet();
    }

    @Override
    protected FlowRule.Builder processSpecificRoutingRule(FlowRule.Builder rb) {
        return rb.forTable(FIB_TABLE);
    }

    @Override
    protected FlowRule.Builder processIpFilter(FilteringObjective filt, IPCriterion ip, PortCriterion port) {
        log.debug("adding rule for IP: {}", ip.ip());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ip.ip());
        treatment.transition(LOCAL_TABLE);
        return DefaultFlowRule.builder()
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(HIGHEST_PRIORITY)
                .makePermanent()
                .forTable(FIB_TABLE);
    }

    @Override
    protected FlowRule.Builder processVlanFiler(FilteringObjective filt, VlanIdCriterion vlan, PortCriterion port) {
        log.debug("adding rule for VLAN: {}", vlan.vlanId());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchVlanId(vlan.vlanId());
        selector.matchInPort(port.port());
        treatment.transition(ETHER_TABLE);
        treatment.deferred().popVlan();
        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .makePermanent()
                .forTable(VLAN_TABLE);
    }


    protected FlowRule.Builder processEthFiler(FilteringObjective filt, EthCriterion eth, PortCriterion port) {
        log.debug("adding rule for MAC: {}", eth.mac());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthDst(eth.mac());
        treatment.transition(VLAN_MPLS_TABLE);
        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .makePermanent()
                .forTable(MAC_TABLE);
    }

    @Override
    protected void initializePipeline() {
        processMacTable(true);
        processVlanMplsTable(true);
        processVlanTable(true);
        processEtherTable(true);
        processCosTable(true);
        processFibTable(true);
        processLocalTable(true);
    }

    private void processMacTable(boolean install) {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;

        // Bcast rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        selector.matchEthDst(MacAddress.BROADCAST);
        treatment.transition(VLAN_MPLS_TABLE);

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(MAC_TABLE).build();
        processFlowRule(true, rule, "Provisioned mac table transition");

        //Drop rule
        processTableMissDrop(true, MAC_TABLE, "Provisioned mac table drop action");

    }

    protected void processVlanMplsTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        selector.matchVlanId(VlanId.ANY);
        treatment.transition(VLAN_TABLE);

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(VLAN_MPLS_TABLE).build();

        processFlowRule(true, rule, "Provisioned vlan/mpls table");
    }

    private void processVlanTable(boolean install) {
        processTableMissDrop(true, VLAN_TABLE, "Provisioned vlan table");
    }

    private void processEtherTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
        .matchEthType(Ethernet.TYPE_ARP);
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder()
                .punt();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ETHER_TABLE).build();

        processFlowRule(true, rule, "Provisioned ether table");
        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4);
        treatment = DefaultTrafficTreatment.builder()
        .transition(COS_MAP_TABLE);

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withPriority(CONTROLLER_PRIORITY)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .fromApp(appId)
                .makePermanent()
                .forTable(ETHER_TABLE).build();
        processFlowRule(true, rule, "Provisioned ether table");

        //Drop rule
        processTableMissDrop(true, VLAN_TABLE, "Provisioned ether table");

    }

    private void processCosTable(boolean install) {
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder()
                .transition(FIB_TABLE);
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(COS_MAP_TABLE).build();
        processFlowRule(true, rule, "Provisioned cos table");

    }

    private void processFibTable(boolean install) {
        processTableMissDrop(true, FIB_TABLE, "Provisioned FIB table");
    }

    private void processLocalTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder()
        .punt();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(LOCAL_TABLE).build();

        processFlowRule(true, rule, "Provisioned Local table");
    }


}
