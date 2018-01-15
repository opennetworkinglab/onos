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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.McastHandler.McastRole;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.storekey.McastStoreKey;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.segmentrouting.McastHandler.McastRole.EGRESS;
import static org.onosproject.segmentrouting.McastHandler.McastRole.INGRESS;
import static org.onosproject.segmentrouting.McastHandler.McastRole.TRANSIT;

/**
 * Command to show the list of mcast trees.
 */
@Command(scope = "onos", name = "sr-mcast-tree",
        description = "Lists all mcast trees")
public class McastTreeListCommand extends AbstractShellCommand {

    // Format for group line
    private static final String G_FORMAT_MAPPING = "group=%s, ingress=%s, transit=%s, egress=%s";
    // Format for sink line
    private static final String S_FORMAT_MAPPING = "\tsink=%s\tpath=%s";

    @Argument(index = 0, name = "mcastIp", description = "mcast Ip",
            required = false, multiValued = false)
    String mcastIp;

    @Override
    protected void execute() {
        // Verify mcast group
        IpAddress mcastGroup = null;
        if (!isNullOrEmpty(mcastIp)) {
            mcastGroup = IpAddress.valueOf(mcastIp);

        }
        // Get SR service
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        // Get the mapping
        Map<McastStoreKey, McastRole> keyToRole = srService.getMcastRoles(mcastGroup);
        // Reduce to the set of mcast groups
        Set<IpAddress> mcastGroups = keyToRole.keySet().stream()
                .map(McastStoreKey::mcastIp)
                .collect(Collectors.toSet());
        // Print the trees for each group
        mcastGroups.forEach(group -> {
            // Create a new map for the group
            Multimap<McastRole, DeviceId> roleDeviceIdMap = ArrayListMultimap.create();
            keyToRole.entrySet()
                    .stream()
                    // Filter only the elements related to this group
                    .filter(entry -> entry.getKey().mcastIp().equals(group))
                    // For each create a new entry in the group related map
                    .forEach(entry -> roleDeviceIdMap.put(entry.getValue(), entry.getKey().deviceId()));
            // Print the map
            printMcastRole(group,
                           roleDeviceIdMap.get(INGRESS),
                           roleDeviceIdMap.get(TRANSIT),
                           roleDeviceIdMap.get(EGRESS)
            );
            // Get sinks paths
            Map<ConnectPoint, List<ConnectPoint>> mcastPaths = srService.getMcastPaths(group);
            // Print the paths
            mcastPaths.forEach(this::printMcastSink);
        });
    }

    private void printMcastRole(IpAddress mcastGroup,
                                Collection<DeviceId> ingress,
                                Collection<DeviceId> transit,
                                Collection<DeviceId> egress) {
        print(G_FORMAT_MAPPING, mcastGroup, ingress, transit, egress);
    }

    private void printMcastSink(ConnectPoint sink, List<ConnectPoint> path) {
        print(S_FORMAT_MAPPING, sink, path);
    }
}
