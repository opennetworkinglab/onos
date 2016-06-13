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
 * Extended multivalue port class.
 */
public interface ExtPort extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the port operator value list.
     *
     * @return the port operator value list
     */
    List<ExtOperatorValue> port();

    /**
     * Returns whether this port list is an exact match to the port list given
     * in the argument.
     *
     * @param port other port to match against
     * @return true if the port list are an exact match, otherwise false
     */
    boolean exactMatch(ExtPort port);

    /**
     * An extended port extension builder..
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
         * Assigns the port list operator value to this object.
         *
         * @param port is the operator-value combination
         * @return this the builder object
         */
        Builder setPort(List<ExtOperatorValue> port);

        /**
         * Builds a port object.
         *
         * @return a port object.
         */
        ExtPort build();
    }
}
