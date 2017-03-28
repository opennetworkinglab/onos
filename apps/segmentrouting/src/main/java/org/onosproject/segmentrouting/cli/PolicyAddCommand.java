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
 */
package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.Policy;
import org.onosproject.segmentrouting.PolicyHandler;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.TunnelPolicy;

/**
 * Command to add a new policy.
 */
@Command(scope = "onos", name = "sr-policy-add",
        description = "Create a new policy")
public class PolicyAddCommand extends AbstractShellCommand {

    // TODO: Need to support skipping some parameters

    @Argument(index = 0, name = "ID",
            description = "policy ID",
            required = true, multiValued = false)
    String policyId;

    @Argument(index = 1, name = "priority",
            description = "priority",
            required = true, multiValued = false)
    int priority;

    @Argument(index = 2, name = "src_IP",
            description = "src IP",
            required = false, multiValued = false)
    String srcIp;

    @Argument(index = 3, name = "src_port",
            description = "src port",
            required = false, multiValued = false)
    short srcPort;

    @Argument(index = 4, name = "dst_IP",
            description = "dst IP",
            required = false, multiValued = false)
    String dstIp;

    @Argument(index = 5, name = "dst_port",
            description = "dst port",
            required = false, multiValued = false)
    short dstPort;

    @Argument(index = 6, name = "proto",
            description = "IP protocol",
            required = false, multiValued = false)
    String proto;

    @Argument(index = 7, name = "policy_type",
            description = "policy type",
            required = true, multiValued = false)
    String policyType;

    @Argument(index = 8, name = "tunnel_ID",
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
        if (srcPort != 0) {
            tpb.setSrcPort(srcPort);
        }
        if (dstPort != 0) {
            tpb.setDstPort(dstPort);
        }
        if (!"ip".equals(proto)) {
            tpb.setIpProto(proto);
        }
        if (Policy.Type.valueOf(policyType) == Policy.Type.TUNNEL_FLOW) {
            if (tunnelId == null) {
                error("tunnel ID must be specified for TUNNEL_FLOW policy");
                return;
            }
            tpb.setTunnelId(tunnelId);
        }
        PolicyHandler.Result result = srService.createPolicy(tpb.build());

        switch (result) {
            case POLICY_EXISTS:
                error("the same policy exists");
                break;
            case ID_EXISTS:
                error("the same policy ID exists");
                break;
            case TUNNEL_NOT_FOUND:
                error("the tunnel is not found");
                break;
            case UNSUPPORTED_TYPE:
                error("the policy type specified is not supported");
                break;
            default:
                break;
        }

    }
}
