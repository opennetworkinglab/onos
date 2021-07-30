/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.net.flowobjective;

/**
 * Represents next action in the NextObjective.
 */
public interface NextTreatment {
    int DEFAULT_WEIGHT = 1;
    /**
     * Types of next action.
     */
    enum Type {
        /**
         * The next action is specified by a TrafficTreatment.
         */
        TREATMENT,

        /**
         * The next action is specified by an Integer next id.
         */
        ID
    }

    /**
     * Type of this next action.
     *
     * @return type
     */
    Type type();
    /**
     * weight of this next action.
     *
     * @return weight
     */
    int weight();
}
