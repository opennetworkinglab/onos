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

/**
 * Extended flow traffic rate class.
 */
public interface ExtTrafficRate extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the AS number.
     *
     * @return the 2 byte ASN
     */
    Short asn();

    /**
     * Returns the traffic rate.
     *
     * @return the floating point traffic rate
     */
    Float rate();

    /**
     * Returns whether this traffic rate is an exact match to the traffic rate given
     * in the argument.
     *
     * @param trafficRate other traffic rate to match against
     * @return true if the traffic rate list are an exact match, otherwise false
     */
    boolean exactMatch(ExtTrafficRate trafficRate);

    /**
     * A traffic rate builder..
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
         * Assigns the AS number to this object.
         *
         * @param asn the ASN
         * @return this the builder object
         */
        Builder setAsn(short asn);

        /**
         * Assigns the traffic rate to this object.
         *
         * @param rate in floating point number bytes per second
         * @return this the builder object
         */
        Builder setRate(float rate);

        /**
         * Builds a traffic rate object.
         *
         * @return a traffic rate object.
         */
        ExtTrafficRate build();
    }
}
