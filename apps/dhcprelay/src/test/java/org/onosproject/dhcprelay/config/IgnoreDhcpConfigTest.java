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
 */

package org.onosproject.dhcprelay.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.dhcprelay.DhcpRelayManager.DHCP_RELAY_APP;

public class IgnoreDhcpConfigTest {
    private static final String CONFIG_FILE_PATH = "dhcp-relay.json";
    private static final ApplicationId APP_ID = new TestApplicationId("DhcpRelayTest");
    private static final DeviceId DEV_1_ID = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV_2_ID = DeviceId.deviceId("of:0000000000000002");
    private static final VlanId IGNORED_VLAN = VlanId.vlanId("100");
    @Test
    public void testIgnoredDhcpConfig() throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.readTree(Resources.getResource(CONFIG_FILE_PATH));
        IgnoreDhcpConfig config = new IgnoreDhcpConfig();
        json = json.path("apps").path(DHCP_RELAY_APP).path(IgnoreDhcpConfig.KEY);
        config.init(APP_ID, IgnoreDhcpConfig.KEY, json, om, null);

        assertEquals(2, config.ignoredVlans().size());
        Collection<VlanId> vlanForDev1 = config.ignoredVlans().get(DEV_1_ID);
        Collection<VlanId> vlanForDev2 = config.ignoredVlans().get(DEV_2_ID);

        assertEquals(1, vlanForDev1.size());
        assertEquals(1, vlanForDev2.size());

        assertTrue(vlanForDev1.contains(IGNORED_VLAN));
        assertTrue(vlanForDev2.contains(IGNORED_VLAN));
    }
}
