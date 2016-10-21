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

package org.onosproject.tetunnel.api.tunnel.path;

/**
 * Representation of a TE LSP route element.
 */
public interface TeRouteSubobject {

    /**
     * Types of TE route subobject.
     */
    enum Type {
        /**
         * Designates Unnumbered link route sub-object.
         */
        UNNUMBERED_LINK,
        /**
         * Designates a label route sub-object.
         */
        LABEL,
        /**
         * Designates an IPv4 address route sub-object.
         */
        IPV4_ADDRESS,
        /**
         * Designates an IPv6 address route sub-object.
         */
        IPV6_ADDRESS
    }

    /**
     * Return type of the route subobject.
     *
     * @return type of route subobject
     */
    Type type();
}
