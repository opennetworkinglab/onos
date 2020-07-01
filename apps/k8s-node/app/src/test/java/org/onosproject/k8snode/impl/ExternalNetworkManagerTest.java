/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for external network manager.
 */
public class ExternalNetworkManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private ExternalNetworkManager target;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        target = new ExternalNetworkManager();

        target.coreService = new TestCoreService();
        target.storageService = new TestStorageService();
        target.activate();
    }

    /**
     * Clean up unit test.
     */
    @After
    public void tearDown() {
        target.deactivate();
        target = null;
    }

    /**
     * Checks if creating and removing a config work well with proper events.
     */
    @Test
    public void testObtainGatewayIp() {
        IpPrefix cidr = IpPrefix.valueOf("192.168.200.0/24");
        target.registerNetwork(cidr);

        assertEquals(target.getGatewayIp(cidr), IpAddress.valueOf("192.168.200.1"));
    }

    @Test
    public void testAllocateReleaseIp() {
        IpPrefix cidr = IpPrefix.valueOf("192.168.200.0/24");
        target.registerNetwork(cidr);
        IpAddress ip = target.allocateIp(cidr);
        assertEquals(251, target.getAllIps(cidr).size());

        target.releaseIp(cidr, ip);
        assertEquals(252, target.getAllIps(cidr).size());
    }

    private static class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }
}
