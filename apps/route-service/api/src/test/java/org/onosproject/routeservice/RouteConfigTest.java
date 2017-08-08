/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.routeservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;

import java.io.InputStream;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

/**
 * Tests for class {@link RouteConfigTest}.
 */
public class RouteConfigTest {
    private static final String KEY = "org.onosproject.routing";

    private static final IpPrefix PREFIX1 = IpPrefix.valueOf("10.0.0.1/24");
    private static final IpPrefix PREFIX2 = IpPrefix.valueOf("20.0.0.1/24");
    private static final IpPrefix PREFIX3 = IpPrefix.valueOf("30.0.0.1/24");
    private static final IpAddress NEXTHOP1 = IpAddress.valueOf("192.168.1.1");
    private static final IpAddress NEXTHOP2 = IpAddress.valueOf("192.168.2.1");
    private static final Route ROUTE1 = new Route(Route.Source.STATIC, PREFIX1, NEXTHOP1);
    private static final Route ROUTE2 = new Route(Route.Source.STATIC, PREFIX2, NEXTHOP1);
    private static final Route ROUTE3 = new Route(Route.Source.STATIC, PREFIX3, NEXTHOP2);
    private static final Set<Route> EXPECTED_ROUTES = ImmutableSet.of(ROUTE1, ROUTE2, ROUTE3);
    private static final Set<Route> UNEXPECTED_ROUTES = ImmutableSet.of(ROUTE1, ROUTE2);

    private RouteConfig config;

    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = RouteConfigTest.class
                .getResourceAsStream("/route-config.json");
        ApplicationId subject = new TestApplicationId(KEY);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new RouteConfig();
        config.init(subject, KEY, jsonNode, mapper, delegate);
    }

    @Test
    public void getRoutes() throws Exception {
        assertThat(config.getRoutes(), is(EXPECTED_ROUTES));
        assertThat(config.getRoutes(), not(UNEXPECTED_ROUTES));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}
