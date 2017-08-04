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

package org.onosproject.net.region;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Service for interacting with inventory of network control regions.
 */
public interface RegionAdminService extends RegionService {

    /**
     * Creates a new region using the supplied data.
     *
     * @param regionId      region identifier
     * @param name          friendly name
     * @param type          region type
     * @param masterNodeIds list of sets of master nodes; null implies empty list
     * @return new region descriptor
     * @throws IllegalArgumentException if region already exists
     */
    Region createRegion(RegionId regionId, String name, Region.Type type,
                        List<Set<NodeId>> masterNodeIds);

    /**
     * Update the specified region using the new set of data.
     *
     * @param regionId      region identifier
     * @param name          friendly name
     * @param type          region type
     * @param masterNodeIds list of sets of master nodes; null implies empty list
     * @return new region descriptor
     */
    Region updateRegion(RegionId regionId, String name, Region.Type type,
                        List<Set<NodeId>> masterNodeIds);

    /**
     * Removes the specified region using the new set of data.
     *
     * @param regionId region identifier
     */
    void removeRegion(RegionId regionId);

    /**
     * Adds the specified collection of devices to the region.
     *
     * @param regionId  region identifier
     * @param deviceIds list of device identifiers
     */
    void addDevices(RegionId regionId, Collection<DeviceId> deviceIds);

    /**
     * Removes the specified collection of devices from the region.
     *
     * @param regionId  region identifier
     * @param deviceIds list of device identifiers
     */
    void removeDevices(RegionId regionId, Collection<DeviceId> deviceIds);

}
