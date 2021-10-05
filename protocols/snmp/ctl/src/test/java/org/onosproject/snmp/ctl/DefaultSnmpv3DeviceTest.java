/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.snmp.ctl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.snmp.SnmpDeviceConfig;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * Test class for DefaultSnmpv3Device.
 */
public class DefaultSnmpv3DeviceTest {

    private final SnmpDeviceConfig config = new SnmpDeviceConfig();
    private final InputStream jsonStream = DefaultSnmpv3DeviceTest.class
            .getResourceAsStream("/device.json");
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String KEY = "snmp";
    private final String snmpHost = "1.1.1.1";
    private final int snmpPort = 1;
    private final String username = "test";
    private final String community = "test";
    private final DeviceId deviceId = DeviceId.deviceId("snmp:1.1.1.1:1");
    private final String defaultProtocol = "udp";
    private final String securityLevel = "authPriv";
    private final String securityName = "sdnonos";
    private final String authProtocol = "SHA";
    private final String authPassword = "sdn@1234";
    private final String privProtocol = "AES";
    private final String privPassword = "sdn@1234";
    private final String contextName = "sdn-context";

    private DefaultSnmpv3Device defaultSnmpv3Device;


    @Before
    public void setUp() throws Exception {
        JsonNode jsonNode = mapper.readTree(jsonStream);
        ConfigApplyDelegate delegate = new DefaultSnmpv3DeviceTest.MockDelegate();
        config.init(deviceId, KEY, jsonNode, mapper, delegate);
        defaultSnmpv3Device = new DefaultSnmpv3Device(config);
    }

    /**
     * Tests fetching snmp host.
     */
    @Test
    public void testGetSnmpHost() {
        assertEquals(snmpHost, defaultSnmpv3Device.getSnmpHost());
    }

    /**
     * Tests fetching snmp port.
     */
    @Test
    public void testGetSnmpPort() {
        assertEquals(snmpPort, defaultSnmpv3Device.getSnmpPort());
    }

    /**
     * Tests fetching username.
     */
    @Test
    public void testGetUsername() {
        assertEquals(username, defaultSnmpv3Device.getUsername());
    }

    /**
     * Tests fetching community string.
     */
    @Test
    public void testGetCommunity() {
        assertEquals(community, defaultSnmpv3Device.getCommunity());
    }

    /**
     * Tests fetching protocol.
     */
    @Test
    public void testGetProtocol() {
        assertEquals(defaultProtocol, defaultSnmpv3Device.getProtocol());
    }

    /**
     * Tests fetching security name.
     */
    @Test
    public void testGetSecurityName() {
        assertEquals(securityName, defaultSnmpv3Device.getSecurityName());
    }

    /**
     * Tests fetching security level.
     */
    @Test
    public void testGetSecurityLevel() {
        assertEquals(securityLevel, defaultSnmpv3Device.getSecurityLevel());
    }

    /**
     * Tests fetching authentication protocol.
     */
    @Test
    public void testGetAuthProtocol() {
        assertEquals(authProtocol, defaultSnmpv3Device.getAuthProtocol());
    }

    /**
     * Tests fetching authentication password.
     */
    @Test
    public void testGetAuthPassword() {
        assertEquals(authPassword, defaultSnmpv3Device.getAuthPassword());
    }

    /**
     * Tests fetching privacy protocol.
     */
    @Test
    public void testGetPrivProtocol() {
        assertEquals(privProtocol, defaultSnmpv3Device.getPrivProtocol());
    }

    /**
     * Tests fetching privacy password.
     */
    @Test
    public void testGetPrivPassword() {
        assertEquals(privPassword, defaultSnmpv3Device.getPrivPassword());
    }

    /**
     * Tests fetching context name.
     */
    @Test
    public void testGetContextName() {
        assertEquals(contextName, defaultSnmpv3Device.getContextName());
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {

        }
    }

}
