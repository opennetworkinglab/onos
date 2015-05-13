/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.tunnel;

import java.util.Optional;

import org.onosproject.net.Annotated;
import org.onosproject.net.ElementId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Provided;

/**
 * Generic representation of a logical port entity in a consistent way,
 * it is used to identify e.g., VLAN#, MPLS label#, ODUk timeSlot, WDM lambda, etc.
 * It supports nested case.
 */
public interface Label extends Annotated, Provided, NetworkResource {

    /** Represents coarse Label type classification. */
    enum Type {
        /**
         * Signifies VLAN-based tag.
         */
        VLAN,

        /**
         * Signifies LAG-based label.
         */
        LAG,

        /**
         * Signifies MPLS-based label.
         */
        MPLS,

        /**
         * Signifies IP-based label.
         */
        IP,

        /**
         * Signifies optical data unit-based label.
         */
        TIMESLOT,

        /**
         * Signifies optical wavelength-based label.
         */
        LAMBDA,

        /**
         * Signifies device-based identifier for the label.
         */
        DEVICE
    }

    /**
     * Returns the identifier to this Label.
     *
     * @return identifier
     */
    LabelId id();

    /**
     * Returns the parent network element to which this label belongs.
     *
     * @return parent network element
     */
    Optional<ElementId> elementId();

    /**
     * Returns the parent network port to which this label belongs, can not be be null.
     *
     * @return port number
     */
    Optional<PortNumber> portNumber();

    /**
     * Returns the parent label to which this label belongs, optional.
     *
     * @return parent label, if it is null, the parent is a physical port
     */
    Optional<Label> parentLabel();

    /**
     * Indicates whether or not the port is global significant.
     *
     * @return true if the port is global significant
     */
    boolean isGlobal();

    /**
     * Returns the label type.
     *
     * @return label type
     */
    Type type();
}
