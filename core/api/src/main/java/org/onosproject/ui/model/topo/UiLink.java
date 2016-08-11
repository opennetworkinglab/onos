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

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a link (line between two elements). This may be one of
 * several concrete subclasses:
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
 * representing multiple underlying UI link instances, for example
 * the link between two sub-regions in a region (layout).
 * </li>
 * </ul>
 */
public abstract class UiLink extends UiElement {

    protected final UiTopology topology;
    protected final UiLinkId id;

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

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .toString();
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
     * Returns the implementing class name as the type of link.
     *
     * @return link type
     */
    public String type() {
        return getClass().getSimpleName();
    }

    /**
     * Returns the identifier of end-point A in string form.
     *
     * @return end point A identifier
     */
    public abstract String endPointA();

    /**
     * Returns the identifier of end-point B in string form.
     *
     * @return end point B identifier
     */
    public abstract String endPointB();

    /**
     * Returns the port number (as a string) for end-point A, if applicable.
     * This default implementation returns null, indicating not-applicable.
     * Subclasses only need to override this method if end-point A has an
     * associated port.
     *
     * @return port number for end-point A
     */
    public String endPortA() {
        return null;
    }

    /**
     * Returns the port number (as a string) for end-point B, if applicable.
     * This default implementation returns null, indicating not-applicable.
     * Subclasses only need to override this method if end-point B has an
     * associated port.
     *
     * @return port number for end-point B
     */
    public String endPortB() {
        return null;
    }
}
