/*
 *  Copyright 2016-present Open Networking Foundation
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

import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.region.RegionId;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an end-station host.
 */
public class UiHost extends UiNode {

    private static final String HOST_CANNOT_BE_NULL = "Host cannot be null";

    private final UiTopology topology;
    private final HostId hostId;

    // Host location
    private DeviceId locDevice;
    private PortNumber locPort;

    private UiLinkId edgeLinkId;
    private RegionId regionId;

    /**
     * Creates a new UI host.
     *
     * @param topology parent topology
     * @param host     backing host
     */
    public UiHost(UiTopology topology, Host host) {
        checkNotNull(host, HOST_CANNOT_BE_NULL);
        this.topology = topology;
        this.hostId = host.id();
        this.regionId = RegionId.regionId(UiRegion.NULL_NAME);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id())
                .add("dev", locDevice)
                .add("port", locPort)
                .add("Region", regionId)
                .toString();
    }

    /**
     * Returns the identity of the host.
     *
     * @return host ID
     */
    public HostId id() {
        return hostId;
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
     * Sets the ID of the region to which this device belongs.
     *
     * @param regionId region identifier
     */
    public void setRegionId(RegionId regionId) {
        this.regionId = regionId;
    }

    @Override
    public String idAsString() {
        return id().toString();
    }

    /**
     * Sets the host's current location.
     *
     * @param deviceId ID of device
     * @param port     port number
     */
    public void setLocation(DeviceId deviceId, PortNumber port) {
        locDevice = deviceId;
        locPort = port;
    }

    /**
     * Sets the ID of the edge link between this host and the device to which
     * it connects.
     *
     * @param id edge link identifier to set
     */
    public void setEdgeLinkId(UiLinkId id) {
        this.edgeLinkId = id;
    }

    /**
     * Returns the host instance backing this UI host.
     *
     * @return the backing host instance
     */
    public Host backingHost() {
        return topology.services.host().getHost(hostId);
    }

    /**
     * Returns the identifier for the edge link between this host and
     * the device to which it is connected.
     *
     * @return edge link identifier
     */
    public UiLinkId edgeLinkId() {
        return edgeLinkId;
    }

    /**
     * Returns the identifier of the device to which the host is connected.
     *
     * @return device identifier
     */
    public DeviceId locationDevice() {
        return locDevice;
    }

    /**
     * Returns the port number of the device to which the host is connected.
     *
     * @return port number
     */
    public PortNumber locationPort() {
        return locPort;
    }
}
