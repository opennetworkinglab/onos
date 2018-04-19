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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mcast.cli.McastGroupCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.mcast.McastRole;
import org.onosproject.segmentrouting.mcast.McastRoleStoreKey;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Command to show the list of mcast roles.
 */
@Command(scope = "onos", name = "sr-mcast-role",
        description = "Lists all mcast roles")
public class McastRoleListCommand extends AbstractShellCommand {

    // OSGi workaround to introduce package dependency
    McastGroupCompleter completer;

    // Format for group line
    private static final String FORMAT_MAPPING = "%s,%s  ingress=%s\ttransit=%s\tegress=%s";

    @Option(name = "-gAddr", aliases = "--groupAddress",
            description = "IP Address of the multicast group",
            valueToShowInHelp = "224.0.0.0",
            required = false, multiValued = false)
    String gAddr = null;

    @Option(name = "-src", aliases = "--connectPoint",
            description = "Source port of:XXXXXXXXXX/XX",
            valueToShowInHelp = "of:0000000000000001/1",
            required = false, multiValued = false)
    String source = null;

    @Override
    protected void execute() {
        // Verify mcast group
        IpAddress mcastGroup = null;
        // We want to use source cp only for a specific group
        ConnectPoint sourcecp = null;
        if (!isNullOrEmpty(gAddr)) {
            mcastGroup = IpAddress.valueOf(gAddr);
            if (!isNullOrEmpty(source)) {
                sourcecp = ConnectPoint.deviceConnectPoint(source);
            }
        }
        // Get SR service, the roles and the groups
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        Map<McastRoleStoreKey, McastRole> keyToRole = srService.getMcastRoles(mcastGroup, sourcecp);
        Set<IpAddress> mcastGroups = keyToRole.keySet().stream()
                .map(McastRoleStoreKey::mcastIp)
                .collect(Collectors.toSet());
        // Print the trees for each group
        mcastGroups.forEach(group -> {
            // Create a new map for the group
            Map<ConnectPoint, Multimap<McastRole, DeviceId>> roleDeviceIdMap = Maps.newHashMap();
            keyToRole.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().mcastIp().equals(group))
                    .forEach(entry -> roleDeviceIdMap.compute(entry.getKey().source(), (gsource, map) -> {
                        map = map == null ? ArrayListMultimap.create() : map;
                        map.put(entry.getValue(), entry.getKey().deviceId());
                        return map;
                    }));
            roleDeviceIdMap.forEach((gsource, map) -> {
                // Print the map
                printMcastRole(group, gsource,
                               map.get(McastRole.INGRESS),
                               map.get(McastRole.TRANSIT),
                               map.get(McastRole.EGRESS));
            });
        });
    }

    private void printMcastRole(IpAddress mcastGroup, ConnectPoint source,
                                Collection<DeviceId> ingress,
                                Collection<DeviceId> transit,
                                Collection<DeviceId> egress) {
        print(FORMAT_MAPPING, mcastGroup, source, ingress, transit, egress);
    }
}
