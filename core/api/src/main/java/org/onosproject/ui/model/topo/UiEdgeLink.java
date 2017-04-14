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

package org.onosproject.ui.model.topo;

import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;

/**
 * Designates a link between a device and a host; that is, an edge link.
 */
public class UiEdgeLink extends UiLink {

    private final HostId hostId;
    private final DeviceId deviceId;
    private final PortNumber port;

    /**
     * Creates a UI link.
     *
     * @param topology parent topology
     * @param id       canonicalized link identifier
     */
    public UiEdgeLink(UiTopology topology, UiLinkId id) {
        super(topology, id);
        hostId = HostId.hostId(id.idA());
        deviceId = (DeviceId) id.elementB();
        port = id.portB();
    }

    @Override
    public String endPointA() {
        return hostId.toString();
    }

    @Override
    public String endPointB() {
        return deviceId.toString();
    }

    // no port for end-point A

    @Override
    public String endPortB() {
        return port.toString();
    }

    /**
     * Returns the host identifier.
     *
     * @return host identifier
     */
    public HostId hostId() {
        return hostId;
    }

    /**
     * Returns the edge device identifier.
     *
     * @return device identifier
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the edge port number.
     *
     * @return edge port number
     */
    public PortNumber portNumber() {
        return port;
    }

}
