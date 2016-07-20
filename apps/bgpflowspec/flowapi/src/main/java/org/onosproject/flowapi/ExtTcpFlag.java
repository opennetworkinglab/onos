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
 * Extended multivalue Tcp Flags list class.
 */
public interface ExtTcpFlag extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the Tcp flag Extended multivalue.
     *
     * @return the Tcp flag Extended multivalue
     */
    List<ExtOperatorValue> tcpFlag();

    /**
     * Returns whether this Tcp flag list is an exact match to the Tcp flag list given
     * in the argument.
     *
     * @param tcpFlag other Tcp flag to match against
     * @return true if the Tcp flag list are an exact match, otherwise false
     */
    boolean exactMatch(ExtTcpFlag tcpFlag);

    /**
     * A Tcp flag extended builder..
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
         * Assigns the Tcp flag operator value to this object.
         *
         * @param tcpFlag the Tcp flag
         * @return this the builder object
         */
        Builder setTcpFlag(List<ExtOperatorValue> tcpFlag);

        /**
         * Builds a Tcp flag object.
         *
         * @return a Tcp flag object.
         */
        ExtTcpFlag build();
    }
}
