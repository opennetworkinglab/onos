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

import com.google.common.collect.Maps;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mcast.cli.McastGroupCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.mcast.McastStoreKey;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Command to show the list of mcast nextids.
 */
@Command(scope = "onos", name = "sr-mcast-next",
        description = "Lists all mcast nextids")
public class McastNextListCommand extends AbstractShellCommand {

    // OSGi workaround to introduce package dependency
    McastGroupCompleter completer;

    // Format for group line
    private static final String FORMAT_MAPPING = "group=%s, deviceIds-nextIds=%s";

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
        Map<McastStoreKey, Integer> keyToNextId = srService.getMcastNextIds(mcastGroup);
        // Reduce to the set of mcast groups
        Set<IpAddress> mcastGroups = keyToNextId.keySet().stream()
                .map(McastStoreKey::mcastIp)
                .collect(Collectors.toSet());
        // Print the nextids for each group
        mcastGroups.forEach(group -> {
            // Create a new map for the group
            Map<Pair<DeviceId, VlanId>, Integer> deviceIdNextMap = Maps.newHashMap();
            keyToNextId.entrySet()
                    .stream()
                    // Filter only the elements related to this group
                    .filter(entry -> entry.getKey().mcastIp().equals(group))
                    // For each create a new entry in the group related map
                    .forEach(entry -> deviceIdNextMap.put(Pair.of(entry.getKey().deviceId(),
                                                          entry.getKey().vlanId()), entry.getValue()));
            // Print the map
            printMcastNext(group, deviceIdNextMap);
        });
    }

    private void printMcastNext(IpAddress mcastGroup, Map<Pair<DeviceId, VlanId>, Integer> deviceIdNextMap) {
        print(FORMAT_MAPPING, mcastGroup, deviceIdNextMap);
    }
}
