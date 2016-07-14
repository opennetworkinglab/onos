/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.flowapi;

import java.util.List;

/**
 * Extended multivalue ICMP type class.
 */
public interface ExtIcmpType extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the Icmp type operator value list.
     *
     * @return the Icmp type operator value list
     */
    List<ExtOperatorValue> icmpType();

    /**
     * Returns whether this Icmp type list is an exact match to the Icmp type list given
     * in the argument.
     *
     * @param icmpType other Icmp type to match against
     * @return true if the Icmp type list are an exact match, otherwise false
     */
    boolean exactMatch(ExtIcmpType icmpType);

    /**
     * An Icmp type extension builder..
     */
    interface Builder {

        /**
         * Assigns the ExtType to this object.
         *
         * @param type extended type
         * @return this the builder object
         */
        Builder setType(ExtType type);

        /**
         * Assigns the Icmp type operator value to this object.
         *
         * @param icmpType the Icmp type operator value combination
         * @return this the builder object
         */
        Builder setIcmpType(List<ExtOperatorValue> icmpType);

        /**
         * Builds a Icmp type object.
         *
         * @return a Tcmp type object.
         */
        ExtIcmpType build();
    }
}
