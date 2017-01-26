/*
 * Copyright 2015-present Open Networking Laboratory
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
 *
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li, Heng Qi and Haisheng Yu
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
 */
package org.onosproject.acl.impl;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.acl.AclRule;
import org.onosproject.acl.AclService;
import org.onosproject.acl.AclStore;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.acl.RuleId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the ACL service.
 */
@Component(immediate = true)
@Service
public class AclManager implements AclService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected AclStore aclStore;

    private final Logger log = getLogger(getClass());
    private ApplicationId appId;
    private final HostListener hostListener = new InternalHostListener();
    private IdGenerator idGenerator;

    /**
     * Checks if the given IP address is in the given CIDR address.
     */
    private boolean checkIpInCidr(Ip4Address ip, Ip4Prefix cidr) {
        int offset = 32 - cidr.prefixLength();
        int cidrPrefix = cidr.address().toInt();
        int ipIntValue = ip.toInt();
        cidrPrefix = cidrPrefix >> offset;
        ipIntValue = ipIntValue >> offset;
        cidrPrefix = cidrPrefix << offset;
        ipIntValue = ipIntValue << offset;

        return (cidrPrefix == ipIntValue);
    }

    private class InternalHostListener implements HostListener {

        /**
         * Generate new ACL flow rules for new host following the given ACL rule.
         */
        private void processHostAddedEvent(HostEvent event, AclRule rule) {
            DeviceId deviceId = event.subject().location().deviceId();
            for (IpAddress address : event.subject().ipAddresses()) {
                if ((rule.srcIp() != null) ?
                        (checkIpInCidr(address.getIp4Address(), rule.srcIp())) :
                        (checkIpInCidr(address.getIp4Address(), rule.dstIp()))) {
                    if (!aclStore.checkIfRuleWorksInDevice(rule.id(), deviceId)) {
                        List<RuleId> allowingRuleList = aclStore
                                .getAllowingRuleByDenyingRule(rule.id());
                        if (allowingRuleList != null) {
                            for (RuleId allowingRuleId : allowingRuleList) {
                                generateAclFlow(aclStore.getAclRule(allowingRuleId), deviceId);
                            }
                        }
                        generateAclFlow(rule, deviceId);
                    }
                }
            }
        }

        @Override
        public void event(HostEvent event) {
            // if a new host appears and an existing rule denies
            // its traffic, a new ACL flow rule is generated.
            if (event.type() == HostEvent.Type.HOST_ADDED) {
                DeviceId deviceId = event.subject().location().deviceId();
                if (mastershipService.getLocalRole(deviceId) == MastershipRole.MASTER) {
                    for (AclRule rule : aclStore.getAclRules()) {
                        if (rule.action() != AclRule.Action.ALLOW) {
                            processHostAddedEvent(event, rule);
                        }
                    }
                }
            }
        }
    }

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.acl");
        hostService.addListener(hostListener);
        idGenerator = coreService.getIdGenerator("acl-ids");
        AclRule.bindIdGenerator(idGenerator);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        hostService.removeListener(hostListener);
        this.clearAcl();
        log.info("Stopped");
    }

    @Override
    public List<AclRule> getAclRules() {
        return aclStore.getAclRules();
    }

    /**
     * Checks if the new ACL rule matches an existing rule.
     * If existing allowing rules matches the new denying rule, store the mappings.
     *
     * @return true if the new ACL rule matches an existing rule, false otherwise
     */
    private boolean matchCheck(AclRule newRule) {
        for (AclRule existingRule : aclStore.getAclRules()) {
            if (newRule.checkMatch(existingRule)) {
                return true;
            }

            if (existingRule.action() == AclRule.Action.ALLOW
                    && newRule.action() == AclRule.Action.DENY) {
                if (existingRule.checkMatch(newRule)) {
                    aclStore.addDenyToAllowMapping(newRule.id(), existingRule.id());
                }
            }
        }
        return false;
    }

    @Override
    public boolean addAclRule(AclRule rule) {
        if (matchCheck(rule)) {
            return false;
        }
        aclStore.addAclRule(rule);
        log.info("ACL rule(id:{}) is added.", rule.id());
        if (rule.action() != AclRule.Action.ALLOW) {
            enforceRuleAdding(rule);
        }
        return true;
    }

    /**
     * Gets a set containing all devices connecting with the hosts
     * whose IP address is in the given CIDR IP address.
     */
    private Set<DeviceId> getDeviceIdSet(Ip4Prefix cidrAddr) {
        Set<DeviceId> deviceIdSet = new HashSet<>();
        final Iterable<Host> hosts = hostService.getHosts();

        if (cidrAddr.prefixLength() != 32) {
            for (Host h : hosts) {
                for (IpAddress a : h.ipAddresses()) {
                    if (checkIpInCidr(a.getIp4Address(), cidrAddr)) {
                        deviceIdSet.add(h.location().deviceId());
                    }
                }
            }
        } else {
            for (Host h : hosts) {
                for (IpAddress a : h.ipAddresses()) {
                    if (checkIpInCidr(a.getIp4Address(), cidrAddr)) {
                        deviceIdSet.add(h.location().deviceId());
                        return deviceIdSet;
                    }
                }
            }
        }
        return deviceIdSet;
    }

    /**
     * Enforces denying ACL rule by ACL flow rules.
     */
    private void enforceRuleAdding(AclRule rule) {
        Set<DeviceId> dpidSet;
        if (rule.srcIp() != null) {
            dpidSet = getDeviceIdSet(rule.srcIp());
        } else {
            dpidSet = getDeviceIdSet(rule.dstIp());
        }

        for (DeviceId deviceId : dpidSet) {
            List<RuleId> allowingRuleList = aclStore.getAllowingRuleByDenyingRule(rule.id());
            if (allowingRuleList != null) {
                for (RuleId allowingRuleId : allowingRuleList) {
                    generateAclFlow(aclStore.getAclRule(allowingRuleId), deviceId);
                }
            }
            generateAclFlow(rule, deviceId);
        }
    }

    /**
     * Generates ACL flow rule according to ACL rule
     * and install it into related device.
     */
    private void generateAclFlow(AclRule rule, DeviceId deviceId) {
        if (rule == null || aclStore.checkIfRuleWorksInDevice(rule.id(), deviceId)) {
            return;
        }

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        FlowEntry.Builder flowEntry = DefaultFlowEntry.builder();

        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);
        if (rule.srcIp() != null) {
            selectorBuilder.matchIPSrc(rule.srcIp());
            if (rule.dstIp() != null) {
                selectorBuilder.matchIPDst(rule.dstIp());
            }
        } else {
            selectorBuilder.matchIPDst(rule.dstIp());
        }
        if (rule.ipProto() != 0) {
            selectorBuilder.matchIPProtocol(Integer.valueOf(rule.ipProto()).byteValue());
        }
        if (rule.dstTpPort() != 0) {
            switch (rule.ipProto()) {
                case IPv4.PROTOCOL_TCP:
                    selectorBuilder.matchTcpDst(TpPort.tpPort(rule.dstTpPort()));
                    break;
                case IPv4.PROTOCOL_UDP:
                    selectorBuilder.matchUdpDst(TpPort.tpPort(rule.dstTpPort()));
                    break;
                default:
                    break;
            }
        }
        if (rule.action() == AclRule.Action.ALLOW) {
            treatment.add(Instructions.createOutput(PortNumber.CONTROLLER));
        }
        flowEntry.forDevice(deviceId);
        flowEntry.withPriority(aclStore.getPriorityByDevice(deviceId));
        flowEntry.withSelector(selectorBuilder.build());
        flowEntry.withTreatment(treatment.build());
        flowEntry.fromApp(appId);
        flowEntry.makePermanent();
        // install flow rule
        flowRuleService.applyFlowRules(flowEntry.build());
        log.debug("ACL flow rule {} is installed in {}.", flowEntry.build(), deviceId);
        aclStore.addRuleToFlowMapping(rule.id(), flowEntry.build());
        aclStore.addRuleToDeviceMapping(rule.id(), deviceId);
    }

    @Override
    public void removeAclRule(RuleId ruleId) {
        aclStore.removeAclRule(ruleId);
        log.info("ACL rule(id:{}) is removed.", ruleId);
        enforceRuleRemoving(ruleId);
    }

    /**
     * Enforces removing an existing ACL rule.
     */
    private void enforceRuleRemoving(RuleId ruleId) {
        Set<FlowRule> flowSet = aclStore.getFlowByRule(ruleId);
        if (flowSet != null) {
            for (FlowRule flowRule : flowSet) {
                flowRuleService.removeFlowRules(flowRule);
                log.debug("ACL flow rule {} is removed from {}.", flowRule.toString(), flowRule.deviceId().toString());
            }
        }
        aclStore.removeRuleToFlowMapping(ruleId);
        aclStore.removeRuleToDeviceMapping(ruleId);
        aclStore.removeDenyToAllowMapping(ruleId);
    }

    @Override
    public void clearAcl() {
        aclStore.clearAcl();
        flowRuleService.removeFlowRulesById(appId);
        log.info("ACL is cleared.");
    }

}
