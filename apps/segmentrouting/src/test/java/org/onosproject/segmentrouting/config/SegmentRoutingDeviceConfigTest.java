/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link SegmentRoutingDeviceConfig}.
 */
public class SegmentRoutingDeviceConfigTest {
    private SegmentRoutingDeviceConfig config;
    private Map<Integer, Set<Integer>> adjacencySids1;
    private Map<Integer, Set<Integer>> adjacencySids2;

    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = SegmentRoutingDeviceConfigTest.class
                .getResourceAsStream("/device.json");

        adjacencySids1 = new HashMap<>();
        Set<Integer> ports1 = new HashSet<>();
        ports1.add(2);
        ports1.add(3);
        adjacencySids1.put(100, ports1);
        Set<Integer> ports2 = new HashSet<>();
        ports2.add(4);
        ports2.add(5);
        adjacencySids1.put(200, ports2);

        adjacencySids2 = new HashMap<>();
        Set<Integer> ports3 = new HashSet<>();
        ports3.add(6);
        adjacencySids2.put(300, ports3);

        DeviceId subject = DeviceId.deviceId("of:0000000000000001");
        String key = "segmentrouting";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new SegmentRoutingDeviceConfig();
        config.init(subject, key, jsonNode, mapper, delegate);
    }

    @Test
    public void testName() throws Exception {
        assertTrue(config.name().isPresent());
        assertThat(config.name().get(), is("Leaf-R1"));
    }

    @Test
    public void testSetName() throws Exception {
        config.setName("Spine-R1");
        assertTrue(config.name().isPresent());
        assertThat(config.name().get(), is("Spine-R1"));
    }

    @Test
    public void testRouterIp() throws Exception {
        assertThat(config.routerIp(), is(IpAddress.valueOf("10.0.1.254")));
    }

    @Test
    public void testSetRouterIp() throws Exception {
        config.setRouterIp("10.0.2.254");
        assertThat(config.routerIp(), is(IpAddress.valueOf("10.0.2.254")));
    }

    @Test
    public void testRouterMac() throws Exception {
        assertThat(config.routerMac(), is(MacAddress.valueOf("00:00:00:00:01:80")));
    }

    @Test
    public void testSetRouterMac() throws Exception {
        config.setRouterMac("00:00:00:00:02:80");
        assertThat(config.routerMac(), is(MacAddress.valueOf("00:00:00:00:02:80")));
    }

    @Test
    public void testNodeSid() throws Exception {
        assertThat(config.nodeSid(), is(101));
    }

    @Test
    public void testSetNodeSid() throws Exception {
        config.setNodeSid(200);
        assertThat(config.nodeSid(), is(200));
    }

    @Test
    public void testIsEdgeRouter() throws Exception {
        assertThat(config.isEdgeRouter(), is(true));
    }

    @Test
    public void testSetIsEdgeRouter() throws Exception {
        config.setIsEdgeRouter(false);
        assertThat(config.isEdgeRouter(), is(false));
    }

    @Test
    public void testAdjacencySids() throws Exception {
        assertThat(config.adjacencySids(), is(adjacencySids1));
    }

    @Test
    public void testSetAdjacencySids() throws Exception {
        config.setAdjacencySids(adjacencySids2);
        assertThat(config.adjacencySids(), is(adjacencySids2));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}