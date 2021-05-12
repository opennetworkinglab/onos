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
import com.google.common.collect.Sets;
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
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerListener;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerService;
import org.onosproject.kubevirtnetworking.api.KubevirtPeerRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterListener;
import org.onosproject.store.service.TestStorageService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_ASSOCIATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_DISASSOCIATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_UPDATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_EXTERNAL_NETWORK_ATTACHED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_EXTERNAL_NETWORK_DETACHED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_INTERNAL_NETWORKS_ATTACHED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_INTERNAL_NETWORKS_DETACHED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_UPDATED;

/**
 * Unit tests for kubernetes router manager.
 */
public class KubevirtRouterManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ROUTER_NAME = "router-1";
    private static final String POD_NAME = "pod-1";
    private static final String NETWORK_NAME = "flat-1";
    private static final String UPDATED_DESCRIPTION = "router-updated";
    private static final MacAddress UPDATED_MAC = MacAddress.valueOf("FF:FF:FF:FF:FF:FF");

    private static final String FLOATING_IP_ID = "fip-1";
    private static final String UNKNOWN_ID = "unknown";

    private static final KubevirtRouter ROUTER = DefaultKubevirtRouter.builder()
            .name(ROUTER_NAME)
            .description(ROUTER_NAME)
            .internal(ImmutableSet.of())
            .external(ImmutableMap.of())
            .enableSnat(true)
            .build();

    private static final KubevirtRouter ROUTER_UPDATED = DefaultKubevirtRouter.builder()
            .name(ROUTER_NAME)
            .description(UPDATED_DESCRIPTION)
            .internal(ImmutableSet.of())
            .external(ImmutableMap.of())
            .enableSnat(true)
            .build();

    private static final KubevirtRouter ROUTER_WITH_INTERNAL = DefaultKubevirtRouter.builder()
            .name(ROUTER_NAME)
            .description(ROUTER_NAME)
            .internal(ImmutableSet.of("vxlan-1", "vxlan-2"))
            .external(ImmutableMap.of())
            .enableSnat(true)
            .build();

    private static final KubevirtRouter ROUTER_WITH_SINGLE_INTERNAL = DefaultKubevirtRouter.builder()
            .name(ROUTER_NAME)
            .description(ROUTER_NAME)
            .internal(ImmutableSet.of("vxlan-1"))
            .external(ImmutableMap.of())
            .enableSnat(true)
            .build();

    private static final KubevirtRouter ROUTER_WITH_EXTERNAL = DefaultKubevirtRouter.builder()
            .name(ROUTER_NAME)
            .description(ROUTER_NAME)
            .internal(ImmutableSet.of())
            .external(ImmutableMap.of("10.10.10.10", "flat"))
            .enableSnat(true)
            .peerRouter(new KubevirtPeerRouter(IpAddress.valueOf("20.20.20.20"),
                    MacAddress.valueOf("11:22:33:44:55:66")))
            .build();

    private static final KubevirtFloatingIp FLOATING_IP_DISASSOCIATED = DefaultKubevirtFloatingIp.builder()
            .id(FLOATING_IP_ID)
            .routerName(ROUTER_NAME)
            .networkName(NETWORK_NAME)
            .floatingIp(IpAddress.valueOf("10.10.10.10"))
            .build();

    private static final KubevirtFloatingIp FLOATING_IP_ASSOCIATED = DefaultKubevirtFloatingIp.builder()
            .id(FLOATING_IP_ID)
            .routerName(ROUTER_NAME)
            .networkName(NETWORK_NAME)
            .floatingIp(IpAddress.valueOf("10.10.10.10"))
            .fixedIp(IpAddress.valueOf("20.20.20.20"))
            .podName(POD_NAME)
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
        TestUtils.setField(kubevirtRouterStore, "loadBalancerService", new TestLoadBalancerService());
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
        target.createRouter(ROUTER_WITH_EXTERNAL);

        target.updatePeerRouterMac(ROUTER_NAME, UPDATED_MAC);
        assertEquals("MAC address was not updated", UPDATED_MAC,
                target.router(ROUTER_NAME).peerRouter().macAddress());

        validateEvents(KUBEVIRT_ROUTER_CREATED, KUBEVIRT_ROUTER_UPDATED);
    }

    /**
     * Tests router's internal networks attached and detached.
     */
    @Test
    public void testRouterInternalAttachedAndDetached() {
        target.createRouter(ROUTER);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertEquals("Router internal did not match", 0, target.router(ROUTER_NAME).internal().size());

        target.updateRouter(ROUTER_WITH_INTERNAL);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertEquals("Router internal did not match", 2, target.router(ROUTER_NAME).internal().size());

        target.updateRouter(ROUTER);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertEquals("Router internal did not match", 0, target.router(ROUTER_NAME).internal().size());

        validateEvents(KUBEVIRT_ROUTER_CREATED, KUBEVIRT_ROUTER_UPDATED,
                KUBEVIRT_ROUTER_INTERNAL_NETWORKS_ATTACHED, KUBEVIRT_ROUTER_UPDATED,
                KUBEVIRT_ROUTER_INTERNAL_NETWORKS_DETACHED);
    }

    @Test
    public void testRouterInternalShrink() {
        target.createRouter(ROUTER_WITH_INTERNAL);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertEquals("Router internal did not match", 2, target.router(ROUTER_NAME).internal().size());

        target.updateRouter(ROUTER_WITH_SINGLE_INTERNAL);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertEquals("Router internal did not match", 1, target.router(ROUTER_NAME).internal().size());

        validateEvents(KUBEVIRT_ROUTER_CREATED, KUBEVIRT_ROUTER_UPDATED,
                KUBEVIRT_ROUTER_INTERNAL_NETWORKS_DETACHED);

        validateInternalRemoval(ImmutableSet.of("vxlan-2"));
    }

    /**
     * Tests router's external networks attached and detached.
     */
    @Test
    public void testRouterExternalAttachedAndDetached() {
        target.createRouter(ROUTER);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertNull(target.router(ROUTER_NAME).peerRouter());
        assertEquals(0, target.router(ROUTER_NAME).external().size());

        target.updateRouter(ROUTER_WITH_EXTERNAL);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertNotNull(target.router(ROUTER_NAME).peerRouter());
        assertEquals(1, target.router(ROUTER_NAME).external().size());

        target.updateRouter(ROUTER);
        assertEquals("Number of router did not match", 1, target.routers().size());
        assertNull(target.router(ROUTER_NAME).peerRouter());
        assertEquals(0, target.router(ROUTER_NAME).external().size());

        validateEvents(KUBEVIRT_ROUTER_CREATED, KUBEVIRT_ROUTER_UPDATED,
                KUBEVIRT_ROUTER_EXTERNAL_NETWORK_ATTACHED, KUBEVIRT_ROUTER_UPDATED,
                KUBEVIRT_ROUTER_EXTERNAL_NETWORK_DETACHED);
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

    /**
     * Tests if getting all floating IPs returns the correct set of floating IPs.
     */
    @Test
    public void testGetFloatingIps() {
        createBasicFloatingIpDisassociated();
        assertEquals("Number of floating IPs did not match", 1, target.floatingIps().size());
    }

    /**
     * Tests if getting a floating IP with ID returns the correct floating IP.
     */
    @Test
    public void testGetFloatingIpById() {
        createBasicFloatingIpDisassociated();
        assertNotNull("Floating IP did not match", target.floatingIp(FLOATING_IP_ID));
        assertNull("Floating IP did not match", target.floatingIp(UNKNOWN_ID));
    }

    /**
     * Tests if getting a floating IP with POD name returns the correct floating IP.
     */
    @Test
    public void testGetFloatingIpByPodName() {
        createBasicFloatingIpAssociated();
        assertNotNull("Floating IP did not match", target.floatingIpByPodName(POD_NAME));
        assertNull("Floating IP did not match", target.floatingIpByPodName(UNKNOWN_ID));
    }

    /**
     * Tests if getting floating IPs with router name returns the correct floating IPs.
     */
    @Test
    public void testGetFloatingIpsByRouterName() {
        createBasicFloatingIpDisassociated();
        assertEquals("Number of floating IPs did not match", 1,
                target.floatingIpsByRouter(ROUTER_NAME).size());
    }

    /**
     * Tests creating and removing a floating IP, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveFloatingIp() {
        target.createFloatingIp(FLOATING_IP_DISASSOCIATED);
        assertEquals("Number of floating IP did not match", 1, target.floatingIps().size());
        assertNotNull("Floating IP was not created", target.floatingIp(FLOATING_IP_ID));

        target.removeFloatingIp(FLOATING_IP_ID);
        assertEquals("Number of floating IP did not match", 0, target.floatingIps().size());
        assertNull("Floating IP was not created", target.floatingIp(FLOATING_IP_ID));

        validateEvents(KUBEVIRT_FLOATING_IP_CREATED, KUBEVIRT_FLOATING_IP_REMOVED);
    }

    /**
     * Tests associating a floating IP, and checks if it triggers proper events.
     */
    @Test
    public void testAssociateFloatingIp() {
        target.createFloatingIp(FLOATING_IP_DISASSOCIATED);
        assertEquals("Number of floating IP did not match", 1, target.floatingIps().size());
        assertNotNull("Floating IP was not created", target.floatingIp(FLOATING_IP_ID));

        target.updateFloatingIp(FLOATING_IP_ASSOCIATED);
        assertEquals("Number of floating IP did not match", 1, target.floatingIps().size());
        assertNotNull("Floating IP was not created", target.floatingIp(FLOATING_IP_ID));

        validateEvents(KUBEVIRT_FLOATING_IP_CREATED, KUBEVIRT_FLOATING_IP_UPDATED,
                KUBEVIRT_FLOATING_IP_ASSOCIATED);
    }

    /**
     * Tests disassociating a floating IP, and checks if it triggers proper events.
     */
    @Test
    public void testDisassociateFloatingIp() {
        target.createFloatingIp(FLOATING_IP_ASSOCIATED);
        assertEquals("Number of floating IP did not match", 1, target.floatingIps().size());
        assertNotNull("Floating IP was not created", target.floatingIp(FLOATING_IP_ID));

        target.updateFloatingIp(FLOATING_IP_DISASSOCIATED);
        assertEquals("Number of floating IP did not match", 1, target.floatingIps().size());
        assertNotNull("Floating IP was not created", target.floatingIp(FLOATING_IP_ID));

        validateEvents(KUBEVIRT_FLOATING_IP_CREATED, KUBEVIRT_FLOATING_IP_UPDATED,
                KUBEVIRT_FLOATING_IP_DISASSOCIATED);
    }

    /**
     * Tests if creating a null floating IP fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullFloatingIp() {
        target.createFloatingIp(null);
    }

    /**
     * Tests if creating a duplicate floating IP fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateFloatingIp() {
        target.createFloatingIp(FLOATING_IP_ASSOCIATED);
        target.createFloatingIp(FLOATING_IP_DISASSOCIATED);
    }

    /**
     * Tests if removing floating IP with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveFloatingIpWithNull() {
        target.removeFloatingIp(null);
    }

    /**
     * Tests if updating an unregistered floating IP fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredFloatingIp() {
        target.updateFloatingIp(FLOATING_IP_ASSOCIATED);
    }

    private void createBasicRouters() {
        target.createRouter(ROUTER);
    }

    private void createBasicFloatingIpDisassociated() {
        target.createFloatingIp(FLOATING_IP_DISASSOCIATED);
    }

    private void createBasicFloatingIpAssociated() {
        target.createFloatingIp(FLOATING_IP_ASSOCIATED);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestKubevirtRouterListener implements KubevirtRouterListener {

        private List<KubevirtRouterEvent> events = Lists.newArrayList();
        private Set<String> internalAdded = new HashSet<>();
        private Set<String> internalRemoved = new HashSet<>();

        @Override
        public void event(KubevirtRouterEvent event) {
            events.add(event);
            if (event.type() == KUBEVIRT_ROUTER_INTERNAL_NETWORKS_ATTACHED) {
                internalAdded = event.internal();
            }
            if (event.type() == KUBEVIRT_ROUTER_INTERNAL_NETWORKS_DETACHED) {
                internalRemoved = event.internal();
            }
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

    private void validateInternalAddition(Set<String> internal) {
        assertEquals("internal addition entries", internal, testListener.internalAdded);
        testListener.internalAdded.clear();
    }

    private void validateInternalRemoval(Set<String> internal) {
        assertEquals("internal addition entries", internal, testListener.internalRemoved);
        testListener.internalRemoved.clear();
    }

    private class TestLoadBalancerService implements KubevirtLoadBalancerService {

        @Override
        public KubevirtLoadBalancer loadBalancer(String name) {
            return null;
        }

        @Override
        public Set<KubevirtLoadBalancer> loadBalancers() {
            return Sets.newHashSet();
        }

        @Override
        public void addListener(KubevirtLoadBalancerListener listener) {

        }

        @Override
        public void removeListener(KubevirtLoadBalancerListener listener) {

        }
    }
}
