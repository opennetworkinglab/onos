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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPortListener;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_UPDATED;

/**
 * Unit tests for kubernetes network manager.
 */
public class KubevirtPortManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_ID = "unknown_id";
    private static final String NETWORK_ID = "network_id";
    private static final String UPDATED_ID = "updated_id";

    private static final String PORT_MAC = "00:11:22:33:44:55";
    private static final KubevirtPort PORT = DefaultKubevirtPort.builder()
            .vmName("test-vm-1")
            .networkId(NETWORK_ID)
            .deviceId(DeviceId.deviceId("dev-1"))
            .ipAddress(IpAddress.valueOf("20.20.20.20"))
            .macAddress(MacAddress.valueOf("00:11:22:33:44:55"))
            .portNumber(PortNumber.portNumber("1"))
            .build();
    private static final KubevirtPort PORT_UPDATED = DefaultKubevirtPort.builder()
            .vmName("test-vm-1")
            .networkId(UPDATED_ID)
            .deviceId(DeviceId.deviceId("dev-1"))
            .ipAddress(IpAddress.valueOf("20.20.20.20"))
            .macAddress(MacAddress.valueOf("00:11:22:33:44:55"))
            .portNumber(PortNumber.portNumber("1"))
            .build();

    private final TestKubevirtPortListener testListener = new TestKubevirtPortListener();

    private KubevirtPortManager target;
    private DistributedKubevirtPortStore kubevirtPortStore;

    @Before
    public void setUp() throws Exception {
        kubevirtPortStore = new DistributedKubevirtPortStore();
        TestUtils.setField(kubevirtPortStore, "coreService", new TestCoreService());
        TestUtils.setField(kubevirtPortStore, "storageService", new TestStorageService());
        TestUtils.setField(kubevirtPortStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        kubevirtPortStore.activate();

        target = new KubevirtPortManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.kubevirtPortStore = kubevirtPortStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        kubevirtPortStore.deactivate();
        target.deactivate();
        kubevirtPortStore = null;
        target = null;
    }

    /**
     * Tests if getting all ports returns correct set of values.
     */
    @Test
    public void testGetPorts() {
        createBasicPorts();
        assertEquals("Number of port did not match", 1, target.ports().size());
    }

    /**
     * Tests if getting a port with network ID returns correct set of values.
     */
    @Test
    public void testGetPortsByNetworkId() {
        createBasicPorts();
        assertEquals("Number of port did not match", 1, target.ports(NETWORK_ID).size());
        assertEquals("Number of port did not match", 0, target.ports(UNKNOWN_ID).size());
    }

    /**
     * Tests if getting a port with ID returns correct value.
     */
    @Test
    public void testGetPortById() {
        createBasicPorts();
        assertNotNull("Port did not match", target.port(MacAddress.valueOf(PORT_MAC)));
    }

    /**
     * Tests creating and removing a port, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndRemovePort() {
        target.createPort(PORT);
        assertEquals("Number of port did not match", 1, target.ports().size());
        assertNotNull("Port was not created", target.port(MacAddress.valueOf(PORT_MAC)));

        target.removePort(MacAddress.valueOf(PORT_MAC));
        assertEquals("Number of port did not match", 0, target.ports().size());
        assertNull("Port was not created", target.port(MacAddress.valueOf(PORT_MAC)));

        validateEvents(KUBEVIRT_PORT_CREATED, KUBEVIRT_PORT_REMOVED);
    }

    /**
     * Tests creating and updating a port, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndUpdatePort() {
        target.createPort(PORT);
        assertEquals("Number of port did not match", 1, target.ports().size());

        target.updatePort(PORT_UPDATED);

        assertEquals("Number of port did not match", 1, target.ports().size());
        assertEquals("Port did not match", UPDATED_ID,
                target.port(MacAddress.valueOf(PORT_MAC)).networkId());

        validateEvents(KUBEVIRT_PORT_CREATED, KUBEVIRT_PORT_UPDATED);
    }

    /**
     * Tests if creating a null port fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullPort() {
        target.createPort(null);
    }

    /**
     * Tests if creating a duplicate port fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createDuplicatePort() {
        target.createPort(PORT);
        target.createPort(PORT);
    }

    /**
     * Tests if updating an unregistered port fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredPort() {
        target.updatePort(PORT);
    }

    /**
     * Tests if updating a null port fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNullPort() {
        target.updatePort(null);
    }

    private void createBasicPorts() {
        target.createPort(PORT);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestKubevirtPortListener implements KubevirtPortListener {

        private List<KubevirtPortEvent> events = Lists.newArrayList();

        @Override
        public void event(KubevirtPortEvent event) {
            events.add(event);
        }
    }

    private void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("Number of events did not match", types.length, testListener.events.size());
        for (Event event : testListener.events) {
            assertEquals("Incorrect event received", types[i], event.type());
            i++;
        }
        testListener.events.clear();
    }
}
