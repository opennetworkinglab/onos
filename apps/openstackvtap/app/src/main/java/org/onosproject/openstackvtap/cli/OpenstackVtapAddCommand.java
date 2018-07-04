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
package org.onosproject.openstackvtap.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;
import org.onosproject.openstackvtap.impl.DefaultOpenstackVtapCriterion;

import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getProtocolTypeFromString;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getVtapTypeFromString;

/**
 * Command line interface for adding openstack vTap rule.
 */
@Command(scope = "onos", name = "openstack-vtap-add",
        description = "OpenstackVtap activate")
public class OpenstackVtapAddCommand extends AbstractShellCommand {

    private final OpenstackVtapAdminService vTapService =
                                            get(OpenstackVtapAdminService.class);

    @Argument(index = 0, name = "srcIp",
            description = "source IP address CIDR (e.g., \"10.1.0.2/32\")",
            required = true, multiValued = false)
    String srcIp = "";

    @Argument(index = 1, name = "dstIp",
            description = "destination IP address CIDR (e.g., \"10.1.0.3/32\")",
            required = true, multiValued = false)
    String dstIp = "";

    @Argument(index = 2, name = "ipProto",
            description = "IP protocol [tcp|udp|icmp|none]",
            required = false, multiValued = false)
    String ipProto = "";

    @Argument(index = 3, name = "srcTpPort",
            description = "source transport layer port (0 is skip)",
            required = false, multiValued = false)
    int srcTpPort = 0;

    @Argument(index = 4, name = "dstTpPort",
            description = "destination transport layer port (0 is skip)",
            required = false, multiValued = false)
    int dstTpPort = 0;

    @Argument(index = 5, name = "type",
            description = "vTap type [all|tx|rx]",
            required = false, multiValued = false)
    String vTapTypeStr = "all";

    @Override
    protected void execute() {
        DefaultOpenstackVtapCriterion.Builder
                    defaultVtapCriterionBuilder = DefaultOpenstackVtapCriterion.builder();
        if (makeCriterion(defaultVtapCriterionBuilder)) {
            OpenstackVtap.Type type = getVtapTypeFromString(vTapTypeStr);
            if (type == null) {
                print("Invalid vTap type");
                return;
            }

            OpenstackVtap vTap = vTapService.createVtap(type, defaultVtapCriterionBuilder.build());
            if (vTap != null) {
                print("Created OpenstackVtap with id { %s }", vTap.id().toString());
            } else {
                print("Failed to create OpenstackVtap");
            }
        }
    }

    private boolean makeCriterion(DefaultOpenstackVtapCriterion.Builder vTapCriterionBuilder) {
        try {
            vTapCriterionBuilder.srcIpPrefix(IpPrefix.valueOf(srcIp));
            vTapCriterionBuilder.dstIpPrefix(IpPrefix.valueOf(dstIp));
        } catch (Exception e) {
            print("Inputted valid source IP & destination IP in CIDR (e.g., \"10.1.0.0/16\")");
            return false;
        }

        vTapCriterionBuilder.ipProtocol(getProtocolTypeFromString(ipProto.toLowerCase()));

        vTapCriterionBuilder.srcTpPort(TpPort.tpPort(srcTpPort));
        vTapCriterionBuilder.dstTpPort(TpPort.tpPort(dstTpPort));

        return true;
    }
}
