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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;

import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getVtapTypeFromString;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.makeVtapCriterion;

/**
 * Adds a openstack vtap rule.
 */
@Service
@Command(scope = "onos", name = "openstack-vtap-add",
        description = "OpenstackVtap activate")
public class OpenstackVtapAddCommand extends AbstractShellCommand {

    private final OpenstackVtapAdminService vtapService =
                                            get(OpenstackVtapAdminService.class);

    @Argument(index = 0, name = "srcIp",
            description = "source IP address CIDR (e.g., \"10.1.0.2/32\")",
            required = true, multiValued = false)
    @Completion(VmIpCompleter.class)
    String srcIp = "";

    @Argument(index = 1, name = "dstIp",
            description = "destination IP address CIDR (e.g., \"10.1.0.3/32\")",
            required = true, multiValued = false)
    @Completion(VmIpCompleter.class)
    String dstIp = "";

    @Argument(index = 2, name = "ipProto",
            description = "IP protocol [any|tcp|udp|icmp]",
            required = false, multiValued = false)
    @Completion(ProtocolTypeCompleter.class)
    String ipProto = "any";

    @Argument(index = 3, name = "srcTpPort",
            description = "source transport layer port (0 is skip)",
            required = false, multiValued = false)
    int srcTpPort = 0;

    @Argument(index = 4, name = "dstTpPort",
            description = "destination transport layer port (0 is skip)",
            required = false, multiValued = false)
    int dstTpPort = 0;

    @Argument(index = 5, name = "type",
            description = "vtap type [all|rx|tx]",
            required = false, multiValued = false)
    @Completion(VtapTypeCompleter.class)
    String vtapTypeStr = "all";

    @Override
    protected void doExecute() {
        OpenstackVtapCriterion criterion =
                makeVtapCriterion(srcIp, dstIp, ipProto, srcTpPort, dstTpPort);
        OpenstackVtap.Type type = getVtapTypeFromString(vtapTypeStr);
        if (type == null) {
            print("Invalid vtap type");
            return;
        }

        OpenstackVtap vtap = vtapService.createVtap(type, criterion);
        if (vtap != null) {
            print("Created OpenstackVtap with id { %s }", vtap.id().toString());
        } else {
            print("Failed to create OpenstackVtap");
        }
    }
}
