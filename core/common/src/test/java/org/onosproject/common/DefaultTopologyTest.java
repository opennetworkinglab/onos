/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.common;

import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.DefaultEdgeWeigher;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onlab.packet.ChassisId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.DefaultGraphDescription;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;

import java.util.Set;
import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Test of the default topology implementation.
 */
public class DefaultTopologyTest {

    public static final ProviderId PID = new ProviderId("of", "foo.bar");

    public static final DeviceId D1 = deviceId("of:1");
    public static final DeviceId D2 = deviceId("of:2");
    public static final DeviceId D3 = deviceId("of:3");
    public static final DeviceId D4 = deviceId("of:4");
    public static final DeviceId D5 = deviceId("of:5");

    public static final TopologyVertex V1 = new DefaultTopologyVertex(D1);
    public static final TopologyVertex V5 = new DefaultTopologyVertex(D5);

    public static final PortNumber P1 = portNumber(1);
    public static final PortNumber P2 = portNumber(2);

    public static final class TestLinkWeigher
            extends DefaultEdgeWeigher<TopologyVertex, TopologyEdge>
            implements LinkWeigher {
        @Override
        public Weight weight(TopologyEdge edge) {
            double value = edge.src().deviceId().equals(D4) ||
                    edge.dst().deviceId().equals(D4)
                    ? 2.0 : HOP_WEIGHT_VALUE;
            return new ScalarWeight(value);
        }
    }
    public static final LinkWeigher WEIGHER = new TestLinkWeigher();


    private DefaultTopology dt;

    public static final ClusterId C0 = ClusterId.clusterId(0);
    public static final ClusterId C1 = ClusterId.clusterId(1);

    @Before
    public void setUp() {
        long now = System.currentTimeMillis();
        Set<Device> devices = of(device("1"), device("2"),
                                 device("3"), device("4"),
                                 device("5"));
        Set<Link> links = of(link("1", 1, "2", 1), link("2", 1, "1", 1),
                             link("3", 2, "2", 2), link("2", 2, "3", 2),
                             link("1", 3, "4", 3), link("4", 3, "1", 3),
                             link("3", 4, "4", 4), link("4", 4, "3", 4));
        GraphDescription graphDescription =
                new DefaultGraphDescription(now, System.currentTimeMillis(), devices, links);

        dt = new DefaultTopology(PID, graphDescription);
        assertEquals("incorrect supplier", PID, dt.providerId());
        assertEquals("incorrect time", now, dt.time());
        assertEquals("incorrect device count", 5, dt.deviceCount());
        assertEquals("incorrect link count", 8, dt.linkCount());
        assertEquals("incorrect cluster count", 2, dt.clusterCount());
        assertEquals("incorrect broadcast set size", 6, dt.broadcastSetSize(C0));
        assertEquals("incorrect root node", V1, dt.getCluster(C0).root());
        assertEquals("incorrect root node", V5, dt.getCluster(C1).root());
    }

    @Test
    public void pathRelated() {
        Set<Path> paths = dt.getPaths(D1, D2);
        assertEquals("incorrect path count", 1, paths.size());

        paths = dt.getPaths(D1, D3);
        assertEquals("incorrect path count", 2, paths.size());

        paths = dt.getPaths(D1, D5);
        assertTrue("no paths expected", paths.isEmpty());

        paths = dt.getPaths(D1, D3, WEIGHER);
        assertEquals("incorrect path count", 1, paths.size());

        paths = dt.getKShortestPaths(D1, D2, 42);
        assertEquals("incorrect path count", 2, paths.size());

        assertEquals("incorrect path count", 2, dt.getKShortestPaths(D1, D2).limit(42).count());

    }

    @Test
    public void pointRelated() {
        assertTrue("should be infrastructure point",
                   dt.isInfrastructure(new ConnectPoint(D1, P1)));
        assertFalse("should not be infrastructure point",
                    dt.isInfrastructure(new ConnectPoint(D1, P2)));
    }

    @Test
    public void clusterRelated() {
        Set<TopologyCluster> clusters = dt.getClusters();
        assertEquals("incorrect cluster count", 2, clusters.size());

        TopologyCluster c = dt.getCluster(D1);
        Set<DeviceId> devs = dt.getClusterDevices(c);
        assertEquals("incorrect cluster device count", 4, devs.size());
        assertTrue("cluster should contain D2", devs.contains(D2));
        assertFalse("cluster should not contain D5", devs.contains(D5));
    }

    // Short-hand for creating a link.
    public static Link link(String src, int sp, String dst, int dp) {
        return DefaultLink.builder().providerId(PID)
                .src(new ConnectPoint(did(src), portNumber(sp)))
                .dst(new ConnectPoint(did(dst), portNumber(dp)))
                .type(Link.Type.DIRECT)
                .build();
    }

    // Crates a new device with the specified id
    public static Device device(String id) {
        return new DefaultDevice(PID, did(id), Device.Type.SWITCH,
                                 "mfg", "1.0", "1.1", "1234", new ChassisId());
    }

    // Short-hand for producing a device id from a string
    public static DeviceId did(String id) {
        return deviceId("of:" + id);
    }

}
