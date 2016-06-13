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

import org.onlab.packet.IpPrefix;

import java.util.List;

/**
 * Ext Prefix class.
 */
public interface ExtPrefix extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the prefix list.
     *
     * @return the IpPrefix list
     */
    List<IpPrefix> prefix();

    /**
     * Returns whether this prefix list is an exact match to the prefix list given
     * in the argument.
     *
     * @param prefix other prefix to match against
     * @return true if the prefix are an exact match, otherwise false
     */
    boolean exactMatch(ExtPrefix prefix);

    /**
     * A prefix builder..
     */
    interface Builder {

        /**
         * Assigns the ExtType to this object.
         *
         * @param type the prefix
         * @return this the builder object
         */
        Builder setType(ExtType type);

        /**
         * Add the prefix to this object.
         *
         * @param prefix the prefix
         * @return this the builder object
         */
        Builder setPrefix(IpPrefix prefix);

        /**
         * Builds a prefix object.
         *
         * @return a port chain.
         */
        ExtPrefix build();
    }
}
