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
 * Extended multivalue Fragment value list class.
 */
public interface ExtFragment extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the fragment operator value list.
     *
     * @return the fragment operator value list
     */
    List<ExtOperatorValue> fragment();

    /**
     * Returns whether this fragment value list is an exact match to the fragment value list given
     * in the argument.
     *
     * @param fragment other fragment value to match against
     * @return true if the fragment value list are an exact match, otherwise false
     */
    boolean exactMatch(ExtFragment fragment);

    /**
     * Extended fragment value builder..
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
         * Assigns the fragment operator value to this object.
         *
         * @param fragment the fragment value
         * @return this the builder object
         */
        Builder setFragment(List<ExtOperatorValue> fragment);

        /**
         * Builds a fragment value object.
         *
         * @return a fragment value object.
         */
        ExtFragment build();
    }
}

