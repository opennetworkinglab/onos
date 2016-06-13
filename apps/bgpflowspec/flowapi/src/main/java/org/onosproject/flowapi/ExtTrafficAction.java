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
 * Extended flow traffic action class.
 */
public interface ExtTrafficAction extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the terminal action.
     *
     * @return the terminal action is set or not
     */
    boolean terminal();

    /**
     * Returns the traffic sampling.
     *
     * @return the traffic sampling set or not
     */
    boolean sample();

    /**
     * Returns the traffic action to be taken is rpd.
     *
     * @return the traffic action rpd is set or not
     */
    boolean rpd();

    /**
     * Returns whether this traffic action is an exact match to the traffic action given
     * in the argument.
     *
     * @param trafficAction other traffic action to match against
     * @return true if the traffic action are an exact match, otherwise false
     */
    boolean exactMatch(ExtTrafficAction trafficAction);

    /**
     * A traffic action builder..
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
         * Assigns the terminal action to this object.
         *
         * @param terminal action
         * @return this the builder object
         */
        Builder setTerminal(boolean terminal);

        /**
         * Assigns the traffic sampling to this object.
         *
         * @param sample to be done or not
         * @return this the builder object
         */
        Builder setSample(boolean sample);

        /**
         * Assigns the traffic action rpd to this object.
         *
         * @param rpd rpd or not
         * @return this the builder object
         */
        Builder setRpd(boolean rpd);

        /**
         * Builds a traffic action object.
         *
         * @return a traffic action object.
         */
        ExtTrafficAction build();
    }
}
