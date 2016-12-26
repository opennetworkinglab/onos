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
package org.onosproject.lisp.ctl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for LispRouterFactory.
 */
public class LispRouterFactoryTest {

    private LispRouterFactory routerFactory;
    private LispRouterAgent agent = new LispRouterAgentAdapter();

    @Before
    public void setUp() throws Exception {
        routerFactory = LispRouterFactory.getInstance();
        routerFactory.setAgent(agent);
    }

    @After
    public void tearDown() throws Exception {
        routerFactory = null;
    }

    @Test
    public void testSetAgent() throws Exception {
        routerFactory.setAgent(agent);

        assertThat(routerFactory, is(routerFactory));
    }

    @Test
    public void testGetRouterInstance() throws Exception {
        IpAddress ipAddress = IpAddress.valueOf("192.168.1.1");

        LispRouter router1 = routerFactory.getRouterInstance(ipAddress);
        LispRouter router2 = routerFactory.getRouterInstance(ipAddress);

        assertThat(router1, is(router2));
    }
}
