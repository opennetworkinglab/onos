package org.onlab.graph;

import org.junit.Test;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;

/**
 * Base for all graph search tests.
 */
public abstract class AbstractGraphPathSearchTest extends GraphTest {

    /**
     * Creates a test-specific graph search to exercise.
     *
     * @return graph search
     */
    protected abstract AbstractGraphPathSearch<TestVertex, TestEdge> graphSearch();

    @Test(expected = IllegalArgumentException.class)
    public void noSuchSourceArgument() {
        graphSearch().search(new AdjacencyListsGraph<>(of(B, C),
                                                       of(new TestEdge(B, C, 1))),
                             A, H, weight);
    }

    @Test(expected = NullPointerException.class)
    public void nullGraphArgument() {
        graphSearch().search(null, A, H, weight);
    }

    @Test(expected = NullPointerException.class)
    public void nullSourceArgument() {
        graphSearch().search(new AdjacencyListsGraph<>(of(B, C),
                                                       of(new TestEdge(B, C, 1))),
                             null, H, weight);
    }

    @Test
    public void samenessThreshold() {
        AbstractGraphPathSearch<TestVertex, TestEdge> search = graphSearch();
        search.setSamenessThreshold(0.3);
        assertEquals("incorrect threshold", 0.3, search.samenessThreshold(), 0.01);
    }

}
