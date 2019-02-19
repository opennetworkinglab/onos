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
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snetworking.api.K8sEndpointsEvent;
import org.onosproject.k8snetworking.api.K8sEndpointsListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.k8snetworking.api.K8sEndpointsEvent.Type.K8S_ENDPOINTS_CREATED;
import static org.onosproject.k8snetworking.api.K8sEndpointsEvent.Type.K8S_ENDPOINTS_REMOVED;
import static org.onosproject.k8snetworking.api.K8sEndpointsEvent.Type.K8S_ENDPOINTS_UPDATED;

/**
 * Unit tests for kubernetes endpoints manager.
 */
public class K8sEndpointsManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_UID = "unknown_uid";
    private static final String UPDATED_UID = "updated_uid";
    private static final String UPDATED_NAME = "updated_name";

    private static final String ENDPOINTS_UID = "endpoints_uid";
    private static final String ENDPOINTS_NAME = "endpoints_name";

    private static final Endpoints ENDPOINTS = createK8sEndpoints(ENDPOINTS_UID, ENDPOINTS_NAME);
    private static final Endpoints ENDPOINTS_UPDATED =
            createK8sEndpoints(ENDPOINTS_UID, UPDATED_NAME);

    private final TestK8sEndpointsListener testListener = new TestK8sEndpointsListener();

    private K8sEndpointsManager target;
    private DistributedK8sEndpointsStore k8sEndpointsStore;

    @Before
    public void setUp() throws Exception {
        k8sEndpointsStore = new DistributedK8sEndpointsStore();
        TestUtils.setField(k8sEndpointsStore, "coreService", new TestCoreService());
        TestUtils.setField(k8sEndpointsStore, "storageService", new TestStorageService());
        TestUtils.setField(k8sEndpointsStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        k8sEndpointsStore.activate();

        target = new K8sEndpointsManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.k8sEndpointsStore = k8sEndpointsStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        k8sEndpointsStore.deactivate();
        target.deactivate();
        k8sEndpointsStore = null;
        target = null;
    }

    /**
     * Tests if getting all endpoints return correct set of endpoints.
     */
    @Test
    public void testGetEndpoints() {
        createBasicEndpoints();
        assertEquals("Number of endpoints did not match", 1, target.endpointses().size());
    }

    /**
     * Tests if getting a endpoints with UID returns the correct endpoints.
     */
    @Test
    public void testGetEndpointsByUid() {
        createBasicEndpoints();
        assertNotNull("Endpoints did not match", target.endpoints(ENDPOINTS_UID));
        assertNull("Endpoints did not match", target.endpoints(UNKNOWN_UID));
    }

    /**
     * Tests creating and removing a endpoints, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveEndpoints() {
        target.createEndpoints(ENDPOINTS);
        assertEquals("Number of endpoints did not match", 1, target.endpointses().size());
        assertNotNull("Endpoint was not created", target.endpoints(ENDPOINTS_UID));

        target.removeEndpoints(ENDPOINTS_UID);
        assertEquals("Number of endpoints did not match", 0, target.endpointses().size());
        assertNull("Endpoint was not removed", target.endpoints(ENDPOINTS_UID));

        validateEvents(K8S_ENDPOINTS_CREATED, K8S_ENDPOINTS_REMOVED);
    }

    /**
     * Tests updating a endpoints, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateEndpoints() {
        target.createEndpoints(ENDPOINTS);
        assertEquals("Number of endpoints did not match", 1, target.endpointses().size());
        assertEquals("Endpoint did not match", ENDPOINTS_NAME,
                target.endpoints(ENDPOINTS_UID).getMetadata().getName());

        target.updateEndpoints(ENDPOINTS_UPDATED);

        assertEquals("Number of endpoints did not match", 1, target.endpointses().size());
        assertEquals("Endpoints did not match", UPDATED_NAME,
                target.endpoints(ENDPOINTS_UID).getMetadata().getName());
        validateEvents(K8S_ENDPOINTS_CREATED, K8S_ENDPOINTS_UPDATED);
    }

    /**
     * Tests if creating a null endpoints fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullEndpoints() {
        target.createEndpoints(null);
    }

    /**
     * Tests if creating a duplicate endpoints fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateEndpoints() {
        target.createEndpoints(ENDPOINTS);
        target.createEndpoints(ENDPOINTS);
    }

    /**
     * Tests if removing endpoints with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveEndpointsWithNull() {
        target.removeEndpoints(null);
    }

    /**
     * Tests if updating an unregistered endpoints fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredEndpoints() {
        target.updateEndpoints(ENDPOINTS);
    }

    private void createBasicEndpoints() {
        target.createEndpoints(ENDPOINTS);
    }

    private static Endpoints createK8sEndpoints(String uid, String name) {
        ObjectMeta meta = new ObjectMeta();
        meta.setUid(uid);
        meta.setName(name);

        Endpoints endpoints = new Endpoints();
        endpoints.setApiVersion("v1");
        endpoints.setKind("endpoints");
        endpoints.setMetadata(meta);

        return endpoints;
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestK8sEndpointsListener implements K8sEndpointsListener {
        private List<K8sEndpointsEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sEndpointsEvent event) {
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
