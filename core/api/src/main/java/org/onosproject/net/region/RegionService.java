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

package org.onosproject.net.region;

import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;

import java.util.Set;

/**
 * Service for interacting with inventory of network control regions.
 */
public interface RegionService extends ListenerService<RegionEvent, RegionListener> {

    /**
     * Returns set of all regions.
     *
     * @return set of regions
     */
    Set<Region> getRegions();

    /**
     * Returns the region with the specified identifier.
     *
     * @param regionId region identifier
     * @return region
     * @throws org.onlab.util.ItemNotFoundException if region with given
     *                                              id does not exist
     */
    Region getRegion(RegionId regionId);

    /**
     * Returns the region to which the specified device belongs.
     *
     * @param deviceId device identifier
     * @return region or null if device does not belong to any region
     */
    Region getRegionForDevice(DeviceId deviceId);

    /**
     * Returns the set of devices that belong to the specified region.
     *
     * @param regionId region identifier
     * @return set of identifiers for devices in the given region
     */
    Set<DeviceId> getRegionDevices(RegionId regionId);

    /**
     * Returns the set of hosts that belong to the specified region.
     *
     * @param regionId region identifier
     * @return set of identifiers for hosts in the given region
     */
    Set<HostId> getRegionHosts(RegionId regionId);

}
