/*
 * Copyright 2017-present Open Networking Foundation
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

import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.Type.INDIRECT;

import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;

/**
 * Link weight for measuring link cost as hop count with indirect links
 * being as expensive as traversing the entire graph to assume the worst.
 */
public class HopCountLinkWeigher implements LinkWeigher {

    public static final LinkWeigher DEFAULT_HOP_COUNT_WEIGHER = new HopCountLinkWeigher();

    private static final ScalarWeight ZERO = new ScalarWeight(0.0);
    private static final ScalarWeight ONE = new ScalarWeight(1.0);
    private static final ScalarWeight DEFAULT_INDIRECT = new ScalarWeight(Short.MAX_VALUE);

    private final ScalarWeight indirectLinkCost;

    /**
     * Creates a new hop-count weight.
     */
    public HopCountLinkWeigher() {
        this.indirectLinkCost = DEFAULT_INDIRECT;
    }

    /**
     * Creates a new hop-count weight with the specified cost of indirect links.
     *
     * @param indirectLinkCost indirect link cost
     */
    public HopCountLinkWeigher(double indirectLinkCost) {
        this.indirectLinkCost = new ScalarWeight(indirectLinkCost);
    }

    @Override
    public Weight weight(TopologyEdge edge) {
        if (edge.link().state() == ACTIVE) {
            return edge.link().type() == INDIRECT ? indirectLinkCost : ONE;
        } else {
            return getNonViableWeight();
        }
    }

    @Override
    public Weight getInitialWeight() {
        return ZERO;
    }

    @Override
    public Weight getNonViableWeight() {
        return ScalarWeight.NON_VIABLE_WEIGHT;
    }

}
