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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.utils.Comparators;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionService;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * List Region details including membership.
 */
@Command(scope = "onos", name = "regions",
        description = "List Region details including membership")
public class RegionListCommand extends AbstractShellCommand {

    private static final String FMT = "id=%s, name=%s, type=%s";
    private static final String FMT_MASTER = "  master=%s";

    @Argument(index = 0, name = "id", description = "Region ID",
            required = false, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        RegionService regionService = get(RegionService.class);

        if (id == null) {
            for (Region region : getSortedRegions(regionService)) {
                printRegion(region);
            }
        } else {
            Region region = regionService.getRegion(RegionId.regionId(id));

            if (region == null) {
                error("No such region %s", id);
            } else {
                printRegion(region);
            }
        }
    }

    /**
     * Returns the list of regions sorted using the region identifier.
     *
     * @param service region service
     * @return sorted region list
     */
    protected List<Region> getSortedRegions(RegionService service) {
        List<Region> regions = newArrayList(service.getRegions());
        Collections.sort(regions, Comparators.REGION_COMPARATOR);
        return regions;
    }

    private void printRegion(Region region) {
        print(FMT, region.id(), region.name(), region.type());
        region.masters().forEach(m -> print(FMT_MASTER, m));
    }
}
