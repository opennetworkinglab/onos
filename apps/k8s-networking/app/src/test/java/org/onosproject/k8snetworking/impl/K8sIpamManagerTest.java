/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.store.service.TestStorageService;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for kubernetes IPAM manager.
 */
public class K8sIpamManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String NETWORK_ID = "sona-network";
    private static final IpAddress IP_ADDRESS_1 = IpAddress.valueOf("10.10.10.2");
    private static final IpAddress IP_ADDRESS_2 = IpAddress.valueOf("10.10.10.3");
    private static final Set<IpAddress> IP_ADDRESSES = ImmutableSet.of(IP_ADDRESS_1, IP_ADDRESS_2);

    private K8sIpamManager target;
    private DistributedK8sIpamStore k8sIpamStore;

    @Before
    public void setUp() throws Exception {
        k8sIpamStore = new DistributedK8sIpamStore();
        TestUtils.setField(k8sIpamStore, "coreService", new TestCoreService());
        TestUtils.setField(k8sIpamStore, "storageService", new TestStorageService());
        TestUtils.setField(k8sIpamStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        k8sIpamStore.activate();

        target = new K8sIpamManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.k8sIpamStore = k8sIpamStore;
        target.activate();
    }

    @After
    public void tearDown() {
        k8sIpamStore.deactivate();
        target.deactivate();
        k8sIpamStore = null;
        target = null;
    }

    /**
     * Tests if allocating IP address works correctly.
     */
    @Test
    public void testAllocateIp() {
        createBasicIpPool();

        assertEquals("Number of allocated IPs did not match", 0,
                target.allocatedIps(NETWORK_ID).size());
        assertEquals("Number of available IPs did not match", 2,
                target.availableIps(NETWORK_ID).size());

        IpAddress allocatedIp = target.allocateIp(NETWORK_ID);
        assertEquals("Number of allocated IPs did not match", 1,
                target.allocatedIps(NETWORK_ID).size());
        assertEquals("Number of available IPs did not match", 1,
                target.availableIps(NETWORK_ID).size());
        assertTrue("Allocated IP did not match",
                IP_ADDRESSES.contains(allocatedIp));
    }

    /**
     * Tests if releasing IP address works correctly.
     */
    @Test
    public void testReleaseIp() {
        createBasicIpPool();

        IpAddress allocatedIp1 = target.allocateIp(NETWORK_ID);
        IpAddress allocatedIp2 = target.allocateIp(NETWORK_ID);

        assertEquals("Number of allocated IPs did not match", 2,
                target.allocatedIps(NETWORK_ID).size());
        assertEquals("Number of available IPs did not match", 0,
                target.availableIps(NETWORK_ID).size());

        target.releaseIp(NETWORK_ID, allocatedIp1);

        assertEquals("Number of allocated IPs did not match", 1,
                target.allocatedIps(NETWORK_ID).size());
        assertEquals("Number of available IPs did not match", 1,
                target.availableIps(NETWORK_ID).size());

        target.releaseIp(NETWORK_ID, allocatedIp2);

        assertEquals("Number of allocated IPs did not match", 0,
                target.allocatedIps(NETWORK_ID).size());
        assertEquals("Number of available IPs did not match", 2,
                target.availableIps(NETWORK_ID).size());
    }

    private void createBasicIpPool() {
        target.initializeIpPool(NETWORK_ID, IP_ADDRESSES);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }
}
