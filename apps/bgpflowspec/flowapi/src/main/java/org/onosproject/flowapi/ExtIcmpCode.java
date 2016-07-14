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
package org.onosproject.flowapi;

import java.util.List;

/**
 * Extended multivalue ICMP code class.
 */
public interface ExtIcmpCode extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the Icmp code operator value list.
     *
     * @return the Icmp code operator value list
     */
    List<ExtOperatorValue> icmpCode();

    /**
     * Returns whether this Icmp code list is an exact match to the Icmp code list given
     * in the argument.
     *
     * @param icmpCode other Icmp code to match against
     * @return true if the Icmp code list are an exact match, otherwise false
     */
    boolean exactMatch(ExtIcmpCode icmpCode);

    /**
     * An Icmp code extension builder..
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
         * Assigns the Icmp code operator value to this object.
         *
         * @param icmpCode the Icmp code operator value combination
         * @return this the builder object
         */
        Builder setIcmpCode(List<ExtOperatorValue> icmpCode);

        /**
         * Builds a Icmp code object.
         *
         * @return a Tcmp code object.
         */
        ExtIcmpCode build();
    }
}
