/*
 * Copyright 2016 Open Networking Laboratory
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
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.TestApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests for class {@link SegmentRoutingAppConfig}.
 */
public class SegmentRoutingAppConfigTest {
    private static final ApplicationId APP_ID =
            new TestApplicationId(SegmentRoutingManager.SR_APP_ID);

    private SegmentRoutingAppConfig config;
    private SegmentRoutingAppConfig invalidConfig;
    private static final String JSON_STRING = "{" +
            "\"vRouterMacs\" : [" +
            "    \"00:00:00:00:00:01\"," +
            "    \"00:00:00:00:00:02\"" +
            "]," +
            "\"vRouterId\" : \"of:1\"," +
            "\"excludePorts\" : [" +
            "    \"port1\"," +
            "    \"port2\"" +
            "]}";
    private static final String INVALID_JSON_STRING = "{" +
            "\"vRouterMacs\" : [" +
            "    \"00:00:00:00:00:01\"," +
            "    \"00:00:00:00:00:02\"" +
            "]," +
            "\"excludePorts\" : [" +
            "    \"port1\"," +
            "    \"port2\"" +
            "]}";
    private static final MacAddress ROUTER_MAC_1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress ROUTER_MAC_2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MacAddress ROUTER_MAC_3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final String PORT_NAME_1 = "port1";
    private static final String PORT_NAME_2 = "port2";
    private static final String PORT_NAME_3 = "port3";
    private static final DeviceId VROUTER_ID_1 = DeviceId.deviceId("of:1");
    private static final DeviceId VROUTER_ID_2 = DeviceId.deviceId("of:2");

    /**
     * Initialize test related variables.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        ApplicationId subject = APP_ID;
        String key = SegmentRoutingManager.SR_APP_ID;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(JSON_STRING);
        JsonNode invalidJsonNode = mapper.readTree(INVALID_JSON_STRING);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new SegmentRoutingAppConfig();
        config.init(subject, key, jsonNode, mapper, delegate);
        invalidConfig = new SegmentRoutingAppConfig();
        invalidConfig.init(subject, key, invalidJsonNode, mapper, delegate);
    }

    /**
     * Tests config validity.
     *
     * @throws Exception
     */
    @Test
    public void testIsValid() throws Exception {
        assertTrue(config.isValid());
        assertFalse(invalidConfig.isValid());
    }

    /**
     * Tests vRouterMacs getter.
     *
     * @throws Exception
     */
    @Test
    public void testVRouters() throws Exception {
        Set<MacAddress> vRouters = config.vRouterMacs();
        assertThat(vRouters.size(), is(2));
        assertTrue(vRouters.contains(ROUTER_MAC_1));
        assertTrue(vRouters.contains(ROUTER_MAC_2));
    }

    /**
     * Tests vRouterMacs setter.
     *
     * @throws Exception
     */
    @Test
    public void testSetVRouters() throws Exception {
        ImmutableSet.Builder<MacAddress> builder = ImmutableSet.builder();
        builder.add(ROUTER_MAC_3);
        config.setVRouterMacs(builder.build());

        Set<MacAddress> macs = config.vRouterMacs();
        assertThat(macs.size(), is(1));
        assertTrue(macs.contains(ROUTER_MAC_3));
    }

    /**
     * Tests vRouterId getter.
     *
     * @throws Exception
     */
    @Test
    public void testVRouterId() throws Exception {
        assertThat(config.vRouterId(), is(VROUTER_ID_1));
    }

    /**
     * Tests vRouterId setter.
     *
     * @throws Exception
     */
    @Test
    public void testSetVRouterId() throws Exception {
        config.setVRouterId(VROUTER_ID_2);
        assertThat(config.vRouterId(), is(VROUTER_ID_2));
    }

    /**
     * Tests excludePort getter.
     *
     * @throws Exception
     */
    @Test
    public void testExcludePorts() throws Exception {
        Set<String> excludePorts = config.excludePorts();
        assertThat(excludePorts.size(), is(2));
        assertTrue(excludePorts.contains(PORT_NAME_1));
        assertTrue(excludePorts.contains(PORT_NAME_2));
    }

    /**
     * Tests excludePort setter.
     *
     * @throws Exception
     */
    @Test
    public void testSetExcludePorts() throws Exception {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add(PORT_NAME_3);
        config.setExcludePorts(builder.build());

        Set<String> excludePorts = config.excludePorts();
        assertThat(excludePorts.size(), is(1));
        assertTrue(excludePorts.contains(PORT_NAME_3));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}