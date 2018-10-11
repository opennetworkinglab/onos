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
package org.onosproject.cli.net;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.NodeIdCompleter;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionService;

import java.util.List;
import java.util.Set;

/**
 * Update an existing region.
 */
@Service
@Command(scope = "onos", name = "region-update",
        description = "Updates an existing region.")
public class RegionUpdateCommand extends AbstractShellCommand {

    private static final BiMap<String, Region.Type> REGION_TYPE_MAP = HashBiMap.create();

    static {
        for (Region.Type t : Region.Type.values()) {
            REGION_TYPE_MAP.put(t.name(), t);
        }
    }

    @Argument(index = 0, name = "id", description = "Region ID",
            required = true, multiValued = false)
    @Completion(RegionIdCompleter.class)
    String id = null;

    @Argument(index = 1, name = "name", description = "Region Name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 2, name = "type", description = "Region Type (CONTINENT|" +
            "COUNTRY|METRO|CAMPUS|BUILDING|FLOOR|ROOM|RACK|LOGICAL_GROUP)",
            required = true, multiValued = false)
    @Completion(RegionTypeCompleter.class)
    String type = null;

    @Argument(index = 3, name = "masters", description = "Region Master, a set " +
    "of nodeIds should be split with '/' delimiter (e.g., 1 2 3 / 4 5 6)",
            required = true, multiValued = true)
    @Completion(NodeIdCompleter.class)
    List<String> masterArgs = null;

    @Override
    protected void doExecute() {
        RegionService regionService = get(RegionService.class);
        RegionAdminService regionAdminService = get(RegionAdminService.class);
        RegionId regionId = RegionId.regionId(id);

        if (regionService.getRegion(regionId) == null) {
            print("The region with id %s does not exist.", regionId);
            return;
        }

        List<Set<NodeId>> masters = Lists.newArrayList();
        Set<NodeId> nodeIds = Sets.newHashSet();
        for (String masterArg : masterArgs) {
            if ("/".equals(masterArg)) {
                masters.add(nodeIds);
                nodeIds = Sets.newHashSet();
            } else {
                nodeIds.add(NodeId.nodeId(masterArg));
            }
        }
        masters.add(nodeIds);

        regionAdminService.updateRegion(regionId, name, REGION_TYPE_MAP.get(type), masters);
        print("Region with id %s is successfully updated.", regionId);
    }
}
