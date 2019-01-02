/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.segmentrouting.xconnect.api;

/**
 * Represents cross connect endpoint.
 */
public abstract class XconnectEndpoint {
    public static final String LB_KEYWORD = "LB:";
    static final String PORT_PATTERN = "^\\d+$";
    static final String LOAD_BALANCER_PATTERN = "^" + LB_KEYWORD + "\\d+$";

    /**
     * Types of endpoint.
     */
    public enum Type {
        /**
         * The endpoint is specified by an port number.
         */
        PORT,

        /**
         * The endpoint is specified by a load balancer.
         */
        LOAD_BALANCER
    }

    /**
     * Type of this endpoint.
     *
     * @return type
     */
    public abstract XconnectEndpoint.Type type();

    /**
     * Constructs XconnectEndpoint from string.
     *
     * @param s string
     * @return XconnectEndpoint
     * @throws IllegalArgumentException if given string is in a wrong format
     */
    public static XconnectEndpoint fromString(String s) {
        if (s.matches(XconnectEndpoint.PORT_PATTERN)) {
            return XconnectPortEndpoint.fromString(s);
        } else if (s.matches(XconnectEndpoint.LOAD_BALANCER_PATTERN)) {
            return XconnectLoadBalancerEndpoint.fromString(s);
        } else {
            throw new IllegalArgumentException("Illegal endpoint format: " + s);
        }
    }
}
