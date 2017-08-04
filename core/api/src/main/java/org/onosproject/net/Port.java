/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;


/**
 * Abstraction of a network port.
 */
public interface Port extends Annotated {

    /** Represents coarse port type classification. */
    enum Type {
        /**
         * Signifies copper-based connectivity.
         */
        COPPER,

        /**
         * Signifies optical fiber-based connectivity.
         */
        FIBER,

        /**
         * Signifies optical fiber-based packet port.
         */
        PACKET,

        /**
         * Signifies optical fiber-based optical tributary port (called T-port).
         * The signal from the client side will be formed into a ITU G.709 (OTN) frame.
         */
        ODUCLT,

        /**
         * Signifies optical fiber-based Line-side port (called L-port).
         */
        OCH,

        /**
         * Signifies optical fiber-based WDM port (called W-port).
         * Optical Multiplexing Section (See ITU G.709).
         */
        OMS,

        /**
         * Signifies virtual port.
         */
        VIRTUAL,

        /**
         * Signifies optical fiber-based OTN port.
         */
        OTU
    }

    /**
     * Returns the parent network element to which this port belongs.
     *
     * @return parent network element
     */
    Element element();

    /**
     * Returns the port number.
     *
     * @return port number
     */
    PortNumber number();

    /**
     * Indicates whether or not the port is currently up and active.
     *
     * @return true if the port is operational
     */
    boolean isEnabled();

    /**
     * Returns the port type.
     *
     * @return port type
     */
    Type type();

    /**
     * Returns the current port speed in Mbps.
     *
     * @return current port speed
     */
    long portSpeed();

    // TODO: more attributes?
}
