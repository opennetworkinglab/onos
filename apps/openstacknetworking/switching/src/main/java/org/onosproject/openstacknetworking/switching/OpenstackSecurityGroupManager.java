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

package org.onosproject.openstacknetworking.switching;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackSecurityGroup;
import org.onosproject.openstackinterface.OpenstackSecurityGroupRule;
import org.onosproject.openstacknetworking.OpenstackSecurityGroupService;
import org.onosproject.openstacknetworking.AbstractVmHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.Constants.*;

/**
 * Populates flows rules for Security Groups of VMs.
 *
 */
@Component(immediate = true)
@Service
public class OpenstackSecurityGroupManager extends AbstractVmHandler
        implements OpenstackSecurityGroupService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    private static final String PROTO_ICMP = "ICMP";
    private static final String PROTO_TCP = "TCP";
    private static final String PROTO_UDP = "UDP";
    private static final String ETHTYPE_IPV4 = "IPV4";

    private final Map<Host, Set<SecurityGroupRule>> securityGroupRuleMap = Maps.newConcurrentMap();
    private ApplicationId appId;

    @Activate
    protected void activate() {
        super.activate();
        appId = coreService.registerApplication(SWITCHING_APP_ID);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void updateSecurityGroup(OpenstackPort osPort) {
        if (!osPort.status().equals(OpenstackPort.PortStatus.ACTIVE)) {
            return;
        }

        Optional<Host> host = getVmByPortId(osPort.id());
        if (!host.isPresent()) {
            log.debug("No host found with {}", osPort.id());
            return;
        }
        eventExecutor.execute(() -> updateSecurityGroupRules(host.get(), true));
    }

    /**
     * Populates security group rules for all VMs in the supplied tenant ID.
     * VMs in the same tenant tend to be engaged to each other by sharing the
     * same security groups or setting the remote to another security group.
     * To make the implementation simpler and robust, it tries to reinstall
     * security group rules for all the VMs in the same tenant whenever a new
     * VM is detected or port is updated.
     *
     * @param tenantId tenant id to update security group rules
     */
    private void populateSecurityGroupRules(String tenantId, boolean install) {
        securityGroupRuleMap.entrySet().stream()
                .filter(entry -> getTenantId(entry.getKey()).equals(tenantId))
                .forEach(entry -> {
                    Host local = entry.getKey();
                    entry.getValue().forEach(sgRule -> {
                        setSecurityGroupRule(local.location().deviceId(),
                                sgRule.rule(),
                                getIp(local),
                                sgRule.remoteIp(), install);
                    });
                });
        log.debug("Updated security group rules for {}", tenantId);
    }

    private void setSecurityGroupRule(DeviceId deviceId, OpenstackSecurityGroupRule sgRule,
                                      Ip4Address vmIp, IpPrefix remoteIp,
                                      boolean install) {
        ForwardingObjective.Builder foBuilder = buildFlowObjective(sgRule, vmIp, remoteIp);
        if (foBuilder == null) {
            return;
        }

        if (install) {
            flowObjectiveService.forward(deviceId, foBuilder.add());
        } else {
            flowObjectiveService.forward(deviceId, foBuilder.remove());
        }
    }

    private ForwardingObjective.Builder buildFlowObjective(OpenstackSecurityGroupRule sgRule,
                                                           Ip4Address vmIp,
                                                           IpPrefix remoteIp) {
        if (remoteIp != null && remoteIp.equals(IpPrefix.valueOf(vmIp, 32))) {
            // do nothing if the remote IP is my IP
            return null;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        buildMatchs(sBuilder, sgRule, vmIp, remoteIp);

        return DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(DefaultTrafficTreatment.builder().build())
                .withPriority(ACL_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);
    }

    private void buildMatchs(TrafficSelector.Builder sBuilder, OpenstackSecurityGroupRule sgRule,
                             Ip4Address vmIp, IpPrefix remoteIp) {
        buildMatchEthType(sBuilder, sgRule.ethertype());
        buildMatchDirection(sBuilder, sgRule.direction(), vmIp);
        buildMatchProto(sBuilder, sgRule.protocol());
        buildMatchPort(sBuilder, sgRule.protocol(), sgRule.direction(),
                sgRule.portRangeMax(), sgRule.portRangeMin());
        buildMatchRemoteIp(sBuilder, remoteIp, sgRule.direction());
    }

    private void buildMatchDirection(TrafficSelector.Builder sBuilder,
                                     OpenstackSecurityGroupRule.Direction direction,
                                     Ip4Address vmIp) {
        if (direction.equals(OpenstackSecurityGroupRule.Direction.EGRESS)) {
            sBuilder.matchIPSrc(IpPrefix.valueOf(vmIp, 32));
        } else {
            sBuilder.matchIPDst(IpPrefix.valueOf(vmIp, 32));
        }
    }

    private void buildMatchEthType(TrafficSelector.Builder sBuilder, String etherType) {
        // Either IpSrc or IpDst (or both) is set by default, and we need to set EthType as IPv4.
        sBuilder.matchEthType(Ethernet.TYPE_IPV4);
        if (etherType != null && !Objects.equals(etherType, "null") &&
                !etherType.toUpperCase().equals(ETHTYPE_IPV4)) {
            log.debug("EthType {} is not supported yet in Security Group", etherType);
        }
    }

    private void buildMatchRemoteIp(TrafficSelector.Builder sBuilder, IpPrefix remoteIpPrefix,
                                    OpenstackSecurityGroupRule.Direction direction) {
        if (remoteIpPrefix != null && !remoteIpPrefix.getIp4Prefix().equals(IP_PREFIX_ANY)) {
            if (direction.equals(OpenstackSecurityGroupRule.Direction.EGRESS)) {
                sBuilder.matchIPDst(remoteIpPrefix);
            } else {
                sBuilder.matchIPSrc(remoteIpPrefix);
            }
        }
    }

    private void buildMatchProto(TrafficSelector.Builder sBuilder, String protocol) {
        if (protocol != null) {
            switch (protocol.toUpperCase()) {
                case PROTO_ICMP:
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_ICMP);
                    break;
                case PROTO_TCP:
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP);
                    break;
                case PROTO_UDP:
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP);
                    break;
                default:
            }
        }
    }

    private void buildMatchPort(TrafficSelector.Builder sBuilder, String protocol,
                                OpenstackSecurityGroupRule.Direction direction,
                                int portMin, int portMax) {
        if (portMin > 0 && portMax > 0 && portMin == portMax) {
            if (protocol.toUpperCase().equals(PROTO_TCP)) {
                if (direction.equals(OpenstackSecurityGroupRule.Direction.EGRESS)) {
                    sBuilder.matchTcpSrc(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchTcpDst(TpPort.tpPort(portMax));
                }
            } else if (protocol.toUpperCase().equals(PROTO_UDP)) {
                if (direction.equals(OpenstackSecurityGroupRule.Direction.EGRESS)) {
                    sBuilder.matchUdpSrc(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchUdpDst(TpPort.tpPort(portMax));
                }
            }
        }
    }

    private void updateSecurityGroupRulesMap(Host host) {
        OpenstackPort osPort = openstackService.port(host.annotations().value(PORT_ID));
        if (osPort == null) {
            log.debug("Failed to get OpenStack port information for {}", host);
            return;
        }

        Set<SecurityGroupRule> rules = Sets.newHashSet();
        osPort.securityGroups().forEach(sgId -> {
            OpenstackSecurityGroup osSecGroup = openstackService.securityGroup(sgId);
            if (osSecGroup != null) {
                osSecGroup.rules().forEach(rule -> rules.addAll(getSgRules(rule)));
            } else {
                // TODO handle the case that the security group removed
                log.warn("Failed to get security group {}", sgId);
            }
        });
        securityGroupRuleMap.put(host, rules);
    }

    /**
     * Returns set of security group rules with individual remote IP by
     * converting remote group to actual IP address.
     *
     * @param sgRule security group rule
     * @return set of security group rules
     */
    private Set<SecurityGroupRule> getSgRules(OpenstackSecurityGroupRule sgRule) {
        Set<SecurityGroupRule> sgRules = Sets.newHashSet();
        if (sgRule.remoteGroupId() != null && !sgRule.remoteGroupId().equals("null")) {
            sgRules = getRemoteIps(sgRule.tenantId(), sgRule.remoteGroupId())
                    .stream()
                    .map(remoteIp -> new SecurityGroupRule(sgRule, remoteIp))
                    .collect(Collectors.toSet());
        } else {
            sgRules.add(new SecurityGroupRule(sgRule, sgRule.remoteIpPrefix()));
        }
        return sgRules;
    }

    /**
     * Returns a set of host IP addresses engaged with supplied security group ID.
     * It only searches a VM in the same tenant boundary.
     *
     * @param tenantId tenant id
     * @param sgId security group id
     * @return set of ip addresses in ip prefix format
     */
    private Set<IpPrefix> getRemoteIps(String tenantId, String sgId) {
        Set<IpPrefix> remoteIps = Sets.newHashSet();
        securityGroupRuleMap.entrySet().stream()
                .filter(entry -> Objects.equals(getTenantId(entry.getKey()), tenantId))
                .forEach(entry -> {
                    if (entry.getValue().stream()
                            .anyMatch(rule -> rule.rule().secuityGroupId().equals(sgId))) {
                        remoteIps.add(IpPrefix.valueOf(getIp(entry.getKey()), 32));
                    }
                });
        return remoteIps;
    }

    private void updateSecurityGroupRules(Host host, boolean isHostAdded) {
        String tenantId = getTenantId(host);
        populateSecurityGroupRules(tenantId, false);

        if (isHostAdded) {
            updateSecurityGroupRulesMap(host);
        } else {
            securityGroupRuleMap.remove(host);
        }

        Tools.stream(hostService.getHosts())
                .filter(h -> Objects.equals(getTenantId(h), getTenantId(host)))
                .forEach(this::updateSecurityGroupRulesMap);

        populateSecurityGroupRules(tenantId, true);
    }

    @Override
    protected void hostDetected(Host host) {
        updateSecurityGroupRules(host, true);
        log.info("Applied security group rules for {}", host);
    }

    @Override
    protected void hostRemoved(Host host) {
        updateSecurityGroupRules(host, false);
        log.info("Applied security group rules for {}", host);
    }

    private final class SecurityGroupRule {
        private final OpenstackSecurityGroupRule rule;
        private final IpPrefix remoteIp;

        private SecurityGroupRule(OpenstackSecurityGroupRule rule, IpPrefix remoteIp) {
            this.rule = rule;
            this.remoteIp = remoteIp;
        }

        private OpenstackSecurityGroupRule rule() {
            return rule;
        }

        private IpPrefix remoteIp() {
            return remoteIp;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof SecurityGroupRule) {
                SecurityGroupRule that = (SecurityGroupRule) obj;
                if (Objects.equals(rule, that.rule) &&
                        Objects.equals(remoteIp, that.remoteIp)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(rule, remoteIp);
        }
    }
}
