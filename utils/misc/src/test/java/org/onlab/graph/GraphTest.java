package org.onlab.graph;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

/**
 * Base class for various graph-related tests.
 */
public class GraphTest {

    static final TestVertex A = new TestVertex("A");
    static final TestVertex B = new TestVertex("B");
    static final TestVertex C = new TestVertex("C");
    static final TestVertex D = new TestVertex("D");
    static final TestVertex E = new TestVertex("E");
    static final TestVertex F = new TestVertex("F");
    static final TestVertex G = new TestVertex("G");
    static final TestVertex H = new TestVertex("H");

    protected Graph<TestVertex, TestEdge> g;

    protected EdgeWeight<TestVertex, TestEdge> weight =
            new EdgeWeight<TestVertex, TestEdge>() {
        @Override
        public double weight(TestEdge edge) {
            return edge.weight();
        }
    };

    protected void printPaths(Set<Path<TestVertex, TestEdge>> paths) {
        for (Path p : paths) {
            System.out.println(p);
        }
    }

    protected Set<TestVertex> vertices() {
        return of(A, B, C, D, E, F, G, H);
    }

    protected Set<TestEdge> edges() {
        return of(new TestEdge(A, B, 1), new TestEdge(A, C, 3),
                  new TestEdge(B, D, 2), new TestEdge(B, C, 1),
                  new TestEdge(B, E, 4), new TestEdge(C, E, 1),
                  new TestEdge(D, H, 5), new TestEdge(D, E, 1),
                  new TestEdge(E, F, 1), new TestEdge(F, D, 1),
                  new TestEdge(F, G, 1), new TestEdge(F, H, 1));
    }

}
