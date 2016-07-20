/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.topology.ClusterId.clusterId;

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
        assertEquals("incorrect id", deviceId("of:111"), cluster.root().deviceId());

    }

    private TopologyCluster cluster(int id, int dc, int lc, String root) {
        return new DefaultTopologyCluster(clusterId(id), dc, lc,
                                          new DefaultTopologyVertex(deviceId(root)));
    }
}
