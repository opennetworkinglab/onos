/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.Policy;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.TunnelPolicy;

/**
 * Command to add a new policy.
 */
@Command(scope = "onos", name = "srpolicy-add",
        description = "Create a new policy")
public class PolicyAddCommand extends AbstractShellCommand {

    // TODO: Need to support skipping some parameters

    @Argument(index = 0, name = "policy ID",
            description = "policy ID",
            required = true, multiValued = false)
    String policyId;

    @Argument(index = 1, name = "priority",
            description = "priority",
            required = true, multiValued = false)
    int priority;

    @Argument(index = 2, name = "src IP",
            description = "src IP",
            required = false, multiValued = false)
    String srcIp;

    @Argument(index = 3, name = "dst IP",
            description = "dst IP",
            required = false, multiValued = false)
    String dstIp;

    @Argument(index = 4, name = "policy type",
            description = "policy type",
            required = true, multiValued = false)
    String policyType;

    @Argument(index = 5, name = "tunnel ID",
            description = "tunnel ID",
            required = false, multiValued = false)
    String tunnelId;

    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        TunnelPolicy.Builder tpb = TunnelPolicy.builder().setPolicyId(policyId);
        tpb.setPriority(priority);
        tpb.setType(Policy.Type.valueOf(policyType));

        if (srcIp != null) {
            tpb.setSrcIp(srcIp);
        }
        if (dstIp != null) {
            tpb.setDstIp(dstIp);
        }
        if (Policy.Type.valueOf(policyType) == Policy.Type.TUNNEL_FLOW) {
            if (tunnelId == null) {
                // TODO: handle errors
                return;
            }
            tpb.setTunnelId(tunnelId);
        }
        srService.createPolicy(tpb.build());
    }
}
