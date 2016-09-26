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

package org.onosproject.net.topology;

import org.onlab.graph.DefaultEdgeWeigher;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;

/**
 * Wrapper which transforms double-based link weigher to {@link Weight}-based
 * link weigher.
 */
public final class AdapterLinkWeigher
        extends DefaultEdgeWeigher<TopologyVertex, TopologyEdge>
        implements LinkWeigher {

    private final LinkWeight doubleWeigher;

    private AdapterLinkWeigher(LinkWeight doubleWeigher) {
        this.doubleWeigher = doubleWeigher;
    }

    @Override
    public Weight weight(TopologyEdge edge) {
        return new ScalarWeight(doubleWeigher.weight(edge));
    }

    /**
     * Transforms double-based link weigher to {@link Weight}-based weigher.
     *
     * @param lw double-based weigher
     * @return {@link Weight}-based weigher
     */
    public static LinkWeigher adapt(LinkWeight lw) {
        return lw == null ? null : new AdapterLinkWeigher(lw);
    }
}
