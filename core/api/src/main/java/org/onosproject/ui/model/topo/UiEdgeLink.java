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
import org.onosproject.net.EdgeLink;
import org.onosproject.net.PortNumber;

/**
 * Designates a link between a device and a host; that is, an edge link.
 */
public class UiEdgeLink extends UiLink {

    private static final String E_UNASSOC =
            "backing link not associated with this UI edge link: ";

    // private (synthetic) host link
    private DeviceId edgeDevice;
    private PortNumber edgePort;
    private EdgeLink edgeLink;

    /**
     * Creates a UI link.
     *
     * @param topology parent topology
     * @param id       canonicalized link identifier
     */
    public UiEdgeLink(UiTopology topology, UiLinkId id) {
        super(topology, id);
    }

    @Override
    public String endPointA() {
        return edgeLink.hostId().toString();
    }

    @Override
    public String endPointB() {
        return edgeDevice + UiLinkId.ID_PORT_DELIMITER + edgePort;
    }

    // no port for end-point A

    @Override
    public String endPortB() {
        return edgePort.toString();
    }

    @Override
    protected void destroy() {
        edgeDevice = null;
        edgePort = null;
        edgeLink = null;
    }

    /**
     * Attaches the given edge link to this UI link. This method will
     * throw an exception if this UI link is not representative of the
     * supplied link.
     *
     * @param elink edge link to attach
     * @throws IllegalArgumentException if the link is not appropriate
     */
    public void attachEdgeLink(EdgeLink elink) {
        UiLinkId.Direction d = id.directionOf(elink);
        // Expected direction of edge links is A-to-B (Host to device)
        // but checking not null is a sufficient test
        if (d == null) {
            throw new IllegalArgumentException(E_UNASSOC + elink);
        }

        edgeLink = elink;
        edgeDevice = elink.hostLocation().deviceId();
        edgePort = elink.hostLocation().port();
    }

}
