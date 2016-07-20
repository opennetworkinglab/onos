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
 * Extended flow traffic redirect class.
 */
public interface ExtTrafficRedirect extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the traffic redirect in human readable format.
     *
     * @return the redirect rule
     */
    String redirect();

    /**
     * Returns whether this traffic redirect is an exact match to the traffic redirect given
     * in the argument.
     *
     * @param redirect other traffic redirect to match against
     * @return true if the traffic redirect are an exact match, otherwise false
     */
    boolean exactMatch(ExtTrafficRedirect redirect);

    /**
     * A dscp value list builder..
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
         * Assigns the traffic redirect to this object.
         *
         * @param redirect the redirect value
         * @return this the builder object
         */
        Builder setRedirect(String redirect);

        /**
         * Builds a traffic redirect object.
         *
         * @return a redirect value object.
         */
        ExtTrafficRedirect build();
    }
}
