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
import org.onosproject.snmp.SnmpException;
import org.snmp4j.TransportMapping;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * Test class for V3SnmpConfiguration.
 */
public class V3SnmpConfigurationTest {

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
    private final String securityName = "sdnonos";
    private final String authProtocol = "SHA";
    private final String authPassword = "sdn@1234";
    private final String privPassword = "sdn@1234";
    private final String contextName = "sdn-context";

    private static final String SLASH = "/";

    private DefaultSnmpv3Device defaultSnmpv3Device;
    private V3SnmpConfiguration v3SnmpConfiguration;

    @Before
    public void setUp() throws Exception {
        JsonNode jsonNode = mapper.readTree(jsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();
        config.init(deviceId, KEY, jsonNode, mapper, delegate);
        defaultSnmpv3Device = new DefaultSnmpv3Device(config);

        v3SnmpConfiguration = V3SnmpConfiguration.builder()
                .setAddress(defaultSnmpv3Device.getSnmpHost())
                .setSecurityName(defaultSnmpv3Device.getSecurityName())
                .setSecurityLevel(defaultSnmpv3Device.getSecurityLevel())
                .setAuthenticationProtocol(defaultSnmpv3Device.getAuthProtocol())
                .setAuthenticationPassword(defaultSnmpv3Device.getAuthPassword())
                .setPrivacyProtocol(defaultSnmpv3Device.getPrivProtocol())
                .setPrivacyPassword(defaultSnmpv3Device.getPrivPassword())
                .setContextName(defaultSnmpv3Device.getContextName())
                .build();

        v3SnmpConfiguration.setPort(defaultSnmpv3Device.getSnmpPort());
    }

    /**
     * Test snmp create target exception case.
     *
     * @throws IOException
     */
    @Test(expected = SnmpException.class)
    public void testCreateTargetException() throws IOException {
        Address targetAddress = GenericAddress.parse(
                defaultSnmpv3Device.getProtocol() +
                        ":" + defaultSnmpv3Device.getSnmpHost() +
                        "/" + defaultSnmpv3Device.getSnmpPort());

        TransportMapping transport = new DefaultUdpTransportMapping();
        v3SnmpConfiguration.createSnmpSession(transport);
        v3SnmpConfiguration.createTarget(targetAddress);

    }

    /**
     * Test fetching security level.
     */
    @Test
    public void testGetSecurityLevel() {
        assertEquals(SecurityLevel.AUTH_PRIV, v3SnmpConfiguration.getSecurityLevel().getSnmpValue());
    }

    /**
     * Test fetching snmp host address.
     */
    @Test
    public void testGetAddress() {
        assertEquals(snmpHost, v3SnmpConfiguration.getAddress().toString());
    }

    /**
     * Test fetching security name.
     */
    @Test
    public void testGetSecurityName() {
        assertEquals(securityName, v3SnmpConfiguration.getSecurityName());
    }

    /**
     * Test fetching authentication password.
     */
    @Test
    public void testGetAuthenticationPassword() {
        assertEquals(authPassword, v3SnmpConfiguration.getAuthenticationPassword());
    }

    /**
     * Test fetching authentication protocol.
     */
    @Test
    public void testGetAuthenticationProtocol() {
        assertEquals(AuthSHA.ID, v3SnmpConfiguration.getAuthenticationProtocol());
    }

    /**
     * Test fetching privacy password.
     */
    @Test
    public void testGetPrivacyPassword() {
        assertEquals(privPassword, v3SnmpConfiguration.getPrivacyPassword());
    }

    /**
     * Test fetching privacy protocol.
     */
    @Test
    public void testGetPrivacyProtocol() {
        assertEquals(PrivAES128.ID, v3SnmpConfiguration.getPrivacyProtocol());
    }

    /**
     * Test fetching context name.
     */
    @Test
    public void testGetContextName() {
        assertEquals(contextName, v3SnmpConfiguration.getContextName());
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {

        }
    }

}
