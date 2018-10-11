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
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicRegionConfig;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionId;

import java.util.List;
import java.util.Set;

/**
 * Add a new region.
 */
@Service
@Command(scope = "onos", name = "region-add",
        description = "Adds a new region.")
public class RegionAddCommand extends AbstractShellCommand {

    private static final String E_BAD_LOC_TYPE = "locType must be {geo|grid}";

    private static final String GEO = "geo";
    private static final String GRID = "grid";
    private static final String SLASH = "/";

    private static final BiMap<String, Region.Type> REGION_TYPE_MAP = HashBiMap.create();

    static {
        for (Region.Type t : Region.Type.values()) {
            REGION_TYPE_MAP.put(t.name(), t);
        }
    }

    @Argument(index = 0, name = "id", description = "Region ID",
            required = true, multiValued = false)
    String id = null;

    @Argument(index = 1, name = "name", description = "Region Name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 2, name = "type", description = "Region Type (CONTINENT|" +
            "COUNTRY|METRO|CAMPUS|BUILDING|DATA_CENTER|FLOOR|ROOM|RACK|LOGICAL_GROUP)",
            required = true, multiValued = false)
    @Completion(RegionTypeCompleter.class)
    String type = null;

    @Argument(index = 3, name = "latOrY",
            description = "Geo latitude / Grid y-coord",
            required = true, multiValued = false)
    Double latOrY = null;

    @Argument(index = 4, name = "longOrX",
            description = "Geo longitude / Grid x-coord",
            required = true, multiValued = false)
    Double longOrX = null;

    @Argument(index = 5, name = "locType", description = "Location type {geo|grid}",
            required = false, multiValued = false)
    String locType = GEO;

    @Argument(index = 6, name = "masters", description = "Region Master, a set " +
            "of nodeIds should be split with '/' delimiter (e.g., 1 2 3 / 4 5 6)",
            required = true, multiValued = true)
    @Completion(NodeIdCompleter.class)
    List<String> masterArgs = null;

    @Override
    protected void doExecute() {
        RegionAdminService service = get(RegionAdminService.class);
        RegionId regionId = RegionId.regionId(id);

        NetworkConfigService cfgService = get(NetworkConfigService.class);
        BasicRegionConfig cfg = cfgService.addConfig(regionId, BasicRegionConfig.class);
        setConfigurationData(cfg);

        List<Set<NodeId>> masters = parseMasterArgs();
        service.createRegion(regionId, name, REGION_TYPE_MAP.get(type), masters);
        print("Region successfully added.");
    }

    private void setConfigurationData(BasicRegionConfig cfg) {
        cfg.name(name).locType(locType);

        if (GEO.equals(locType)) {
            cfg.latitude(latOrY).longitude(longOrX);

        } else if (GRID.equals(locType)) {
            cfg.gridY(latOrY).gridX(longOrX);

        } else {
            throw new IllegalArgumentException(E_BAD_LOC_TYPE);

        }

        cfg.apply();
    }

    private List<Set<NodeId>> parseMasterArgs() {
        List<Set<NodeId>> masters = Lists.newArrayList();
        Set<NodeId> nodeIds = Sets.newHashSet();
        for (String masterArg : masterArgs) {
            if (masterArg.equals(SLASH)) {
                masters.add(nodeIds);
                nodeIds = Sets.newHashSet();
            } else {
                nodeIds.add(NodeId.nodeId(masterArg));
            }
        }
        masters.add(nodeIds);
        return masters;
    }
}