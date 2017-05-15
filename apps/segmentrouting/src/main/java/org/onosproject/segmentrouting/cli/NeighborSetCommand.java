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
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.storekey.NeighborSetNextObjectiveStoreKey;

/**
 * Command to read the current state of the neighborSetNextObjectiveStore.
 *
 */
@Command(scope = "onos", name = "sr-ns-objstore",
        description = "Displays the current neighborSet seen by each switch "
                + "in the network and the next-id it maps to")
public class NeighborSetCommand extends AbstractShellCommand {

    private static final String FORMAT_MAPPING = "  %s";

    @Override
    protected void execute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        printNeighborSet(srService.getNeighborSet());
    }

    private void printNeighborSet(Map<NeighborSetNextObjectiveStoreKey, Integer> ns) {
        StringBuilder nsbldr = new StringBuilder();
        ns.entrySet().forEach(entry -> {
            nsbldr.append("\n " + entry.getKey());
            nsbldr.append(" --> NextId: " + entry.getValue());
        });
        print(FORMAT_MAPPING, nsbldr.toString());
    }
}
