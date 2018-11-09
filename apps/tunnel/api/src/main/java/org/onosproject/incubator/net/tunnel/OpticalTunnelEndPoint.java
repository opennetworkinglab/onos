/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.tunnel;

import java.util.Optional;

import com.google.common.annotations.Beta;
import org.onosproject.net.Annotated;
import org.onosproject.net.ElementId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Provided;

/**
 * Generic representation of a logical port entity in a consistent way,
 * it is used to identify e.g., ODUk timeSlot, WDM lambda, etc.
 * It supports nested case.
 */
@Beta
public interface OpticalTunnelEndPoint extends TunnelEndPoint, Annotated, Provided, NetworkResource {

    /** Represents coarse tunnel point type classification. */
    public enum Type {
        /**
         * Signifies optical data unit-based tunnel point.
         */
        TIMESLOT,

        /**
         * Signifies optical wavelength-based tunnel point.
         */
        LAMBDA
    }

    /**
     * Returns the identifier.
     *
     * @return identifier
     */
    OpticalLogicId id();

    /**
     * Returns the parent network element to which this tunnel point belongs.
     *
     * @return parent network element
     */
    Optional<ElementId> elementId();

    /**
     * Returns the parent network port to which this tunnel point belongs, can not be be null.
     *
     * @return port number
     */
    Optional<PortNumber> portNumber();

    /**
     * Returns the parent tunnel point to which this tunnel point belongs, optional.
     *
     * @return parent tunnel point, if it is null, the parent is a physical port
     */
    Optional<OpticalTunnelEndPoint> parentPoint();

    /**
     * Indicates whether or not the port is global significant.
     *
     * @return true if the port is global significant
     */
    boolean isGlobal();

    /**
     * Returns the tunnel point type.
     *
     * @return tunnel point type
     */
    Type type();
}
