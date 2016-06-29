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

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackSecurityGroup;
import org.onosproject.openstackinterface.OpenstackSecurityGroupRule;
import org.onosproject.openstacknetworking.OpenstackPortInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Populates flows rules for Security Groups of VMs.
 *
 */
public class OpenstackSecurityGroupRulePopulator {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackSecurityGroupRulePopulator.class);

    private OpenstackInterfaceService openstackService;
    private FlowObjectiveService flowObjectiveService;

    private ApplicationId appId;

    private static final String PROTO_ICMP = "ICMP";
    private static final String PROTO_TCP = "TCP";
    private static final String PROTO_UDP = "UDP";

    private static final String ETHTYPE_IPV4 = "IPV4";

    private static final IpPrefix IP_PREFIX_ANY = Ip4Prefix.valueOf("0.0.0.0/0");

    private static final int ACL_RULE_PRIORITY = 30000;

    /**
     * Constructor.
     *
     * @param appId application ID
     * @param openstackService OpenStack interface service
     * @param flowObjectiveService flow objective service
     */
    public OpenstackSecurityGroupRulePopulator(ApplicationId appId, OpenstackInterfaceService openstackService,
                                               FlowObjectiveService flowObjectiveService) {
        this.appId = appId;
        this.openstackService = openstackService;
        this.flowObjectiveService = flowObjectiveService;
    }

    /**
     * Populates flow rules for security groups.
     *
     * @param id Device ID
     * @param sgId Security Group ID
     * @param vmIp VM IP address
     * @param portInfoMap Port Info map
     */
    public void populateSecurityGroupRules(DeviceId id, String sgId, Ip4Address vmIp,
                                           Map<String, OpenstackPortInfo> portInfoMap) {
        OpenstackSecurityGroup securityGroup = openstackService.securityGroup(sgId);
        if (securityGroup != null) {
            securityGroup.rules().stream().forEach(sgRule -> {
                if (sgRule.remoteGroupId() != null && !sgRule.remoteGroupId().equals("null")) {
                    openstackService.ports().stream()
                        .filter(port -> port.securityGroups().contains(sgRule.remoteGroupId()))
                        .flatMap(port -> port.fixedIps().values().stream())
                        .forEach(remoteIp -> setSecurityGroupRule(id, sgRule,
                                vmIp, IpPrefix.valueOf((IpAddress) remoteIp, 32)));
                } else {
                    setSecurityGroupRule(id, sgRule, vmIp, sgRule.remoteIpPrefix());
                }
            });

            openstackService.ports().stream().forEach(osPort ->
                osPort.securityGroups().stream().forEach(remoteVmSgId -> {
                    OpenstackSecurityGroup remoteVmSg = openstackService.securityGroup(remoteVmSgId);
                    remoteVmSg.rules().stream()
                        .filter(remoteVmSgRule -> remoteVmSgRule.remoteGroupId().equals(sgId))
                        .forEach(remoteVmSgRule -> {
                            Ip4Address remoteVmIp =
                                    (Ip4Address) osPort.fixedIps().values().stream().findAny().orElse(null);
                            OpenstackPortInfo osPortInfo = portInfoMap.get(OpenstackSwitchingManager.PORTNAME_PREFIX_VM
                                    + osPort.id().substring(0, 11));
                            if (osPortInfo != null && remoteVmIp != null) {
                                setSecurityGroupRule(osPortInfo.deviceId(), remoteVmSgRule, remoteVmIp,
                                        IpPrefix.valueOf(vmIp, 32));
                            }
                        });
                }));
        }
    }

    /**
     * Removes flow rules for security groups.
     *
     * @param id Device ID
     * @param sgId Security Group ID to remove
     * @param vmIp VM IP address
     * @param portInfoMap port info map
     * @param securityGroupMap security group info map
     */
    public void removeSecurityGroupRules(DeviceId id, String sgId, Ip4Address vmIp,
                                         Map<String, OpenstackPortInfo> portInfoMap,
                                         Map<String, OpenstackSecurityGroup> securityGroupMap) {
        OpenstackSecurityGroup securityGroup = securityGroupMap.get(sgId);
        if (securityGroup != null) {
            securityGroup.rules().stream().forEach(sgRule -> {
                if (sgRule.remoteGroupId() != null && !sgRule.remoteGroupId().equals("null")) {
                    portInfoMap.values().stream()
                            .filter(portInfo -> portInfo.securityGroups().contains(sgRule.remoteGroupId()))
                            .map(OpenstackPortInfo::ip)
                            .forEach(remoteIp -> {
                                removeSecurityGroupRule(id, sgRule, vmIp, IpPrefix.valueOf(remoteIp, 32));
                            });
                } else {
                    removeSecurityGroupRule(id, sgRule, vmIp, sgRule.remoteIpPrefix());
                }
            });

            portInfoMap.values().stream()
                .forEach(portInfo -> portInfo.securityGroups()
                    .forEach(remoteVmSgId -> {
                        OpenstackSecurityGroup remoteVmSg = securityGroupMap.get(remoteVmSgId);
                        remoteVmSg.rules().stream()
                            .filter(remoteVmSgRule -> remoteVmSgRule.remoteGroupId().equals(sgId))
                            .forEach(remoteVmSgRule -> removeSecurityGroupRule(portInfo.deviceId(),
                                    remoteVmSgRule, portInfo.ip(), IpPrefix.valueOf(vmIp, 32)));
                    }));
        }
    }

    private void setSecurityGroupRule(DeviceId id, OpenstackSecurityGroupRule sgRule,
                                      Ip4Address vmIp, IpPrefix remoteIp) {
        ForwardingObjective.Builder foBuilder = buildFlowObjective(id, sgRule, vmIp, remoteIp);
        if (foBuilder != null) {
            flowObjectiveService.forward(id, foBuilder.add());
        }
    }

    private void removeSecurityGroupRule(DeviceId id, OpenstackSecurityGroupRule sgRule,
                                      Ip4Address vmIp, IpPrefix remoteIp) {
        ForwardingObjective.Builder foBuilder = buildFlowObjective(id, sgRule, vmIp, remoteIp);
        if (foBuilder != null) {
            flowObjectiveService.forward(id, foBuilder.remove());
        }
    }

    ForwardingObjective.Builder buildFlowObjective(DeviceId id, OpenstackSecurityGroupRule sgRule,
                                           Ip4Address vmIp, IpPrefix remoteIp) {
        if (remoteIp != null && remoteIp.equals(IpPrefix.valueOf(vmIp, 32))) {
            return null;
        }
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        buildMatchs(sBuilder, sgRule, vmIp, remoteIp);

        ForwardingObjective.Builder foBuilder = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(ACL_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        return foBuilder;
    }

    private void buildMatchs(TrafficSelector.Builder sBuilder, OpenstackSecurityGroupRule sgRule,
                             Ip4Address vmIp, IpPrefix remoteIp) {
        buildMatchEthType(sBuilder, sgRule.ethertype());
        buildMatchDirection(sBuilder, sgRule.direction(), vmIp);
        buildMatchProto(sBuilder, sgRule.protocol());
        buildMatchPort(sBuilder, sgRule.protocol(), sgRule.direction(), sgRule.portRangeMax(), sgRule.portRangeMin());
        buildMatchRemoteIp(sBuilder, remoteIp, sgRule.direction());
    }

    private void buildMatchDirection(TrafficSelector.Builder sBuilder,
                                     OpenstackSecurityGroupRule.Direction direction, Ip4Address vmIp) {
        if (direction.equals(OpenstackSecurityGroupRule.Direction.EGRESS)) {
            sBuilder.matchIPSrc(IpPrefix.valueOf(vmIp, 32));
        } else {
            sBuilder.matchIPDst(IpPrefix.valueOf(vmIp, 32));
        }
    }

    private void buildMatchEthType(TrafficSelector.Builder sBuilder, String ethertype) {
        // Either IpSrc or IpDst (or both) is set by default, and we need to set EthType as IPv4.
        sBuilder.matchEthType(Ethernet.TYPE_IPV4);
        if (ethertype != null && ethertype != "null" &&
                !ethertype.toUpperCase().equals(ETHTYPE_IPV4)) {
            log.error("EthType {} is not supported yet in Security Group", ethertype);
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
                    sBuilder.matchTcpDst(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchTcpSrc(TpPort.tpPort(portMax));
                }
            } else if (protocol.toUpperCase().equals(PROTO_UDP)) {
                if (direction.equals(OpenstackSecurityGroupRule.Direction.EGRESS)) {
                    sBuilder.matchUdpDst(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchUdpSrc(TpPort.tpPort(portMax));
                }
            }
        }
    }
}
