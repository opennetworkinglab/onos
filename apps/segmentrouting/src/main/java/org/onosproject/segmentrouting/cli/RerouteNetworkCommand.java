/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.segmentrouting.SegmentRoutingService;

/**
 * Command to manually trigger routing and rule-population in the network.
 *
 */
@Command(scope = "onos", name = "sr-reroute-network",
        description = "Repopulate routing rules given current network state")
public class RerouteNetworkCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        srService.rerouteNetwork();
    }

}
