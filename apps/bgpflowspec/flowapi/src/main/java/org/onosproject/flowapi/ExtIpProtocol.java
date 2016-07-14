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
 * Extended multivalue Ip protocol class.
 */
public interface ExtIpProtocol extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the Ip protocol operator value list.
     *
     * @return the Ip protocol operator value list
     */
    List<ExtOperatorValue> ipProtocol();

    /**
     * Returns whether this Ip protocol list is an exact match to the Ip protocol list given
     * in the argument.
     *
     * @param ipProto other Ip protocols list to match against
     * @return true if the Ip protocols list are an exact match, otherwise false
     */
    boolean exactMatch(ExtIpProtocol ipProto);

    /**
     * Ip protocol extension builder.
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
         * Adds the Ip protocol operator value to this object.
         *
         * @param ipProto is the operator-value combination
         * @return this the builder object
         */
        Builder setIpProtocol(List<ExtOperatorValue> ipProto);

        /**
         * Builds a Ip protocol object.
         *
         * @return a Ip protocol object.
         */
        ExtIpProtocol build();
    }
}
