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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent.Type.KUBEVIRT_LOAD_BALANCER_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent.Type.KUBEVIRT_LOAD_BALANCER_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent.Type.KUBEVIRT_LOAD_BALANCER_UPDATED;

/**
 * Unit tests for kubernetes load balancer manager.
 */
public class KubevirtLoadBalancerManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String LB_NAME = "lb-1";
    private static final String NETWORK_NAME = "vxlan-1";
    private static final String UPDATED_DESCRIPTION = "lb-updated";
    private static final String UNKNOWN_ID = "unknown";

    private static final KubevirtLoadBalancer LB = DefaultKubevirtLoadBalancer.builder()
            .name(LB_NAME)
            .description(LB_NAME)
            .networkId(NETWORK_NAME)
            .vip(IpAddress.valueOf("10.10.10.10"))
            .members(ImmutableSet.of())
            .rules(ImmutableSet.of())
            .build();

    private static final KubevirtLoadBalancer LB_UPDATED = DefaultKubevirtLoadBalancer.builder()
            .name(LB_NAME)
            .description(UPDATED_DESCRIPTION)
            .networkId(NETWORK_NAME)
            .vip(IpAddress.valueOf("10.10.10.10"))
            .members(ImmutableSet.of())
            .rules(ImmutableSet.of())
            .build();

    private static final KubevirtLoadBalancer LB_WITH_MEMBERS = DefaultKubevirtLoadBalancer.builder()
            .name(LB_NAME)
            .description(LB_NAME)
            .networkId(NETWORK_NAME)
            .vip(IpAddress.valueOf("10.10.10.10"))
            .members(ImmutableSet.of(IpAddress.valueOf("10.10.10.11"),
                    IpAddress.valueOf("10.10.10.12")))
            .rules(ImmutableSet.of())
            .build();

    private final TestKubevirtLoadBalancerListener testListener = new TestKubevirtLoadBalancerListener();

    private KubevirtLoadBalancerManager target;
    private DistributedKubevirtLoadBalancerStore lbStore;

    @Before
    public void setUp() throws Exception {
        lbStore = new DistributedKubevirtLoadBalancerStore();
        TestUtils.setField(lbStore, "coreService", new TestCoreService());
        TestUtils.setField(lbStore, "storageService", new TestStorageService());
        TestUtils.setField(lbStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        lbStore.activate();

        target = new KubevirtLoadBalancerManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.kubevirtLoadBalancerStore = lbStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        lbStore.deactivate();
        target.deactivate();
        lbStore = null;
        target = null;
    }

    /**
     * Tests if getting all load balancers returns the correct set of load balancers.
     */
    @Test
    public void testGetLoadBalancers() {
        createBasicLoadBalancers();
        assertEquals("Number of load balancers did not match", 1, target.loadBalancers().size());
    }

    /**
     * Tests if getting a load balancer with ID returns the correct load balancer.
     */
    @Test
    public void testGetLoadBalancerByName() {
        createBasicLoadBalancers();
        assertNotNull("Load balancer did not match", target.loadBalancer(LB_NAME));
        assertNull("Load balancer did not match", target.loadBalancer(UNKNOWN_ID));
    }

    /**
     * Tests creating and removing a load balancer, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveLoadBalancer() {
        target.createLoadBalancer(LB);
        assertEquals("Number of load balancers did not match", 1, target.loadBalancers().size());
        assertNotNull("Load balancer was not created", target.loadBalancer(LB_NAME));

        target.removeLoadBalancer(LB_NAME);
        assertEquals("Number of load balancers did not match", 0, target.loadBalancers().size());
        assertNull("Load balancer was not removed", target.loadBalancer(LB_NAME));

        validateEvents(KUBEVIRT_LOAD_BALANCER_CREATED, KUBEVIRT_LOAD_BALANCER_REMOVED);
    }

    /**
     * Tests updating a load balancer, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateLoadBalancer() {
        target.createLoadBalancer(LB);
        assertEquals("Number of load balancers did not match", 1, target.loadBalancers().size());
        assertEquals("Load balancer did not match", LB_NAME, target.loadBalancer(LB_NAME).name());

        target.updateLoadBalancer(LB_UPDATED);
        assertEquals("Number of load balancers did not match", 1, target.loadBalancers().size());
        assertEquals("Load balancer did not match", LB_NAME, target.loadBalancer(LB_NAME).name());

        validateEvents(KUBEVIRT_LOAD_BALANCER_CREATED, KUBEVIRT_LOAD_BALANCER_UPDATED);
    }

    /**
     * Tests if creating a null load balancer fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullLoadBalancer() {
        target.createLoadBalancer(null);
    }

    /**
     * Tests if creating a duplicate load balancer fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateLoadBalancer() {
        target.createLoadBalancer(LB);
        target.createLoadBalancer(LB);
    }

    /**
     * Tests if removing load balancer with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveLoadBalancerWithNull() {
        target.removeLoadBalancer(null);
    }

    /**
     * Tests if updating an unregistered load balancer fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredLoadBalancer() {
        target.updateLoadBalancer(LB);
    }

    private void createBasicLoadBalancers() {
        target.createLoadBalancer(LB);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestKubevirtLoadBalancerListener implements KubevirtLoadBalancerListener {

        private List<KubevirtLoadBalancerEvent> events = Lists.newArrayList();

        @Override
        public void event(KubevirtLoadBalancerEvent event) {
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
