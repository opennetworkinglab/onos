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
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;

/**
 * Represents a link between two devices; that is, an infrastructure link.
 */
public class UiDeviceLink extends UiLink {

    private static final String E_UNASSOC =
            "backing link not associated with this UI device link: ";

    // devices and ports at either end of this link
    private DeviceId deviceA;
    private DeviceId deviceB;
    private PortNumber portA;
    private PortNumber portB;

    // two unidirectional links underlying this link...
    private Link linkAtoB;
    private Link linkBtoA;


    /**
     * Creates a device to device UI link.
     *
     * @param topology parent topology
     * @param id       canonicalized link identifier
     */
    public UiDeviceLink(UiTopology topology, UiLinkId id) {
        super(topology, id);
    }

    @Override
    public String endPointA() {
        return deviceA + UiLinkId.ID_PORT_DELIMITER + portA;
    }

    @Override
    public String endPointB() {
        return deviceB + UiLinkId.ID_PORT_DELIMITER + portB;
    }

    @Override
    public String endPortA() {
        return portA == null ? null : portA.toString();
    }

    @Override
    public String endPortB() {
        return portB == null ? null : portB.toString();
    }


    @Override
    protected void destroy() {
        deviceA = null;
        deviceB = null;
        portA = null;
        portB = null;
        linkAtoB = null;
        linkBtoA = null;
    }


    /**
     * Attaches the given backing link to this UI link. This method will
     * throw an exception if this UI link is not representative of the
     * supplied link.
     *
     * @param link backing link to attach
     * @throws IllegalArgumentException if the link is not appropriate
     */
    public void attachBackingLink(Link link) {
        UiLinkId.Direction d = id.directionOf(link);

        if (d == UiLinkId.Direction.A_TO_B) {
            linkAtoB = link;
            deviceA = link.src().deviceId();
            portA = link.src().port();
            deviceB = link.dst().deviceId();
            portB = link.dst().port();

        } else if (d == UiLinkId.Direction.B_TO_A) {
            linkBtoA = link;
            deviceB = link.src().deviceId();
            portB = link.src().port();
            deviceA = link.dst().deviceId();
            portA = link.dst().port();

        } else {
            throw new IllegalArgumentException(E_UNASSOC + link);
        }
    }

    /**
     * Detaches the given backing link from this UI link, returning true if the
     * reverse link is still attached, or false otherwise.
     *
     * @param link the backing link to detach
     * @return true if other link still attached, false otherwise
     * @throws IllegalArgumentException if the link is not appropriate
     */
    public boolean detachBackingLink(Link link) {
        UiLinkId.Direction d = id.directionOf(link);
        if (d == UiLinkId.Direction.A_TO_B) {
            linkAtoB = null;
            return linkBtoA != null;
        }
        if (d == UiLinkId.Direction.B_TO_A) {
            linkBtoA = null;
            return linkAtoB != null;
        }
        throw new IllegalArgumentException(E_UNASSOC + link);
    }


    /**
     * Returns the identity of device A.
     *
     * @return device A ID
     */
    public DeviceId deviceA() {
        return deviceA;
    }

    /**
     * Returns the port number of device A.
     *
     * @return port A
     */
    public PortNumber portA() {
        return portA;
    }

    /**
     * Returns the identity of device B.
     *
     * @return device B ID
     */
    public DeviceId deviceB() {
        return deviceB;
    }

    /**
     * Returns the port number of device B.
     *
     * @return port B
     */
    public PortNumber portB() {
        return portB;
    }

    /**
     * Returns backing link from A to B.
     *
     * @return backing link A to B
     */
    public Link linkAtoB() {
        return linkAtoB;
    }

    /**
     * Returns backing link from B to A.
     *
     * @return backing link B to A
     */
    public Link linkBtoA() {
        return linkBtoA;
    }

}
