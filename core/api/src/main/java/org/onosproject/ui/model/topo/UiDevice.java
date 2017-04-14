/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.model.topo;

import com.google.common.base.MoreObjects;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.RegionId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a device.
 */
public class UiDevice extends UiNode {

    private static final String DEVICE_CANNOT_BE_NULL = "Device cannot be null";

    private final UiTopology topology;
    private final DeviceId deviceId;

    private RegionId regionId;

    /**
     * Creates a new UI device.
     *
     * @param topology parent topology
     * @param device   backing device
     */
    public UiDevice(UiTopology topology, Device device) {
        checkNotNull(device, DEVICE_CANNOT_BE_NULL);
        this.topology = topology;
        this.deviceId = device.id();
    }

    /**
     * Sets the ID of the region to which this device belongs.
     *
     * @param regionId region identifier
     */
    public void setRegionId(RegionId regionId) {
        this.regionId = regionId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id())
                .add("region", regionId)
                .toString();
    }

    /**
     * Returns the identity of the device.
     *
     * @return device ID
     */
    public DeviceId id() {
        return deviceId;
    }

    @Override
    public String idAsString() {
        return id().toString();
    }

    /**
     * Returns the device instance backing this UI device.
     *
     * @return the backing device instance
     */
    public Device backingDevice() {
        return topology.services.device().getDevice(deviceId);
    }

    /**
     * Returns the identifier of the region to which this device belongs.
     * This will be null if the device does not belong to any region.
     *
     * @return region ID
     */
    public RegionId regionId() {
        return regionId;
    }

    /**
     * Returns the UI region to which this device belongs.
     *
     * @return the UI region
     */
    public UiRegion uiRegion() {
        return topology.findRegion(regionId);
    }

    /**
     * Returns a string representation of the type of the backing device.
     *
     * @return the device type
     */
    public String type() {
        return backingDevice().type().toString().toLowerCase();
    }
}
