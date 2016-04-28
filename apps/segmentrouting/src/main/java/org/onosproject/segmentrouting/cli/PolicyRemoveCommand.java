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
import org.onosproject.segmentrouting.PolicyHandler;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.TunnelPolicy;

/**
 * Command to remove a policy.
 */
@Command(scope = "onos", name = "sr-policy-remove",
        description = "Remove a policy")
public class PolicyRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "policy ID",
            description = "policy ID",
            required = true, multiValued = false)
    String policyId;

    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        TunnelPolicy.Builder tpb = TunnelPolicy.builder().setPolicyId(policyId);
        PolicyHandler.Result result = srService.removePolicy(tpb.build());
        if (result == PolicyHandler.Result.POLICY_NOT_FOUND) {
            print("ERROR: the policy is not found");
        }
    }
}