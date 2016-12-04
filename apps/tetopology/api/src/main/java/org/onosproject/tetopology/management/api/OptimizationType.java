/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

/**
 * TE optimization type.
 */
public enum OptimizationType {

    /**
     * Designates optimization is not applied.
     */
    NOT_OPTIMIZED(0),

    /**
     * Designates optimization criteria least cost.
     */
    LEAST_COST(1),

    /**
     * Designates optimization criteria shortest delay.
     */
    SHORTEST_DELAY(2),

    /**
     * Designates optimization criteria best link utilization.
     */
    BEST_LINK_UTILIZATION(3),

    /**
     * Designates optimization criteria best link protection.
     */
    BEST_LINK_PROTECTION(4);

    private int value;

    /**
     * Creates an instance of OptimizationType.
     *
     * @param value value of optimization type
     */
    OptimizationType(int value) {
        this.value = value;
    }

    /**
     * Returns the optimization type value.
     *
     * @return the value of optimization type
     */
    public int value() {
        return value;
    }

    /**
     * Returns the optimization constant corresponding to the given value.
     * If the given value cannot be mapped to any optimization type, a null
     * is returned.
     *
     * @param value the value of the optimization type
     * @return corresponding optimization type constant
     */
    public static OptimizationType of(int value) {
        switch (value) {
            case 0:
                return OptimizationType.NOT_OPTIMIZED;
            case 1:
                return OptimizationType.LEAST_COST;
            case 2:
                return OptimizationType.SHORTEST_DELAY;
            case 3:
                return OptimizationType.BEST_LINK_UTILIZATION;
            case 4:
                return OptimizationType.BEST_LINK_PROTECTION;
            default:
                return null;
        }
    }
}
