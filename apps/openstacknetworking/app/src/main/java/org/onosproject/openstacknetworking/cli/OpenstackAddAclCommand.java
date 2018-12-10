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
package org.onosproject.openstacknetworking.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;

import java.util.Optional;

import static org.onosproject.cli.AbstractShellCommand.get;
import static org.onosproject.openstacknetworking.api.Constants.DHCP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FORCED_ACL_RULE;

/**
 * Adds a acl.
 */
@Service
@Command(scope = "onos", name = "openstack-add-acl",
        description = "Add acl rules to VM")
public class OpenstackAddAclCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "src ip", description = "src ip address", required = true)
    private String srcIpStr = null;

    @Argument(index = 1, name = "src ip", description = "src tcp port", required = true)
    private int srcPort = 0;

    @Argument(index = 2, name = "dst ip", description = "dst ip address", required = true)
    private String dstIpStr = null;

    @Argument(index = 3, name = "dst port", description = "dst tcp port", required = true)
    private int dstPort = 0;

    @Override
    protected void doExecute() {

        OpenstackFlowRuleService flowRuleService = get(OpenstackFlowRuleService.class);
        CoreService coreService = get(CoreService.class);

        ApplicationId appId = coreService.getAppId(OPENSTACK_NETWORKING_APP_ID);

        InstancePortService instancePortService = get(InstancePortService.class);

        IpAddress srcIpAddress;

        IpAddress dstIpAddress;

        try {
            srcIpAddress = IpAddress.valueOf(srcIpStr);

            dstIpAddress = IpAddress.valueOf(dstIpStr);
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException occurred because of {}", e);
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcIpAddress.toIpPrefix())
                .matchIPDst(dstIpAddress.toIpPrefix());

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                drop().build();

        if (srcPort != 0 || dstPort != 0) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP);
            if (srcPort != 0) {
                sBuilder.matchTcpSrc(TpPort.tpPort(srcPort));
            }

            if (dstPort != 0) {
                sBuilder.matchTcpDst(TpPort.tpPort(dstPort));
            }
        }

        log.info("Deny the packet from srcIp: {}, dstPort: {} to dstIp: {}, dstPort: {}",
                srcIpAddress.toString(),
                srcPort,
                dstIpAddress.toString(),
                dstPort);

        Optional<InstancePort> instancePort = instancePortService.instancePorts().stream()
                .filter(port -> port.ipAddress().toString().equals(dstIpStr))
                .findAny();

        if (!instancePort.isPresent()) {
            log.info("Instance port that matches with the given dst ip address isn't present {}");
            return;
        }

        flowRuleService.setRule(
                appId,
                instancePort.get().deviceId(),
                sBuilder.build(),
                treatment,
                PRIORITY_FORCED_ACL_RULE,
                DHCP_TABLE,
                true);
    }
}
