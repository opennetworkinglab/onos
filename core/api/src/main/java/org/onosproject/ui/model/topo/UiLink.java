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
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Link;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a link (line between two elements). This may have one of
 * several forms:
 * <ul>
 * <li>
 * An infrastructure link:
 * two backing unidirectional links between two devices.
 * </li>
 * <li>
 * An edge link:
 * representing the connection between a host and a device.
 * </li>
 * <li>
 * An aggregation link:
 * representing multiple underlying UI link instances.
 * </li>
 * </ul>
 */
public class UiLink extends UiElement {

    private static final String E_UNASSOC =
            "backing link not associated with this UI link: ";

    private final UiTopology topology;
    private final UiLinkId id;

    /**
     * Creates a UI link.
     *
     * @param topology parent topology
     * @param id       canonicalized link identifier
     */
    public UiLink(UiTopology topology, UiLinkId id) {
        this.topology = topology;
        this.id = id;
    }

    // devices at either end of this link
    private DeviceId deviceA;
    private DeviceId deviceB;

    // two unidirectional links underlying this link...
    private Link linkAtoB;
    private Link linkBtoA;

    // ==OR== : private (synthetic) host link
    private DeviceId edgeDevice;
    private EdgeLink edgeLink;

    // ==OR== : set of underlying UI links that this link aggregates
    private Set<UiLink> children;


    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .toString();
    }

    @Override
    protected void destroy() {
        deviceA = null;
        deviceB = null;
        linkAtoB = null;
        linkBtoA = null;
        edgeLink = null;
        if (children != null) {
            children.clear();
            children = null;
        }
    }

    /**
     * Returns the canonicalized link identifier for this link.
     *
     * @return the link identifier
     */
    public UiLinkId id() {
        return id;
    }

    @Override
    public String idAsString() {
        return id.toString();
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
            deviceB = link.dst().deviceId();

        } else if (d == UiLinkId.Direction.B_TO_A) {
            linkBtoA = link;
            deviceB = link.src().deviceId();
            deviceA = link.dst().deviceId();

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
        // but checking not null is sufficient
        if (d == null) {
            throw new IllegalArgumentException(E_UNASSOC + elink);
        }

        edgeLink = elink;
        edgeDevice = elink.hostLocation().deviceId();
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
     * Returns the identity of device B.
     *
     * @return device B ID
     */
    public DeviceId deviceB() {
        return deviceB;
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
