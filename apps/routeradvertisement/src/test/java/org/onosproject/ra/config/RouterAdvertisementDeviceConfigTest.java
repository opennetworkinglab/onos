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

package org.onosproject.ra.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import java.util.List;
import java.util.ArrayList;
import org.onosproject.net.host.InterfaceIpAddress;

import java.io.InputStream;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.is;

/**
 * Tests for class {@link RouterAdvertisementDeviceConfig}.
 */
public class RouterAdvertisementDeviceConfigTest {
    private RouterAdvertisementDeviceConfig config;
    private List<InterfaceIpAddress> prefixes;

    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = RouterAdvertisementDeviceConfigTest.class
                .getResourceAsStream("/device.json");

        prefixes = new ArrayList<InterfaceIpAddress>();

        prefixes.add(InterfaceIpAddress.valueOf("2001:0558:FF10:04C9::6:100/120"));
        prefixes.add(InterfaceIpAddress.valueOf("2001:0558:FF10:04C9::7:100/120"));

        DeviceId subject = DeviceId.deviceId("of:0000000000000001");
        String key = "routeradvertisement";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new RouterAdvertisementDeviceConfig();
        config.init(subject, key, jsonNode, mapper, delegate);

    }

    @Test
    public void testIsValid() {
        assertTrue(config.isValid());
    }

    @Test
    public void testPrefixes() throws Exception {
        assertThat(config.prefixes(), is(prefixes));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config configFile) {
        }
    }
}
