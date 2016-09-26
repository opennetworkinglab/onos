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

package org.onlab.graph;

/**
 * Abstraction of a graph edge weight.
 */
public interface Weight extends Comparable<Weight> {

    /**
     * Merges the given weight with this one returning a new aggregated
     * weight.
     *
     * @param otherWeight weight to add
     * @return aggregated weight
     */
    Weight merge(Weight otherWeight);

    /**
     * Subtracts the given weight from this one and produces a new weight.
     *
     * @param otherWeight weight to subtract
     * @return residual weight
     */
    Weight subtract(Weight otherWeight);

    /**
     * Returns true if the weighted subject (link/path) can be traversed; false otherwise.
     *
     * @return true if weight is adequate, false if weight is infinite
     */
    boolean isViable();

    /**
     * Returns true if the weight is negative (means that aggregated
     * path cost will decrease if we add weighted subject to it).
     *
     * @return true if the weight is negative, false otherwise
     */
    boolean isNegative();
}
