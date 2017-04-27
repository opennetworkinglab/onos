/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.store.service.TestStorageService;
import org.openstack4j.model.network.Router;
import org.openstack4j.openstack.networking.domain.NeutronRouter;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.*;

/**
 * Unit tests for OpenStack router manager.
 */
public class OpenstackRouterManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_ID = "unknown_id";
    private static final String ROUTER_ID = "router_1";
    private static final String ROUTER_NAME = "router_1";
    private static final Router ROUTER = NeutronRouter.builder()
            .id(ROUTER_ID)
            .name(ROUTER_NAME)
            .build();

    private final TestOpenstackRouterListener testListener = new TestOpenstackRouterListener();

    private OpenstackRouterManager target;
    private DistributedOpenstackRouterStore osRouterStore;

    @Before
    public void setUp() throws Exception {
        osRouterStore = new DistributedOpenstackRouterStore();
        TestUtils.setField(osRouterStore, "coreService", new TestCoreService());
        TestUtils.setField(osRouterStore, "storageService", new TestStorageService());
        TestUtils.setField(osRouterStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        osRouterStore.activate();

        target = new OpenstackRouterManager();
        target.coreService = new TestCoreService();
        target.osRouterStore = osRouterStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        osRouterStore.deactivate();
        target.deactivate();
        osRouterStore = null;
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
     * Tests if getting a router with ID returns correct value.
     */
    @Test
    public void testGetRouterById() {
        createBasicRouters();
        assertTrue("Router did not exist", target.router(ROUTER_ID) != null);
        assertTrue("Router did not exist", target.router(UNKNOWN_ID) == null);
    }

    /**
     * Tests creating and removing a router, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndRemoveRouter() {
        target.createRouter(ROUTER);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertTrue("Router was not created", target.router(ROUTER_ID) != null);

        target.removeRouter(ROUTER.getId());
        assertEquals("Number of router did not match", 0, target.routers().size());
        assertTrue("Router was not removed", target.router(ROUTER_ID) == null);

        validateEvents(OPENSTACK_ROUTER_CREATED, OPENSTACK_ROUTER_REMOVED);
    }

    /**
     * Tests creating and updating a router, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndUpdateRouter() {
        target.createRouter(ROUTER);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertEquals("Router did not match", ROUTER_NAME, target.router(ROUTER_ID).getName());

        final Router updated = NeutronRouter.builder()
                .id(ROUTER_ID)
                .name("updated-name")
                .build();

        target.updateRouter(updated);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertEquals("Router did not match", "updated-name", target.router(ROUTER_ID).getName());

        validateEvents(OPENSTACK_ROUTER_CREATED, OPENSTACK_ROUTER_UPDATED);
    }

    /**
     * Tests adding and removing external gateway to a router, and checks if
     * proper events are triggered.
     */
    @Test
    public void testAddAndRemoveExternalGateway() {
        target.createRouter(ROUTER);
        assertTrue("Router did not match", target.router(ROUTER_ID).getExternalGatewayInfo() == null);

        Router updated = NeutronRouter.builder()
                .id(ROUTER_ID)
                .name(ROUTER_NAME)
                .externalGateway("test-network-id")
                .build();

        target.updateRouter(updated);
        assertEquals("Router did not match",
                "test-network-id",
                target.router(ROUTER_ID).getExternalGatewayInfo().getNetworkId());

        target.updateRouter(ROUTER);
        assertTrue("Router did not match", target.router(ROUTER_ID).getExternalGatewayInfo() == null);

        validateEvents(OPENSTACK_ROUTER_CREATED, OPENSTACK_ROUTER_UPDATED,
                OPENSTACK_ROUTER_GATEWAY_ADDED,
                OPENSTACK_ROUTER_UPDATED,
                OPENSTACK_ROUTER_GATEWAY_REMOVED);
    }

    /**
     * Tests if creating a router with null value fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateRouterWithNull() {
        target.createRouter(null);
    }

    /**
     * Tests if creating a router with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateRouterWithNullId() {
        final Router testRouter = NeutronRouter.builder()
                .id(null)
                .name(ROUTER_NAME)
                .build();
        target.createRouter(testRouter);
    }

    /**
     * Tests if updating a router with null name fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateRouterWithNullName() {
        final Router testRouter = NeutronRouter.builder()
                .id(ROUTER_ID)
                .name(null)
                .build();
        target.createRouter(testRouter);
    }

    /**
     * Tests if creating a duplicate router fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateRouter() {
        target.createRouter(ROUTER);
        target.createRouter(ROUTER);
    }

    /**
     * Tests if updating a router with null value fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateRouterWithNull() {
        target.updateRouter(null);
    }

    /**
     * Tests if updating a router with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRouterWithNullId() {
        final Router testRouter = NeutronRouter.builder()
                .id(null)
                .name(ROUTER_NAME)
                .build();
        target.updateRouter(testRouter);
    }

    /**
     * Tests if updating a router with null name fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRouterWithNullName() {
        final Router testRouter = NeutronRouter.builder()
                .id(ROUTER_ID)
                .name(null)
                .build();
        target.updateRouter(testRouter);
    }

    /**
     * Tests if updating an unregistered router fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredRouter() {
        target.updateRouter(ROUTER);
    }

    // TODO fix openstack4j floating IP data model and add unit tests

    private void createBasicRouters() {
        target.createRouter(ROUTER);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestOpenstackRouterListener implements OpenstackRouterListener {
        private List<OpenstackRouterEvent> events = Lists.newArrayList();

        @Override
        public void event(OpenstackRouterEvent event) {
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
