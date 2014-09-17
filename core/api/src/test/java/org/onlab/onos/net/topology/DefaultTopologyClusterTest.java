package org.onlab.onos.net.topology;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.topology.ClusterId.clusterId;

/**
 * Test of the default topology cluster implementation.
 */
public class DefaultTopologyClusterTest {

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(cluster(3, 2, 1, "of:1"), cluster(3, 2, 1, "of:1"))
                .addEqualityGroup(cluster(3, 2, 1, "of:2"), cluster(3, 2, 1, "of:2"))
                .addEqualityGroup(cluster(0, 2, 1, "of:1"), cluster(0, 2, 1, "of:1"))
                .addEqualityGroup(cluster(3, 3, 1, "of:1"), cluster(3, 3, 1, "of:1"))
                .testEquals();
    }

    @Test
    public void basics() {
        TopologyCluster cluster = cluster(6, 5, 4, "of:111");
        assertEquals("incorrect id", clusterId(6), cluster.id());
        assertEquals("incorrect id", 5, cluster.deviceCount());
        assertEquals("incorrect id", 4, cluster.linkCount());
        assertEquals("incorrect id", deviceId("of:111"), cluster.root());

    }

    private TopologyCluster cluster(int id, int dc, int lc, String root) {
        return new DefaultTopologyCluster(clusterId(id), dc, lc, deviceId(root));
    }
}