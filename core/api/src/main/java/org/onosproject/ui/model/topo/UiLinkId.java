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

import org.onlab.util.Identifier;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.region.RegionId;

import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A canonical representation of an identifier for {@link UiLink}s.
 */
public final class UiLinkId {

    private static final String E_PORT_NULL = "Port number cannot be null";
    private static final String E_DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String E_HOST_ID_NULL = "Host ID cannot be null";
    private static final String E_REGION_ID_NULL = "Region ID cannot be null";
    private static final String E_IDENTICAL = "Region IDs cannot be same";

    private static final Comparator<RegionId> REGION_ID_COMPARATOR =
            Comparator.comparing(Identifier::toString);
    private static final Comparator<DeviceId> DEVICE_ID_COMPARATOR =
            Comparator.comparing(DeviceId::toString);


    /**
     * Designates the directionality of an underlying (uni-directional) link.
     */
    public enum Direction {
        A_TO_B,
        B_TO_A
    }

    /**
     * Designates the type of link the identifier represents.
     */
    public enum Type {
        REGION_REGION,
        REGION_DEVICE,
        DEVICE_DEVICE,
        HOST_DEVICE
    }

    static final String CP_DELIMITER = "~";
    static final String ID_PORT_DELIMITER = "/";

    private final RegionId regionA;
    private final ElementId elementA;
    private final PortNumber portA;

    private final RegionId regionB;
    private final ElementId elementB;
    private final PortNumber portB;

    private final String idStr;
    private final String idA;
    private final String idB;

    private final Type type;

    /**
     * Creates a UI link identifier. It is expected that A comes before B when
     * the two identifiers are naturally sorted, thus providing a representation
     * which is invariant to whether A or B is source or destination of the
     * underlying link.
     *
     * @param a  first element ID
     * @param pa first element port
     * @param b  second element ID
     * @param pb second element port
     */
    private UiLinkId(ElementId a, PortNumber pa, ElementId b, PortNumber pb) {
        elementA = a;
        portA = pa;
        elementB = b;
        portB = pb;

        regionA = null;
        regionB = null;

        boolean isEdgeLink = (a instanceof HostId);

        // NOTE: for edgelinks, hosts are always element A
        idA = isEdgeLink ? a.toString() : a + ID_PORT_DELIMITER + pa;
        idB = b + ID_PORT_DELIMITER + pb;
        idStr = idA + CP_DELIMITER + idB;

        type = isEdgeLink ? Type.HOST_DEVICE : Type.DEVICE_DEVICE;
    }

    /**
     * Creates a UI link identifier. It is expected that A comes before B when
     * the two identifiers are naturally sorted.
     *
     * @param a first region ID
     * @param b second region ID
     */
    private UiLinkId(RegionId a, RegionId b) {
        regionA = a;
        regionB = b;

        elementA = null;
        elementB = null;
        portA = null;
        portB = null;

        idA = a.toString();
        idB = b.toString();
        idStr = idA + CP_DELIMITER + idB;

        type = Type.REGION_REGION;
    }

    /**
     * Creates a UI link identifier, with region at one end and a device/port
     * at the other.
     *
     * @param r region ID
     * @param d device ID
     * @param p port number
     */
    private UiLinkId(RegionId r, DeviceId d, PortNumber p) {
        regionA = r;
        elementB = d;
        portB = p;

        regionB = null;
        elementA = null;
        portA = null;

        idA = r.toString();
        idB = elementB + ID_PORT_DELIMITER + portB;
        idStr = idA + CP_DELIMITER + idB;

        type = Type.REGION_DEVICE;
    }

    @Override
    public String toString() {
        return idStr;
    }

    /**
     * String representation of endpoint A.
     *
     * @return string rep of endpoint A
     */
    public String idA() {
        return idA;
    }

    /**
     * String representation of endpoint B.
     *
     * @return string rep of endpoint B
     */
    public String idB() {
        return idB;
    }

    /**
     * Returns the identifier of the first element. Note that the returned
     * value will be null if this identifier is for a region-region link.
     *
     * @return first element identity
     */
    public ElementId elementA() {
        return elementA;
    }

    /**
     * Returns the port of the first element. Note that the returned
     * value will be null if this identifier is for a region-region link.
     *
     * @return first element port
     */
    public PortNumber portA() {
        return portA;
    }

    /**
     * Returns the identifier of the second element. Note that the returned
     * value will be null if this identifier is for a region-region link.
     *
     * @return second element identity
     */
    public ElementId elementB() {
        return elementB;
    }

    /**
     * Returns the port of the second element. Note that the returned
     * value will be null if this identifier is for a region-region link.
     *
     * @return second element port
     */
    public PortNumber portB() {
        return portB;
    }

    /**
     * Returns the identity of the first region. Note that the returned value
     * will be null if this identifier is for a device-device or device-host
     * link.
     *
     * @return first region ID
     */
    public RegionId regionA() {
        return regionA;
    }

    /**
     * Returns the identity of the second region. Note that the returned value
     * will be null if this identifier is for a device-device or device-host
     * link.
     *
     * @return second region ID
     */
    public RegionId regionB() {
        return regionB;
    }

    /**
     * Returns the type of link this identifier represents.
     *
     * @return the link identifier type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns true if this identifier represents a region-region link.
     *
     * @return true if region-region link identifier; false otherwise
     */
    public boolean isRegionRegion() {
        return type == Type.REGION_REGION;
    }

    /**
     * Returns true if this identifier represents a region-device link.
     *
     * @return true if region-device link identifier; false otherwise
     */
    public boolean isRegionDevice() {
        return type == Type.REGION_DEVICE;
    }

    /**
     * Returns true if this identifier represents a device-device
     * (infrastructure) link.
     *
     * @return true if device-device link identifier; false otherwise
     */
    public boolean isDeviceDevice() {
        return type == Type.DEVICE_DEVICE;
    }

    /**
     * Returns true if this identifier represents a host-device (edge) link.
     *
     * @return true if host-device link identifier; false otherwise
     */
    public boolean isHostDevice() {
        return type == Type.HOST_DEVICE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UiLinkId uiLinkId = (UiLinkId) o;
        return idStr.equals(uiLinkId.idStr);
    }

    @Override
    public int hashCode() {
        return idStr.hashCode();
    }

    /**
     * Returns the direction of the given link, or null if this link ID does
     * not correspond to the given link.
     *
     * @param link the link to examine
     * @return corresponding direction
     */
    Direction directionOf(Link link) {
        ConnectPoint src = link.src();
        ElementId srcId = src.elementId();
        return elementA.equals(srcId) ? Direction.A_TO_B
                : elementB.equals(srcId) ? Direction.B_TO_A
                : null;
    }

    /**
     * Generates the canonical link identifier for the given link.
     *
     * @param link link for which the identifier is required
     * @return link identifier
     * @throws NullPointerException if src or dst connect point is null
     */
    public static UiLinkId uiLinkId(Link link) {
        return canonicalizeIdentifier(link.src(), link.dst());
    }

    /**
     * Creates the canonical link identifier from the given link key.
     *
     * @param lk link key
     * @return equivalent link identifier
     * @throws NullPointerException if src or dst connect point is null
     */
    public static UiLinkId uiLinkId(LinkKey lk) {
        return canonicalizeIdentifier(lk.src(), lk.dst());
    }

    private static UiLinkId canonicalizeIdentifier(ConnectPoint src, ConnectPoint dst) {
        if (src == null || dst == null) {
            throw new NullPointerException(
                    "null src or dst connect point (illegal for UiLinkId)");
        }
        ElementId srcId = src.elementId();
        ElementId dstId = dst.elementId();

        // canonicalize
        int comp = srcId.toString().compareTo(dstId.toString());
        return comp <= 0 ? new UiLinkId(srcId, src.port(), dstId, dst.port())
                : new UiLinkId(dstId, dst.port(), srcId, src.port());
    }

    /**
     * Generates the canonical link identifier for a link between the
     * specified region nodes.
     *
     * @param one the first region ID
     * @param two the second region ID
     * @return link identifier
     * @throws NullPointerException     if any of the required fields are null
     * @throws IllegalArgumentException if the identifiers are identical
     */
    public static UiLinkId uiLinkId(RegionId one, RegionId two) {
        checkNotNull(one, E_REGION_ID_NULL);
        checkNotNull(two, E_REGION_ID_NULL);
        checkArgument(!one.equals(two), E_IDENTICAL);

        boolean flip = REGION_ID_COMPARATOR.compare(one, two) > 0;
        return flip ? new UiLinkId(two, one) : new UiLinkId(one, two);
    }

    /**
     * Generates the canonical link identifier for a link between the specified
     * region and device/port.
     *
     * @param regionId   region ID
     * @param deviceId   device ID
     * @param portNumber port number
     * @return link identifier
     * @throws NullPointerException if any of the required fields are null
     */
    public static UiLinkId uiLinkId(RegionId regionId, DeviceId deviceId,
                                    PortNumber portNumber) {
        checkNotNull(regionId, E_REGION_ID_NULL);
        checkNotNull(deviceId, E_DEVICE_ID_NULL);
        checkNotNull(portNumber, E_PORT_NULL);

        return new UiLinkId(regionId, deviceId, portNumber);
    }

    /**
     * Generates an identifier for a link between two devices.
     *
     * @param a  device A
     * @param pa port A
     * @param b  device B
     * @param pb port B
     * @return link identifier
     * @throws NullPointerException if any of the required fields are null
     */
    public static UiLinkId uiLinkId(DeviceId a, PortNumber pa,
                                    DeviceId b, PortNumber pb) {
        checkNotNull(a, E_DEVICE_ID_NULL + " (A)");
        checkNotNull(b, E_DEVICE_ID_NULL + " (B)");
        checkNotNull(pa, E_PORT_NULL + " (A)");
        checkNotNull(pb, E_PORT_NULL + " (B)");

        boolean flip = DEVICE_ID_COMPARATOR.compare(a, b) > 0;
        return flip ? new UiLinkId(b, pb, a, pa) : new UiLinkId(a, pa, b, pb);
    }

    /**
     * Generates an identifier for an edge link. Note that host is always
     * element A.
     *
     * @param h host
     * @param d device
     * @param p port
     * @return link identifier
     * @throws NullPointerException if any of the required fields are null
     */
    public static UiLinkId uiLinkId(HostId h, DeviceId d, PortNumber p) {
        checkNotNull(h, E_HOST_ID_NULL);
        checkNotNull(d, E_DEVICE_ID_NULL);
        checkNotNull(p, E_PORT_NULL);
        return new UiLinkId(h, PortNumber.P0, d, p);
    }

}
