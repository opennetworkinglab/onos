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
 * Extended multivalue Dscp value class.
 */
public interface ExtDscpValue extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the dscp value operator value list.
     *
     * @return the dscp value operator value list
     */
    List<ExtOperatorValue> dscpValue();

    /**
     * Returns whether this dscp value list is an exact match to the dscp value list given
     * in the argument.
     *
     * @param dscpValue other dscp value to match against
     * @return true if the dscp value list are an exact match, otherwise false
     */
    boolean exactMatch(ExtDscpValue dscpValue);

    /**
     * A dscp value extended builder..
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
         * Assigns the dscp operator value to this object.
         *
         * @param dscpValue the dscp value
         * @return this the builder object
         */
        Builder setDscpValue(List<ExtOperatorValue> dscpValue);

        /**
         * Builds a dscp value object.
         *
         * @return a dscp value object.
         */
        ExtDscpValue build();
    }
}
