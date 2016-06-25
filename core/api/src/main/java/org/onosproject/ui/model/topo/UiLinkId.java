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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;

/**
 * A canonical representation of an identifier for {@link UiLink}s.
 */
public final class UiLinkId {

    /**
     * Designates the directionality of an underlying (uni-directional) link.
     */
    public enum Direction {
        A_TO_B,
        B_TO_A
    }

    private static final String CP_DELIMITER = "~";
    private static final String ID_PORT_DELIMITER = "/";

    private final ElementId idA;
    private final PortNumber portA;
    private final ElementId idB;
    private final PortNumber portB;

    private final String idStr;

    /**
     * Creates a UI link identifier. It is expected that A comes before B when
     * the two identifiers are naturally sorted, thus providing a representation
     * which is invariant to whether A or B is source or destination of the
     * underlying link.
     *
     * @param a first element ID
     * @param pa first element port
     * @param b second element ID
     * @param pb second element port
     */
    private UiLinkId(ElementId a, PortNumber pa, ElementId b, PortNumber pb) {
        idA = a;
        portA = pa;
        idB = b;
        portB = pb;

        idStr = a + ID_PORT_DELIMITER + pa + CP_DELIMITER +
                b + ID_PORT_DELIMITER + pb;
    }

    @Override
    public String toString() {
        return idStr;
    }

    /**
     * Returns the identifier of the first element.
     *
     * @return first element identity
     */
    public ElementId elementA() {
        return idA;
    }

    /**
     * Returns the port of the first element.
     *
     * @return first element port
     */
    public PortNumber portA() {
        return portA;
    }

    /**
     * Returns the identifier of the second element.
     *
     * @return second element identity
     */
    public ElementId elementB() {
        return idB;
    }

    /**
     * Returns the port of the second element.
     *
     * @return second element port
     */
    public PortNumber portB() {
        return portB;
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
        return idA.equals(srcId) ? Direction.A_TO_B
                : idB.equals(srcId) ? Direction.B_TO_A
                : null;
    }

    /**
     * Generates the canonical link identifier for the given link.
     *
     * @param link link for which the identifier is required
     * @return link identifier
     * @throws NullPointerException if any of the required fields are null
     */
    public static UiLinkId uiLinkId(Link link) {
        ConnectPoint src = link.src();
        ConnectPoint dst = link.dst();
        if (src == null || dst == null) {
            throw new NullPointerException("null src or dst connect point: " + link);
        }

        ElementId srcId = src.elementId();
        ElementId dstId = dst.elementId();

        // canonicalize
        int comp = srcId.toString().compareTo(dstId.toString());
        return comp <= 0 ? new UiLinkId(srcId, src.port(), dstId, dst.port())
                : new UiLinkId(dstId, dst.port(), srcId, src.port());
    }
}
