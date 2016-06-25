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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.Policy;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.TunnelPolicy;

/**
 * Command to show the list of policies.
 */
@Command(scope = "onos", name = "sr-policy-list",
        description = "Lists all policies")
public class PolicyListCommand extends AbstractShellCommand {

    private static final String FORMAT_MAPPING_TUNNEL =
            "  id=%s, type=%s,  prio=%d, src=%s, port=%d, dst=%s, port=%d, proto=%s, tunnel=%s";

    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        srService.getPolicies().forEach(policy -> printPolicy(policy));
    }

    private void printPolicy(Policy policy) {
        if (policy.type() == Policy.Type.TUNNEL_FLOW) {
            print(FORMAT_MAPPING_TUNNEL, policy.id(), policy.type(), policy.priority(),
                    policy.srcIp(), policy.srcPort(), policy.dstIp(), policy.dstPort(),
                    (policy.ipProto() == null) ? "" : policy.ipProto(),
                    ((TunnelPolicy) policy).tunnelId());
        }
    }
}