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
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.TestApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.segmentrouting.SegmentRoutingManager;

import java.io.InputStream;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Tests for class {@link SegmentRoutingAppConfig}.
 */
public class SegmentRoutingAppConfigTest {
    private SegmentRoutingAppConfig config;
    private SegmentRoutingAppConfig invalidConfig;
    private SegmentRoutingAppConfig mplsEcmpConfig;

    private static final MacAddress ROUTER_MAC_1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress ROUTER_MAC_2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MacAddress ROUTER_MAC_3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final ConnectPoint PORT_1 = ConnectPoint.deviceConnectPoint("of:1/1");
    private static final ConnectPoint PORT_2 = ConnectPoint.deviceConnectPoint("of:1/2");
    private static final ConnectPoint PORT_3 = ConnectPoint.deviceConnectPoint("of:1/3");
    private static final DeviceId VROUTER_ID_1 = DeviceId.deviceId("of:1");
    private static final DeviceId VROUTER_ID_2 = DeviceId.deviceId("of:2");
    private static final String PROVIDER_1 = "org.onosproject.provider.host";
    private static final String PROVIDER_2 = "org.onosproject.netcfghost";
    private static final String PROVIDER_3 = "org.onosproject.anotherprovider";

    /**
     * Initialize test related variables.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = SegmentRoutingAppConfigTest.class
                .getResourceAsStream("/app.json");
        InputStream invalidJsonStream = SegmentRoutingAppConfigTest.class
                .getResourceAsStream("/app-invalid.json");
        InputStream mplsEcmpJsonStream = SegmentRoutingAppConfigTest.class
                .getResourceAsStream("/app-ecmp.json");

        String key = SegmentRoutingManager.APP_NAME;
        ApplicationId subject = new TestApplicationId(key);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        JsonNode invalidJsonNode = mapper.readTree(invalidJsonStream);
        JsonNode mplsEcmpJsonNode = mapper.readTree(mplsEcmpJsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new SegmentRoutingAppConfig();
        config.init(subject, key, jsonNode, mapper, delegate);
        invalidConfig = new SegmentRoutingAppConfig();
        invalidConfig.init(subject, key, invalidJsonNode, mapper, delegate);
        mplsEcmpConfig = new SegmentRoutingAppConfig();
        mplsEcmpConfig.init(subject, key, mplsEcmpJsonNode, mapper, delegate);
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
        assertTrue(mplsEcmpConfig.isValid());
    }

    /**
     * Test MPLS-ECMP default getter. By-default
     * MPLS-ECMPS is false.
     */
    @Test
    public void testDefaultMplsEcmp() {
        boolean mplsEcmp = config.mplsEcmp();
        assertThat(mplsEcmp, is(false));
    }

    /**
     * Test MPLS-ECMP getter.
     */
    @Test
    public void testMplsEcmp() {
        boolean mplsEcmp = mplsEcmpConfig.mplsEcmp();
        assertThat(mplsEcmp, is(true));
    }

    /**
     * Test MPLS-ECMP setter.
     */
    @Test
    public void testSetMplsEcmp() {
        /*
         * In the config the value is not set.
         */
        boolean mplsEcmp = config.mplsEcmp();
        assertThat(mplsEcmp, is(false));
        config.setMplsEcmp(true);
        mplsEcmp = config.mplsEcmp();
        assertThat(mplsEcmp, is(true));
        /*
         * In the mplsEcmpConfig the value is true,
         */
        mplsEcmp = mplsEcmpConfig.mplsEcmp();
        assertThat(mplsEcmp, is(true));
        config.setMplsEcmp(false);
        mplsEcmp = config.mplsEcmp();
        assertThat(mplsEcmp, is(false));
    }

    /**
     * Tests vRouterMacs getter.
     *
     * @throws Exception
     */
    @Test
    public void testVRouterMacs() throws Exception {
        Set<MacAddress> vRouterMacs = config.vRouterMacs();
        assertNotNull("vRouterMacs should not be null", vRouterMacs);
        assertThat(vRouterMacs.size(), is(2));
        assertTrue(vRouterMacs.contains(ROUTER_MAC_1));
        assertTrue(vRouterMacs.contains(ROUTER_MAC_2));
    }

    /**
     * Tests vRouterMacs setter.
     *
     * @throws Exception
     */
    @Test
    public void testSetVRouterMacs() throws Exception {
        ImmutableSet.Builder<MacAddress> builder = ImmutableSet.builder();
        builder.add(ROUTER_MAC_3);
        config.setVRouterMacs(builder.build());

        Set<MacAddress> vRouterMacs = config.vRouterMacs();
        assertThat(vRouterMacs.size(), is(1));
        assertTrue(vRouterMacs.contains(ROUTER_MAC_3));
    }

    /**
     * Tests suppressSubnet getter.
     *
     * @throws Exception
     */
    @Test
    public void testSuppressSubnet() throws Exception {
        Set<ConnectPoint> suppressSubnet = config.suppressSubnet();
        assertNotNull("suppressSubnet should not be null", suppressSubnet);
        assertThat(suppressSubnet.size(), is(2));
        assertTrue(suppressSubnet.contains(PORT_1));
        assertTrue(suppressSubnet.contains(PORT_2));
    }

    /**
     * Tests suppressSubnet setter.
     *
     * @throws Exception
     */
    @Test
    public void testSetSuppressSubnet() throws Exception {
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        builder.add(PORT_3);
        config.setSuppressSubnet(builder.build());

        Set<ConnectPoint> suppressSubnet = config.suppressSubnet();
        assertNotNull("suppressSubnet should not be null", suppressSubnet);
        assertThat(suppressSubnet.size(), is(1));
        assertTrue(suppressSubnet.contains(PORT_3));
    }

    /**
     * Tests suppressHostByPort getter.
     *
     * @throws Exception
     */
    @Test
    public void testSuppressHostByPort() throws Exception {
        Set<ConnectPoint> suppressHostByPort = config.suppressHostByPort();
        assertNotNull("suppressHostByPort should not be null", suppressHostByPort);
        assertThat(suppressHostByPort.size(), is(2));
        assertTrue(suppressHostByPort.contains(PORT_1));
        assertTrue(suppressHostByPort.contains(PORT_2));
    }

    /**
     * Tests suppressHostByPort setter.
     *
     * @throws Exception
     */
    @Test
    public void testSetSuppressHostByPort() throws Exception {
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        builder.add(PORT_3);
        config.setSuppressHostByPort(builder.build());

        Set<ConnectPoint> suppressHostByPort = config.suppressHostByPort();
        assertNotNull("suppressHostByPort should not be null", suppressHostByPort);
        assertThat(suppressHostByPort.size(), is(1));
        assertTrue(suppressHostByPort.contains(PORT_3));
    }

    /**
     * Tests suppressHostByProvider getter.
     *
     * @throws Exception
     */
    @Test
    public void testSuppressHostByProvider() throws Exception {
        Set<String> supprsuppressHostByProvider = config.suppressHostByProvider();
        assertNotNull("suppressHostByProvider should not be null", supprsuppressHostByProvider);
        assertThat(supprsuppressHostByProvider.size(), is(2));
        assertTrue(supprsuppressHostByProvider.contains(PROVIDER_1));
        assertTrue(supprsuppressHostByProvider.contains(PROVIDER_2));
    }

    /**
     * Tests suppressHostByProvider setter.
     *
     * @throws Exception
     */
    @Test
    public void testSetHostLearning() throws Exception {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add(PROVIDER_3);
        config.setSuppressHostByProvider(builder.build());

        Set<String> supprsuppressHostByProvider = config.suppressHostByProvider();
        assertNotNull("suppressHostByProvider should not be null", supprsuppressHostByProvider);
        assertThat(supprsuppressHostByProvider.size(), is(1));
        assertTrue(supprsuppressHostByProvider.contains(PROVIDER_3));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}
