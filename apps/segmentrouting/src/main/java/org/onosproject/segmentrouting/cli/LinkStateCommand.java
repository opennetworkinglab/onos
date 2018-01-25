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

package org.onosproject.segmentrouting.cli;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.SegmentRoutingService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;


/**
 * Command to read the current state of the DestinationSetNextObjectiveStore.
 *
 */
@Command(scope = "onos", name = "sr-link-state", description = "Displays the current internal link state "
        + "noted by this instance of the controller")
public class LinkStateCommand extends AbstractShellCommand {
    private static final String FORMAT_MAPPING = "  %s";

    @Override
    protected void execute() {
        SegmentRoutingService srService = AbstractShellCommand
                .get(SegmentRoutingService.class);
        printLinkState(srService.getSeenLinks(),
                       srService.getDownedPortState());
    }

    private void printLinkState(ImmutableMap<Link, Boolean> seenLinks,
                                ImmutableMap<DeviceId, Set<PortNumber>> downedPortState) {
        List<Link> a = Lists.newArrayList();
        a.addAll(seenLinks.keySet());
        a.sort(new CompLinks());

        StringBuilder slbldr = new StringBuilder();
        slbldr.append("\n Seen Links: ");
        for (int i = 0; i < a.size(); i++) {
            slbldr.append("\n "
                    + (seenLinks.get(a.get(i)) == Boolean.TRUE ? "  up : "
                                                               : "down : "));
            slbldr.append(a.get(i).src() + " --> " + a.get(i).dst());
        }
        print(FORMAT_MAPPING, slbldr.toString());

        StringBuilder dpbldr = new StringBuilder();
        dpbldr.append("\n\n Administratively Disabled Ports: ");
        downedPortState.entrySet().forEach(entry -> dpbldr
                .append("\n " + entry.getKey() + entry.getValue()));
        print(FORMAT_MAPPING, dpbldr.toString());
    }

    static class CompLinks implements Comparator<Link> {

        @Override
        public int compare(Link o1, Link o2) {
            int res = o1.src().deviceId().toString()
                    .compareTo(o2.src().deviceId().toString());
            if (res < 0) {
                return -1;
            } else if (res > 0) {
                return +1;
            }
            if (o1.src().port().toLong() < o2.src().port().toLong()) {
                return -1;
            }
            return +1;
        }

    }

}
