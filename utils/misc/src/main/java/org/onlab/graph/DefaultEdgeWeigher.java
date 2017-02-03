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
 * Default weigher returns identical weight for every graph edge. Basically it
 * is a hop count weigher.
 * Produces weights of {@link ScalarWeight} type.
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public class DefaultEdgeWeigher<V extends Vertex, E extends Edge<V>>
        implements EdgeWeigher<V, E> {

    /**
     * Common weight value for any link.
     */
    protected static final double HOP_WEIGHT_VALUE = 1;
    /**
     * Weight value for null path (without links).
     */
    protected static final double NULL_WEIGHT_VALUE = 0;

    /**
     * Default weight based on hop count.
     * {@value #HOP_WEIGHT_VALUE}
     */
    public static final ScalarWeight DEFAULT_HOP_WEIGHT =
            new ScalarWeight(HOP_WEIGHT_VALUE);

    /**
     * Default initial weight.
     * {@value #NULL_WEIGHT_VALUE}
     */
    public static final ScalarWeight DEFAULT_INITIAL_WEIGHT =
            new ScalarWeight(NULL_WEIGHT_VALUE);

    @Override
    public Weight weight(E edge) {
        return DEFAULT_HOP_WEIGHT;
    }

    @Override
    public Weight getInitialWeight() {
        return DEFAULT_INITIAL_WEIGHT;
    }

    @Override
    public Weight getNonViableWeight() {
        return ScalarWeight.NON_VIABLE_WEIGHT;
    }
}
