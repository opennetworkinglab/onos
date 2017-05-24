/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onosproject.net.driver.Projectable;

/**
 * Abstraction of a network infrastructure link.
 */
public interface Link extends Annotated, Provided, Projectable, NetworkResource {

    /**
     * Coarse representation of the link type.
     */
    enum Type {
        /**
         * Signifies that this is a direct single-segment link.
         */
        DIRECT,

        /**
         * Signifies that this link is potentially comprised from multiple
         * underlying segments or hops, and as such should be used to tag
         * links traversing optical paths, tunnels or intervening 'dark'
         * switches.
         */
        INDIRECT,

        /**
         * Signifies that this link is an edge, i.e. host link.
         */
        EDGE,

        /**
         * Signifies that this link represents a logical link backed by
         * some form of a tunnel, e.g., GRE, MPLS, ODUk, OCH.
         */
        TUNNEL,

        /**
         * Signifies that this link is realized by fiber (either single channel or WDM).
         */
        OPTICAL,

        /**
         * Signifies that this link is a virtual link or a pseudo-wire.
         */
        VIRTUAL
    }

    /**
     * Representation of the link state, which applies primarily only to
     * configured durable links, i.e. those that need to remain present,
     * but instead be marked as inactive.
     */
    enum State {
        /**
         * Signifies that a link is currently active.
         */
        ACTIVE,

        /**
         * Signifies that a link is currently inactive.
         */
        INACTIVE
    }

    /**
     * Returns the link source connection point.
     *
     * @return link source connection point
     */
    ConnectPoint src();

    /**
     * Returns the link destination connection point.
     *
     * @return link destination connection point
     */
    ConnectPoint dst();

    /**
     * Returns the link type.
     *
     * @return link type
     */
    Type type();

    /**
     * Returns the link state.
     *
     * @return link state
     */
    State state();

    /**
     * Indicates if the link was created from a predefined configuration.
     *
     * @return true if the link was created from a predefined configuration,
     *              false otherwise.
     */
    boolean isExpected();
}
