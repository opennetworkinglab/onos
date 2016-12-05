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

import org.onosproject.cluster.NodeId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Manages inventory of regions of devices; not intended for direct use.
 */
public interface RegionStore extends Store<RegionEvent, RegionStoreDelegate> {

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
     * Creates a new region using the supplied data.
     *
     * @param regionId      region identifier
     * @param name          friendly name
     * @param type          region type
     * @param annots        annotations
     * @param masterNodeIds list of master nodes; null implies empty list
     * @return new region descriptor
     * @throws IllegalArgumentException if item already exists
     */
    Region createRegion(RegionId regionId, String name, Region.Type type,
                        Annotations annots, List<Set<NodeId>> masterNodeIds);

    /**
     * Updates the specified new region using the supplied data.
     *
     * @param regionId      region identifier
     * @param name          friendly name
     * @param type          region type
     * @param annots        annotations
     * @param masterNodeIds list of master nodes; null implies empty list
     * @return new region descriptor
     * @throws IllegalArgumentException if item already exists
     */
    Region updateRegion(RegionId regionId, String name, Region.Type type,
                        Annotations annots, List<Set<NodeId>> masterNodeIds);

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
