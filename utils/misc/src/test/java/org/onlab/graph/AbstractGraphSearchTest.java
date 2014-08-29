package org.onlab.graph;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

/**
 * Base for all graph search tests.
 */
public abstract class AbstractGraphSearchTest extends GraphTest {

    /**
     * Creates a graph search to be tested.
     *
     * @return graph search
     */
    protected abstract GraphPathSearch<TestVertex, TestEdge> graphSearch();

    @Test(expected = IllegalArgumentException.class)
    public void badSource() {
        graphSearch().search(new AdjacencyListsGraph<>(ImmutableSet.of(B, C),
                                                       ImmutableSet.of(new TestEdge(B, C, 1))),
                             A, H, weight);
    }

    @Test(expected = NullPointerException.class)
    public void nullSource() {
        graphSearch().search(new AdjacencyListsGraph<>(ImmutableSet.of(B, C),
                                                       ImmutableSet.of(new TestEdge(B, C, 1))),
                             null, H, weight);
    }

    @Test(expected = NullPointerException.class)
    public void nullGraph() {
        graphSearch().search(null, A, H, weight);
    }

}
