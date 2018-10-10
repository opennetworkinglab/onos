/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onosproject.dhcprelay.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.onosproject.dhcprelay.DhcpRelayManager.DHCP_RELAY_APP;

/**
 * Tests for DHCP relay app configuration.
 */
public class DhcpRelayConfigTest {
    private static final String CONFIG_FILE_PATH = "dhcp-relay.json";
    private static final String INVALID_CONFIG_FILE_PATH = "invalid-dhcp-relay.json";
    private static final ApplicationId APP_ID = new TestApplicationId("DhcpRelayTest");
    private static final ConnectPoint DEFAULT_CONNECT_POINT = ConnectPoint.deviceConnectPoint("of:0000000000000002/2");
    private static final Ip4Address DEFAULT_SERVER_IP = Ip4Address.valueOf("172.168.10.2");
    private static final Ip4Address DEFAULT_GATEWAY_IP = Ip4Address.valueOf("192.168.10.254");
    private static final Ip6Address DEFAULT_SERVER_IP_V6 = Ip6Address.valueOf("2000::200:1");
    private static final Ip6Address DEFAULT_GATEWAY_IP_V6 = Ip6Address.valueOf("1000::100:1");
    private static final ConnectPoint INDIRECT_CONNECT_POINT = ConnectPoint.deviceConnectPoint("of:0000000000000002/3");
    private static final Ip4Address INDIRECT_SERVER_IP = Ip4Address.valueOf("172.168.10.3");

    @Test
    public void testDefaultConfig() throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.readTree(Resources.getResource(CONFIG_FILE_PATH));
        DefaultDhcpRelayConfig config = new DefaultDhcpRelayConfig();
        json = json.path("apps").path(DHCP_RELAY_APP).path(DefaultDhcpRelayConfig.KEY);
        config.init(APP_ID, DefaultDhcpRelayConfig.KEY, json, om, null);

        assertEquals(1, config.dhcpServerConfigs().size());
        DhcpServerConfig serverConfig = config.dhcpServerConfigs().get(0);
        assertEquals(DEFAULT_CONNECT_POINT, serverConfig.getDhcpServerConnectPoint().orElse(null));
        assertEquals(DEFAULT_SERVER_IP, serverConfig.getDhcpServerIp4().orElse(null));
        assertEquals(DEFAULT_GATEWAY_IP, serverConfig.getDhcpGatewayIp4().orElse(null));
        assertEquals(DEFAULT_SERVER_IP_V6, serverConfig.getDhcpServerIp6().orElse(null));
        assertEquals(DEFAULT_GATEWAY_IP_V6, serverConfig.getDhcpGatewayIp6().orElse(null));
    }

    @Test
    public void testIndirectConfig() throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.readTree(Resources.getResource(CONFIG_FILE_PATH));
        IndirectDhcpRelayConfig config = new IndirectDhcpRelayConfig();
        json = json.path("apps").path(DHCP_RELAY_APP).path(IndirectDhcpRelayConfig.KEY);
        config.init(APP_ID, IndirectDhcpRelayConfig.KEY, json, om, null);

        assertEquals(1, config.dhcpServerConfigs().size());
        DhcpServerConfig serverConfig = config.dhcpServerConfigs().get(0);
        assertEquals(INDIRECT_CONNECT_POINT, serverConfig.getDhcpServerConnectPoint().orElse(null));
        assertEquals(INDIRECT_SERVER_IP, serverConfig.getDhcpServerIp4().orElse(null));
        assertNull(serverConfig.getDhcpGatewayIp4().orElse(null));
        assertNull(serverConfig.getDhcpServerIp6().orElse(null));
        assertNull(serverConfig.getDhcpGatewayIp6().orElse(null));
    }

    @Test
    public void testInvalidConfig() throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.readTree(Resources.getResource(INVALID_CONFIG_FILE_PATH));
        DefaultDhcpRelayConfig config = new DefaultDhcpRelayConfig();
        json = json.path("apps").path(DHCP_RELAY_APP).path(DefaultDhcpRelayConfig.KEY);
        config.init(APP_ID, DefaultDhcpRelayConfig.KEY, json, om, null);
        assertFalse(config.isValid());
    }
}
