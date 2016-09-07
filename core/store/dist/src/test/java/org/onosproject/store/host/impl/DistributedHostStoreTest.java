/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.store.host.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests for the ECHostStore.
 */
public class DistributedHostStoreTest {

    private DistributedHostStore ecXHostStore;

    private static final HostId HOSTID = HostId.hostId(MacAddress.valueOf("1a:1a:1a:1a:1a:1a"));

    private static final IpAddress IP1 = IpAddress.valueOf("10.2.0.2");
    private static final IpAddress IP2 = IpAddress.valueOf("10.2.0.3");

    private static final ProviderId PID = new ProviderId("of", "foo");

    @Before
    public void setUp() {
        ecXHostStore = new DistributedHostStore();

        ecXHostStore.storageService = new TestStorageService();
        ecXHostStore.activate();
    }

    @After
    public void tearDown() {
        ecXHostStore.deactivate();
    }

    /**
     * Tests the removeIp method call.
     */
    @Test
    public void testRemoveIp() {
        Set<IpAddress> ips = new HashSet<>();
        ips.add(IP1);
        ips.add(IP2);

        HostDescription description = new DefaultHostDescription(HOSTID.mac(),
                                                                    HOSTID.vlanId(),
                                                                    HostLocation.NONE,
                                                                    ips);
        ecXHostStore.createOrUpdateHost(PID, HOSTID, description, false);
        ecXHostStore.removeIp(HOSTID, IP1);
        Host host = ecXHostStore.getHost(HOSTID);

        assertFalse(host.ipAddresses().contains(IP1));
        assertTrue(host.ipAddresses().contains(IP2));
    }

}
