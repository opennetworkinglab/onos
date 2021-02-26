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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtPeerRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_UPDATED;

/**
 * Unit tests for kubernetes router manager.
 */
public class KubevirtRouterManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ROUTER_NAME = "router-1";
    private static final String UPDATED_DESCRIPTION = "router-updated";

    private static final MacAddress UPDATED_MAC = MacAddress.valueOf("FF:FF:FF:FF:FF:FF");

    private static final KubevirtRouter ROUTER = DefaultKubevirtRouter.builder()
            .name(ROUTER_NAME)
            .description(ROUTER_NAME)
            .internal(ImmutableSet.of("vxlan-1", "vxlan-2"))
            .external(ImmutableMap.of("10.10.10.10", "flat"))
            .enableSnat(true)
            .peerRouter(new KubevirtPeerRouter(IpAddress.valueOf("20.20.20.20"),
                    MacAddress.valueOf("11:22:33:44:55:66")))
            .build();

    private static final KubevirtRouter ROUTER_UPDATED = DefaultKubevirtRouter.builder()
            .name(ROUTER_NAME)
            .description(UPDATED_DESCRIPTION)
            .internal(ImmutableSet.of("vxlan-1", "vxlan-2"))
            .external(ImmutableMap.of("10.10.10.10", "flat"))
            .enableSnat(true)
            .peerRouter(new KubevirtPeerRouter(IpAddress.valueOf("20.20.20.20"),
                    MacAddress.valueOf("11:22:33:44:55:66")))
            .build();

    private final TestKubevirtRouterListener testListener = new TestKubevirtRouterListener();

    private KubevirtRouterManager target;
    private DistributedKubevirtRouterStore kubevirtRouterStore;

    @Before
    public void setUp() throws Exception {
        kubevirtRouterStore = new DistributedKubevirtRouterStore();
        TestUtils.setField(kubevirtRouterStore, "coreService", new TestCoreService());
        TestUtils.setField(kubevirtRouterStore, "storageService", new TestStorageService());
        TestUtils.setField(kubevirtRouterStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        kubevirtRouterStore.activate();

        target = new KubevirtRouterManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.kubevirtRouterStore = kubevirtRouterStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        kubevirtRouterStore.deactivate();
        target.deactivate();
        kubevirtRouterStore = null;
        target = null;
    }

    /**
     * Tests if getting all routers returns correct set of values.
     */
    @Test
    public void testGetRouters() {
        createBasicRouters();
        assertEquals("Number of router did not match", 1, target.routers().size());
    }

    /**
     * Tests if getting a router with name returns correct value.
     */
    @Test
    public void testGetRouterByName() {
        createBasicRouters();
        assertNotNull("Router did not match", target.router(ROUTER_NAME));
    }

    /**
     * Tests creating and removing a router, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndRemoveRouter() {
        target.createRouter(ROUTER);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertNotNull("Router was not created", target.router(ROUTER_NAME));

        target.removeRouter(ROUTER_NAME);
        assertEquals("Number of router did not match", 0, target.routers().size());
        assertNull("Router was not created", target.router(ROUTER_NAME));

        validateEvents(KUBEVIRT_ROUTER_CREATED, KUBEVIRT_ROUTER_REMOVED);
    }

    /**
     * Tests creating and updating a port, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndUpdateRouter() {
        target.createRouter(ROUTER);
        assertEquals("Number of router did not match", 1, target.routers().size());

        target.updateRouter(ROUTER_UPDATED);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertEquals("Router did not match", UPDATED_DESCRIPTION, target.router(ROUTER_NAME).description());

        validateEvents(KUBEVIRT_ROUTER_CREATED, KUBEVIRT_ROUTER_UPDATED);
    }

    /**
     * Tests updating peer router MAC address.
     */
    @Test
    public void testPeerRouterMacUpdate() {
        target.createRouter(ROUTER);

        target.updatePeerRouterMac(ROUTER_NAME, UPDATED_MAC);
        assertEquals("MAC address was not updated", UPDATED_MAC,
                target.router(ROUTER_NAME).peerRouter().macAddress());

        validateEvents(KUBEVIRT_ROUTER_CREATED, KUBEVIRT_ROUTER_UPDATED);
    }

    /**
     * Tests if creating a null router fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullRouter() {
        target.createRouter(null);
    }

    /**
     * Tests if creating a duplicate router fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createDuplicateRouter() {
        target.createRouter(ROUTER);
        target.createRouter(ROUTER);
    }

    /**
     * Tests if updating an unregistered router fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredRouter() {
        target.updateRouter(ROUTER);
    }

    /**
     * Tests if updating a null router fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNullRouter() {
        target.updateRouter(null);
    }

    private void createBasicRouters() {
        target.createRouter(ROUTER);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestKubevirtRouterListener implements KubevirtRouterListener {

        private List<KubevirtRouterEvent> events = Lists.newArrayList();

        @Override
        public void event(KubevirtRouterEvent event) {
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
