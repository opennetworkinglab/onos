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

package org.onosproject.ui.model.topo;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.region.RegionId;


/**
 * Designates a link between a region node and a device.
 */
public class UiRegionDeviceLink extends UiLink {

    private static final String E_NOT_REGION_DEVICE_ID =
            "UI link identifier not region to device";

    // private (synthetic) region-device link
    private final RegionId region;
    private final DeviceId device;
    private final PortNumber port;

    /**
     * Creates a region to device UI link. Note that it is expected that the
     * link identifier is one that has a region ID at one end, and a device
     * ID at the other
     *
     * @param topology parent topology
     * @param id       canonicalized link identifier
     * @throws IllegalArgumentException if the link ID is not region-region
     */
    public UiRegionDeviceLink(UiTopology topology, UiLinkId id) {
        super(topology, id);
        region = id.regionA();
        device = (DeviceId) id.elementB();
        port = id.portB();
        if (region == null || device == null || port == null) {
            throw new IllegalArgumentException(E_NOT_REGION_DEVICE_ID);
        }
    }

    @Override
    public String endPointA() {
        return region.id();
    }

    @Override
    public String endPointB() {
        return device + UiLinkId.ID_PORT_DELIMITER + port;
    }

    // no port for end-point A

    @Override
    public String endPortB() {
        return port.toString();
    }

    /**
     * Returns the identity of the region.
     *
     * @return region ID
     */
    public RegionId region() {
        return region;
    }

    /**
     * Returns the identity of the device.
     *
     * @return device ID
     */
    public DeviceId device() {
        return device;
    }

    /**
     * Returns the identity of the device port.
     *
     * @return device port number
     */
    public PortNumber port() {
        return port;
    }
}
