/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping.addresses;

/**
 * Presentation of a single mapping selection.
 */
public interface MappingAddress {

    String TYPE_SEPARATOR = ":";

    /**
     * Types of address to which the mapping criterion may apply.
     */
    enum Type {

        /** IPv4 Address. */
        IPV4,

        /** IPv6 Address. */
        IPV6,

        /** Autonomous System Number. */
        AS,

        /** Ethernet Address (MAC Address). */
        ETH,

        /** Distinguished Name. */
        DN,

        /** Extension Address. */
        EXTENSION
    }

    /**
     * Returns the type of mapping address.
     *
     * @return type of mapping address
     */
    Type type();
}
