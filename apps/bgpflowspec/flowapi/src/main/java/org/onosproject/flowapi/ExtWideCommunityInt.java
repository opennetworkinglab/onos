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
 * Extended wide community integer class.
 */
public interface ExtWideCommunityInt extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the wide community integer list.
     *
     * @return the wide community integer list
     */
    List<Integer> communityInt();

    /**
     * Returns whether this wide community integer is an exact match to the wide community int given
     * in the argument.
     *
     * @param wCommInt other wide community integer to match against
     * @return true if the wide community integer are an exact match, otherwise false
     */
    boolean exactMatch(ExtWideCommunityInt wCommInt);

    /**
     * A wide community integer list builder..
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
         * Assigns the wide community integer to this object.
         *
         * @param wCommInt the wide community integer
         * @return this the builder object
         */
        Builder setwCommInt(Integer wCommInt);

        /**
         * Builds a wide community integer object.
         *
         * @return a wide community integer value object.
         */
        ExtWideCommunityInt build();
    }
}
