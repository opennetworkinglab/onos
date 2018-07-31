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
package org.onosproject.openstacktroubleshoot.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacktroubleshoot.api.OpenstackTroubleshootService;
import org.onosproject.openstacktroubleshoot.api.Reachability;

import java.util.Map;

/**
 * Checks the east-west VMs connectivity.
 */
@Command(scope = "onos", name = "openstack-check-east-west",
        description = "Checks the east-west VMs connectivity")
public class OpenstackEastWestProbeCommand extends AbstractShellCommand {

    private static final String REACHABLE = "Reachable :)";
    private static final String UNREACHABLE = "Unreachable :(";
    private static final String ARROW = "->";

    private static final String FORMAT = "%-20s%-5s%-20s%-20s";

    @Override
    protected void execute() {
        OpenstackTroubleshootService troubleshootService =
                AbstractShellCommand.get(OpenstackTroubleshootService.class);

        if (troubleshootService == null) {
            error("Failed to troubleshoot openstack networking.");
            return;
        }

        print(FORMAT, "Source IP", "", "Destination IP", "Reachability");

        Map<String, Reachability> map = troubleshootService.probeEastWestBulk();

        map.values().forEach(r -> {
            String result = r.isReachable() ? REACHABLE : UNREACHABLE;
            print(FORMAT, r.srcIp().toString(), ARROW, r.dstIp().toString(), result);
        });
    }
}
