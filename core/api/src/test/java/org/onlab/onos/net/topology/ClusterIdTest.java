package org.onlab.onos.net.topology;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.onlab.onos.net.topology.ClusterId.clusterId;

/**
 * Test of the cluster ID.
 */
public class ClusterIdTest {

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(clusterId(1), clusterId(1))
                .addEqualityGroup(clusterId(3), clusterId(3)).testEquals();
    }

    @Test
    public void basics() {
        assertEquals("incorrect index", 123, clusterId(123).index());
    }

}