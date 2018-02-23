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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.grouphandler.NextNeighbors;
import org.onosproject.segmentrouting.storekey.DestinationSetNextObjectiveStoreKey;

/**
 * Command to read the current state of the DestinationSetNextObjectiveStore.
 *
 */
@Command(scope = "onos", name = "sr-next-hops",
        description = "Displays the current next-hops seen by each switch "
                + "towards a set of destinations and the next-id it maps to")
public class NextHopCommand extends AbstractShellCommand {

    private static final String FORMAT_MAPPING = "  %s";

    @Override
    protected void execute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        printDestinationSet(srService.getDestinationSet());
    }

    private void printDestinationSet(Map<DestinationSetNextObjectiveStoreKey,
                                      NextNeighbors> ds) {
        ArrayList<DestinationSetNextObjectiveStoreKey> a = new ArrayList<>();
        ds.keySet().forEach(key -> a.add(key));
        a.sort(new Comp());

        StringBuilder dsbldr = new StringBuilder();
        for (int i = 0; i < a.size(); i++) {
            dsbldr.append("\n " + a.get(i));
            dsbldr.append(" --> via: " + ds.get(a.get(i)));
        }
        print(FORMAT_MAPPING, dsbldr.toString());
    }

    static class Comp implements Comparator<DestinationSetNextObjectiveStoreKey> {

        @Override
        public int compare(DestinationSetNextObjectiveStoreKey o1,
                           DestinationSetNextObjectiveStoreKey o2) {
            int res = o1.deviceId().toString().compareTo(o2.deviceId().toString());
            if (res < 0) {
                return -1;
            } else if (res > 0) {
                return +1;
            }
            return 0;
        }

    }
}
