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
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.storekey.XConnectStoreKey;
import java.io.InputStream;
import java.util.Set;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for class {@link XConnectConfig}.
 */
public class XConnectConfigTest {
    private static final DeviceId DEV1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV2 = DeviceId.deviceId("of:0000000000000002");
    private static final VlanId VLAN10 = VlanId.vlanId((short) 10);
    private static final VlanId VLAN20 = VlanId.vlanId((short) 20);
    private static final PortNumber PORT3 = PortNumber.portNumber(3);
    private static final PortNumber PORT4 = PortNumber.portNumber(4);
    private static final PortNumber PORT5 = PortNumber.portNumber(5);
    private static final XConnectStoreKey KEY1 = new XConnectStoreKey(DEV1, VLAN10);
    private static final XConnectStoreKey KEY2 = new XConnectStoreKey(DEV2, VLAN10);
    private static final XConnectStoreKey KEY3 = new XConnectStoreKey(DEV2, VLAN20);
    private static final XConnectStoreKey KEY4 = new XConnectStoreKey(DEV2, VlanId.NONE);

    private XConnectConfig config;
    private XConnectConfig invalidConfig;

    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = SegmentRoutingAppConfigTest.class
                .getResourceAsStream("/xconnect.json");
        InputStream invalidJsonStream = SegmentRoutingAppConfigTest.class
                .getResourceAsStream("/xconnect-invalid.json");

        String key = SegmentRoutingManager.APP_NAME;
        ApplicationId subject = new TestApplicationId(key);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        JsonNode invalidJsonNode = mapper.readTree(invalidJsonStream);
        ConfigApplyDelegate delegate = new XConnectConfigTest.MockDelegate();

        config = new XConnectConfig();
        config.init(subject, key, jsonNode, mapper, delegate);
        invalidConfig = new XConnectConfig();
        invalidConfig.init(subject, key, invalidJsonNode, mapper, delegate);
    }

    /**
     * Tests config validity.
     */
    @Test
    public void testIsValid() {
        assertTrue(config.isValid());
        assertFalse(invalidConfig.isValid());
    }

    /**
     * Tests getXconnects.
     */
    @Test
    public void testGetXconnects() {
        Set<XConnectStoreKey> xconnects = config.getXconnects();
        assertThat(xconnects.size(), is(3));
        assertTrue(xconnects.contains(KEY1));
        assertTrue(xconnects.contains(KEY2));
        assertTrue(xconnects.contains(KEY3));
        assertFalse(xconnects.contains(KEY4));
    }

    /**
     * Tests getPorts.
     */
    @Test
    public void testGetPorts() {
        Set<PortNumber> ports;

        ports = config.getPorts(KEY1);
        assertThat(ports.size(), is(2));
        assertTrue(ports.contains(PORT3));
        assertTrue(ports.contains(PORT4));

        ports = config.getPorts(KEY2);
        assertThat(ports.size(), is(2));
        assertTrue(ports.contains(PORT3));
        assertTrue(ports.contains(PORT4));

        ports = config.getPorts(KEY3);
        assertThat(ports.size(), is(2));
        assertTrue(ports.contains(PORT4));
        assertTrue(ports.contains(PORT5));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}