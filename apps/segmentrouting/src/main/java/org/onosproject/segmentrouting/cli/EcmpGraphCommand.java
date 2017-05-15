/*
 * Copyright 2016-present Open Networking Laboratory
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


import java.util.Map;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.EcmpShortestPathGraph;
import org.onosproject.segmentrouting.SegmentRoutingService;

/**
 * Command to read the current state of the ECMP shortest-path graph.
 *
 */
@Command(scope = "onos", name = "sr-ecmp-spg",
        description = "Displays the current ecmp shortest-path-graph in this "
                + "controller instance")
public class EcmpGraphCommand extends AbstractShellCommand {

    private static final String FORMAT_MAPPING = "  %s";

    @Override
    protected void execute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        printEcmpGraph(srService.getCurrentEcmpSpg());
    }

    private void printEcmpGraph(Map<DeviceId, EcmpShortestPathGraph> currentEcmpSpg) {
        if (currentEcmpSpg == null) {
            print(FORMAT_MAPPING, "No ECMP graph found");
            return;
        }
        StringBuilder ecmp = new StringBuilder();
        currentEcmpSpg.forEach((key, value) -> {
            ecmp.append("\n\nRoot Device: " + key + " ECMP Paths: " + value);
        });
        print(FORMAT_MAPPING, ecmp.toString());
    }
}
