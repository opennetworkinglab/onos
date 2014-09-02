package org.onlab.graph;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Test of the base edge implementation.
 */
public class AbstractEdgeTest {

    @Test
    public void equality() {
        TestVertex v1 = new TestVertex("1");
        TestVertex v2 = new TestVertex("2");
        new EqualsTester()
                .addEqualityGroup(new TestEdge(v1, v2, 1),
                                  new TestEdge(v1, v2, 1))
                .addEqualityGroup(new TestEdge(v2, v1, 1))
                .testEquals();
    }

}
