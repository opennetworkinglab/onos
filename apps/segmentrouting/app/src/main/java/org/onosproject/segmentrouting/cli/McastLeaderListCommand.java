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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.NodeId;
import org.onosproject.mcast.cli.McastGroupCompleter;
import org.onosproject.segmentrouting.SegmentRoutingService;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Command to show the mcast leaders of the groups.
 */
@Command(scope = "onos", name = "sr-mcast-leader",
        description = "Lists all mcast leaders")
public class McastLeaderListCommand extends AbstractShellCommand {

    // OSGi workaround to introduce package dependency
    McastGroupCompleter completer;

    // Format for group line
    private static final String G_FORMAT_MAPPING = "group=%s, leader=%s";

    @Option(name = "-gAddr", aliases = "--groupAddress",
            description = "IP Address of the multicast group",
            valueToShowInHelp = "224.0.0.0",
            required = false, multiValued = false)
    String gAddr = null;

    @Override
    protected void execute() {
        // Verify mcast group
        IpAddress mcastGroup = null;
        if (!isNullOrEmpty(gAddr)) {
            mcastGroup = IpAddress.valueOf(gAddr);
        }
        // Get SR service
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        // Get the mapping
        Map<IpAddress, NodeId> keyToRole = srService.getMcastLeaders(mcastGroup);
        // And print local cache
        keyToRole.forEach(this::printMcastLeder);
    }

    private void printMcastLeder(IpAddress mcastGroup,
                                 NodeId nodeId) {
        print(G_FORMAT_MAPPING, mcastGroup, nodeId);
    }

}