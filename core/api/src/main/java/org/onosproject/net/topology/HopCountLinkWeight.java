package org.onosproject.net.topology;

import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.Type.INDIRECT;

/**
 * Link weight for measuring link cost as hop count with indirect links
 * being as expensive as traversing the entire graph to assume the worst.
 */
public class HopCountLinkWeight implements LinkWeight {
    private final int indirectLinkCost;

    /**
     * Creates a new hop-count weight.
     */
    public HopCountLinkWeight() {
        this.indirectLinkCost = Short.MAX_VALUE;
    }

    /**
     * Creates a new hop-count weight with the specified cost of indirect links.
     */
    public HopCountLinkWeight(int indirectLinkCost) {
        this.indirectLinkCost = indirectLinkCost;
    }

    @Override
    public double weight(TopologyEdge edge) {
        // To force preference to use direct paths first, make indirect
        // links as expensive as the linear vertex traversal.
        return edge.link().state() ==
                ACTIVE ? (edge.link().type() ==
                INDIRECT ? indirectLinkCost : 1) : -1;
    }
}

