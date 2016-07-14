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
 * Extended traffic marking class.
 */
public interface ExtTrafficMarking extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the traffic marking DSCP value.
     *
     * @return the marking rule
     */
    byte marking();

    /**
     * Returns whether this traffic marking is an exact match to the traffic marking given
     * in the argument.
     *
     * @param marking other traffic marking to match against
     * @return true if the traffic marking are an exact match, otherwise false
     */
    boolean exactMatch(ExtTrafficMarking marking);

    /**
     * A traffic marking builder..
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
         * Assigns the traffic marking to this object.
         *
         * @param marking the marking value
         * @return this the builder object
         */
        Builder setMarking(byte marking);

        /**
         * Builds a traffic marking object.
         *
         * @return a marking value object.
         */
        ExtTrafficMarking build();
    }
}
