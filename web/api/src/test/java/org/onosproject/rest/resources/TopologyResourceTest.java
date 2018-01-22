/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.rest.resources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.DefaultTopologyCluster;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyServiceAdapter;

import javax.ws.rs.client.WebTarget;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.device;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.link;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for Topology REST APIs.
 */
public class TopologyResourceTest extends ResourceTest {

    private static class MockTopology implements Topology {
        @Override
        public long time() {
            return 11111L;
        }

        @Override
        public long creationTime() {
            return 22222L;
        }

        @Override
        public long computeCost() {
            return 0;
        }

        @Override
        public int clusterCount() {
            return 2;
        }

        @Override
        public int deviceCount() {
            return 6;
        }

        @Override
        public int linkCount() {
            return 4;
        }

        @Override
        public ProviderId providerId() {
            return ProviderId.NONE;
        }
    }

    private static class MockTopologyService extends TopologyServiceAdapter {
        final DefaultTopologyVertex root = new DefaultTopologyVertex(did("rootnode"));
        final Topology topology = new MockTopology();
        final TopologyCluster cluster1 =
                new DefaultTopologyCluster(ClusterId.clusterId(0),
                        2, 1, root);
        final TopologyCluster cluster2 =
                new DefaultTopologyCluster(ClusterId.clusterId(1),
                        4, 3, root);

        @Override
        public Topology currentTopology() {
            return topology;
        }

        @Override
        public Set<TopologyCluster> getClusters(Topology topology) {
            return ImmutableSet.of(cluster1, cluster2);
        }

        @Override
        public TopologyCluster getCluster(Topology topology, ClusterId clusterId) {
            return cluster1;
        }

        @Override
        public Set<DeviceId> getClusterDevices(Topology topology, TopologyCluster cluster) {
            DeviceId device1 = did("dev1");
            DeviceId device2 = did("dev2");

            return ImmutableSet.of(device1, device2);
        }

        @Override
        public Set<Link> getClusterLinks(Topology topology, TopologyCluster cluster) {
            Link link1 = link("src1", 1, "dst1", 1);
            Link link2 = link("src2", 1, "dst2", 1);
            Link link3 = link("src3", 1, "dst3", 1);
            return ImmutableSet.of(link1, link2, link3);
        }

        @Override
        public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
            return "dev2".equals(connectPoint.elementId().toString());
        }

        @Override
        public boolean isBroadcastPoint(Topology topology, ConnectPoint connectPoint) {
            return "dev1".equals(connectPoint.elementId().toString());
        }

    }

    private static class MockDeviceService extends DeviceServiceAdapter {

        @Override
        public Device getDevice(DeviceId deviceId) {
            String deviceId1 = "dev2";
            Device device = device(deviceId1);
            return device;
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            String deviceIdString = "dev2";
            Device device = device(deviceIdString);
            Port port = new DefaultPort(device, portNumber(1), true);
            return port;
        }
    }

    /**
     * Initializes the test harness.
     */
    @Before
    public void setUpTest() {
        TopologyService topologyService =  new MockTopologyService();
        DeviceService mockDeviceService = new MockDeviceService();
        CodecManager codecService =  new CodecManager();
        codecService.activate();

        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(DeviceService.class, mockDeviceService)
                        .add(TopologyService.class, topologyService)
                        .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Tests the topology overview.
     */
    @Test
    public void getTopology() {
        WebTarget wt = target();
        String response = wt.path("topology").request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(4));

        assertThat(result.get("time").asLong(), is(11111L));
        assertThat(result.get("clusters").asLong(), is(2L));
        assertThat(result.get("devices").asLong(), is(6L));
        assertThat(result.get("links").asLong(), is(4L));
    }

    /**
     * Tests the clusters overview.
     */
    @Test
    public void getTopologyClusters() {
        WebTarget wt = target();
        String response = wt.path("topology/clusters").request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        JsonArray clusters = result.get("clusters").asArray();
        assertThat(clusters, notNullValue());
        assertThat(clusters.size(), is(2));
    }

    /**
     * Tests an individual cluster overview.
     */
    @Test
    public void getCluster() {
        WebTarget wt = target();
        String response = wt.path("topology/clusters/0").request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.get("id").asLong(), is(0L));
        assertThat(result.get("deviceCount").asLong(), is(2L));
        assertThat(result.get("linkCount").asLong(), is(1L));
        assertThat(result.get("root").asString(), containsString("rootnode"));

        assertThat(result.names(), hasSize(4));
    }

    /**
     * Tests an individual cluster's devices list.
     */
    @Test
    public void getClusterDevices() {
        WebTarget wt = target();
        String response = wt.path("topology/clusters/0/devices").request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        JsonArray devices = result.get("devices").asArray();
        assertThat(devices.size(), is(2));

        assertThat(devices.get(0).asString(), is("of:dev1"));
        assertThat(devices.get(1).asString(), is("of:dev2"));
    }

    /**
     * Tests an individual cluster's links list.
     */
    @Test
    public void getClusterLinks() {
        WebTarget wt = target();
        String response = wt.path("topology/clusters/1/links").request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        JsonArray links = result.get("links").asArray();
        assertThat(links.size(), is(3));

        JsonObject link0 = links.get(0).asObject();
        JsonObject src0 = link0.get("src").asObject();
        String device0 = src0.get("device").asString();
        assertThat(device0, is("of:src1"));

        JsonObject link2 = links.get(2).asObject();
        JsonObject src2 = link2.get("src").asObject();
        String device2 = src2.get("device").asString();
        assertThat(device2, is("of:src3"));
    }

    /**
     * Tests a broadcast query.
     */
    @Test
    public void getBroadcast() {
        WebTarget wt = target();
        String response = wt.path("topology/broadcast/dev1:1").request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.get("broadcast").asBoolean(), is(true));
    }

    /**
     * Tests an infrastructure query.
     */
    @Test
    public void getInfrastructure() {
        WebTarget wt = target();
        String response = wt.path("topology/infrastructure/dev2:1").request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
        assertThat(result.get("infrastructure").asBoolean(), is(true));
    }
}
