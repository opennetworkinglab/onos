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
 * Extended multivalue packet length list class.
 */
public interface ExtPacketLength extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the packet length operator value list.
     *
     * @return the packet length operator value list
     */
    List<ExtOperatorValue> packetLength();

    /**
     * Returns whether this packet length list is an exact match to the packet length list given
     * in the argument.
     *
     * @param packetLength other packet length to match against
     * @return true if the packet length list are an exact match, otherwise false
     */
    boolean exactMatch(ExtPacketLength packetLength);

    /**
     * A packet length extended builder..
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
         * Assigns the packet length list to this object.
         *
         * @param packetLength the packet length
         * @return this the builder object
         */
        Builder setPacketLength(List<ExtOperatorValue> packetLength);

        /**
         * Builds a packet length object.
         *
         * @return a packet length object.
         */
        ExtPacketLength build();
    }
}
