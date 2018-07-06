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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
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

import static org.onosproject.openstacknetworking.api.Constants.DHCP_ARP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FORCED_ACL_RULE;

@Command(scope = "onos", name = "openstack-remove-acl",
        description = "Add acl rules to VM")
public class OpenstackRemoveAclCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "src ip", description = "src ip address", required = true)
    private String dstIp = null;

    @Argument(index = 1, name = "dst port", description = "dst port", required = true)
    private int portNumber = 0;

    @Override
    protected void execute() {

        OpenstackFlowRuleService flowRuleService = AbstractShellCommand.get(OpenstackFlowRuleService.class);
        CoreService coreService = AbstractShellCommand.get(CoreService.class);

        ApplicationId appId = coreService.getAppId(OPENSTACK_NETWORKING_APP_ID);

        InstancePortService instancePortService = AbstractShellCommand.get(InstancePortService.class);

        try {
            IpAddress dstIpAddress = IpAddress.valueOf(
                    IpAddress.Version.INET, Ip4Address.valueOf(dstIp).toOctets());

            log.info("Allow the packet again from srcIp: {}, dstPort: {}", dstIpAddress.toString(), portNumber);

            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchIPSrc(dstIpAddress.toIpPrefix())
                    .matchTcpDst(TpPort.tpPort(portNumber))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                    drop().build();

            Optional<InstancePort> instancePort = instancePortService.instancePorts().stream()
                    .filter(port -> port.ipAddress().toString().equals(dstIpAddress.toString()))
                    .findAny();

            if (!instancePort.isPresent()) {
                log.info("Instance port that matches with the given ip address isn't present {}");
                return;
            }

            flowRuleService.setRule(
                    appId,
                    instancePort.get().deviceId(),
                    selector,
                    treatment,
                    PRIORITY_FORCED_ACL_RULE,
                    DHCP_ARP_TABLE,
                    false);
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException occurred because of {}", e.toString());
        }
    }
}
