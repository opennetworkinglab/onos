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
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.segmentrouting.SegmentRoutingService;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link SegmentRoutingAppConfig}.
 */
public class SegmentRoutingAppConfigTest {
    private static final ApplicationId APP_ID =
            new TestApplicationId(SegmentRoutingService.SR_APP_ID);

    private SegmentRoutingAppConfig config;
    private MacAddress routerMac1;
    private MacAddress routerMac2;
    private MacAddress routerMac3;

    /**
     * Initialize test related variables.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        String jsonString = "{" +
                "\"vRouterMacs\" : [" +
                "    \"00:00:00:00:00:01\"," +
                "    \"00:00:00:00:00:02\"" +
                "]}";

        routerMac1 = MacAddress.valueOf("00:00:00:00:00:01");
        routerMac2 = MacAddress.valueOf("00:00:00:00:00:02");
        routerMac3 = MacAddress.valueOf("00:00:00:00:00:03");

        ApplicationId subject = APP_ID;
        String key = SegmentRoutingService.SR_APP_ID;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonString);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new SegmentRoutingAppConfig();
        config.init(subject, key, jsonNode, mapper, delegate);
    }

    /**
     * Tests vRouters getter.
     *
     * @throws Exception
     */
    @Test
    public void testVRouters() throws Exception {
        assertTrue(config.isValid());

        Set<MacAddress> vRouters = config.vRouterMacs();
        assertThat(vRouters.size(), is(2));
        assertTrue(vRouters.contains(routerMac1));
        assertTrue(vRouters.contains(routerMac2));
    }

    /**
     * Tests vRouters setter.
     *
     * @throws Exception
     */
    @Test
    public void testSetVRouters() throws Exception {
        ImmutableSet.Builder<MacAddress> builder = ImmutableSet.builder();
        builder.add(routerMac3);
        config.setVRouterMacs(builder.build());

        Set<MacAddress> macs = config.vRouterMacs();
        assertThat(macs.size(), is(1));
        assertTrue(macs.contains(routerMac3));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}