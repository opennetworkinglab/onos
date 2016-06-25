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

import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents an end-station host.
 */
public class UiHost extends UiNode {

    private final UiTopology topology;
    private final Host host;

    // Host location
    private DeviceId locDevice;
    private PortNumber locPort;

    private UiLinkId edgeLinkId;

    /**
     * Creates a new UI host.
     *
     * @param topology parent topology
     * @param host     backing host
     */
    public UiHost(UiTopology topology, Host host) {
        this.topology = topology;
        this.host = host;
    }

//    @Override
//    protected void destroy() {
//    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id())
                .add("dev", locDevice)
                .add("port", locPort)
                .toString();
    }

    /**
     * Returns the identity of the host.
     *
     * @return host ID
     */
    public HostId id() {
        return host.id();
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
        return host;
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
