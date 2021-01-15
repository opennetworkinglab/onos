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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent.Type.K8S_NETWORK_POLICY_CREATED;
import static org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent.Type.K8S_NETWORK_POLICY_REMOVED;
import static org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent.Type.K8S_NETWORK_POLICY_UPDATED;

/**
 * Unit tests for kubernetes network policy manager.
 */
public class K8sNetworkPolicyManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_UID = "unknown_uid";
    private static final String UPDATED_UID = "updated_uid";
    private static final String UPDATED_NAME = "updated_name";

    private static final String NETWORK_POLICY_UID = "network_policy_uid";
    private static final String NETWORK_POLICY_NAME = "network_policy_name";

    private static final NetworkPolicy NETWORK_POLICY =
            createK8sNetworkPolicy(NETWORK_POLICY_UID, NETWORK_POLICY_NAME);
    private static final NetworkPolicy NETWORK_POLICY_UPDATED =
            createK8sNetworkPolicy(NETWORK_POLICY_UID, UPDATED_NAME);

    private final TestK8sNetworkPolicyListener testListener = new TestK8sNetworkPolicyListener();

    private K8sNetworkPolicyManager target;
    private DistributedK8sNetworkPolicyStore k8sNetworkPolicyStore;

    @Before
    public void setUp() throws Exception {
        k8sNetworkPolicyStore = new DistributedK8sNetworkPolicyStore();
        TestUtils.setField(k8sNetworkPolicyStore, "coreService", new TestCoreService());
        TestUtils.setField(k8sNetworkPolicyStore, "storageService", new TestStorageService());
        TestUtils.setField(k8sNetworkPolicyStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        k8sNetworkPolicyStore.activate();

        target = new K8sNetworkPolicyManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.k8sNetworkPolicyStore = k8sNetworkPolicyStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        k8sNetworkPolicyStore.deactivate();
        target.deactivate();
        k8sNetworkPolicyStore = null;
        target = null;
    }

    /**
     * Tests if getting all network policies return correct set of network policies.
     */
    @Test
    public void testGetNetworkPolicies() {
        createBasicNetworkPolicies();
        assertEquals("Number of network policies did not match", 1, target.networkPolicies().size());
    }

    /**
     * Tests if getting a network policy with UID returns the correct network policy.
     */
    @Test
    public void testGetNetworkPolicyByUid() {
        createBasicNetworkPolicies();
        assertNotNull("Network policy did not match", target.networkPolicy(NETWORK_POLICY_UID));
        assertNull("Network policy did not match", target.networkPolicy(UNKNOWN_UID));
    }

    /**
     * Tests creating and removing a network policy, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveNetworkPolicy() {
        target.createNetworkPolicy(NETWORK_POLICY);
        assertEquals("Number of network policies did not match", 1, target.networkPolicies().size());
        assertNotNull("Network policy was not created", target.networkPolicy(NETWORK_POLICY_UID));

        target.removeNetworkPolicy(NETWORK_POLICY_UID);
        assertEquals("Number of network policies did not match", 0, target.networkPolicies().size());
        assertNull("Network policy was not removed", target.networkPolicy(NETWORK_POLICY_UID));
        validateEvents(K8S_NETWORK_POLICY_CREATED, K8S_NETWORK_POLICY_REMOVED);
    }

    /**
     * Tests updating a network policy, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateNetworkPolicy() {
        target.createNetworkPolicy(NETWORK_POLICY);
        assertEquals("Number of network policies did not match", 1, target.networkPolicies().size());
        assertEquals("Network policy did not match", NETWORK_POLICY_NAME,
                target.networkPolicy(NETWORK_POLICY_UID).getMetadata().getName());

        target.updateNetworkPolicy(NETWORK_POLICY_UPDATED);
        assertEquals("Number of network policies did not match", 1, target.networkPolicies().size());
        assertEquals("Network policy did not match", UPDATED_NAME,
                target.networkPolicy(NETWORK_POLICY_UID).getMetadata().getName());
        validateEvents(K8S_NETWORK_POLICY_CREATED, K8S_NETWORK_POLICY_UPDATED);
    }

    /**
     * Tests if creating a null network policy fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullNetworkPolicy() {
        target.createNetworkPolicy(null);
    }

    /**
     * Tests if creating a duplicate network policies fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicatedNetworkPolicy() {
        target.createNetworkPolicy(NETWORK_POLICY);
        target.createNetworkPolicy(NETWORK_POLICY);
    }

    /**
     * Tests if removing network policy with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNetworkPolicyWithNull() {
        target.removeNetworkPolicy(null);
    }

    /**
     * Tests if updating an unregistered network policy fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredNetworkPolicy() {
        target.updateNetworkPolicy(NETWORK_POLICY);
    }

    private void createBasicNetworkPolicies() {
        target.createNetworkPolicy(NETWORK_POLICY);
    }

    private static NetworkPolicy createK8sNetworkPolicy(String uid, String name) {
        ObjectMeta meta = new ObjectMeta();
        meta.setUid(uid);
        meta.setName(name);

        NetworkPolicy networkPolicy = new NetworkPolicy();
        networkPolicy.setApiVersion("v1");
        networkPolicy.setKind("NetworkPolicy");
        networkPolicy.setMetadata(meta);

        return networkPolicy;
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestK8sNetworkPolicyListener implements K8sNetworkPolicyListener {
        private List<K8sNetworkPolicyEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sNetworkPolicyEvent event) {
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
