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
import io.fabric8.kubernetes.api.model.Service;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snetworking.api.K8sServiceEvent;
import org.onosproject.k8snetworking.api.K8sServiceListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.k8snetworking.api.K8sServiceEvent.Type.K8S_SERVICE_CREATED;
import static org.onosproject.k8snetworking.api.K8sServiceEvent.Type.K8S_SERVICE_REMOVED;
import static org.onosproject.k8snetworking.api.K8sServiceEvent.Type.K8S_SERVICE_UPDATED;

/**
 * Unit tests for kubernetes service manager.
 */
public class K8sServiceManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_UID = "unknown_uid";
    private static final String UPDATED_UID = "updated_uid";
    private static final String UPDATED_NAME = "updated_name";

    private static final String SERVICE_UID = "service_uid";
    private static final String SERVICE_NAME = "service_name_1";

    private static final Service SERVICE = createK8sService(SERVICE_UID, SERVICE_NAME);
    private static final Service SERVICE_UPDATED =
            createK8sService(SERVICE_UID, UPDATED_NAME);


    private final TestK8sServiceListener testListener = new TestK8sServiceListener();

    private K8sServiceManager target;
    private DistributedK8sServiceStore k8sServiceStore;

    @Before
    public void setUp() throws Exception {
        k8sServiceStore = new DistributedK8sServiceStore();
        TestUtils.setField(k8sServiceStore, "coreService", new TestCoreService());
        TestUtils.setField(k8sServiceStore, "storageService", new TestStorageService());
        TestUtils.setField(k8sServiceStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        k8sServiceStore.activate();

        target = new K8sServiceManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.k8sServiceStore = k8sServiceStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        k8sServiceStore.deactivate();
        target.deactivate();
        k8sServiceStore = null;
        target = null;
    }

    /**
     * Tests if getting all services return correct set of services.
     */
    @Test
    public void testGetServices() {
        createBasicServices();
        assertEquals("Number of service did not match", 1, target.services().size());
    }

    /**
     * Tests if getting a service with UID returns the correct service.
     */
    @Test
    public void testGetServiceByUid() {
        createBasicServices();
        assertNotNull("Service did not match", target.service(SERVICE_UID));
        assertNull("Service did not match", target.service(UNKNOWN_UID));
    }

    /**
     * Tests creating and removing a service, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveService() {
        target.createService(SERVICE);
        assertEquals("Number of services did not match", 1, target.services().size());
        assertNotNull("Service was not created", target.service(SERVICE_UID));

        target.removeService(SERVICE_UID);
        assertEquals("Number of services did not match", 0, target.services().size());
        assertNull("Service was not removed", target.service(SERVICE_UID));

        validateEvents(K8S_SERVICE_CREATED, K8S_SERVICE_REMOVED);
    }

    /**
     * Tests updating a service, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateService() {
        target.createService(SERVICE);
        assertEquals("Number of services did not match", 1, target.services().size());
        assertEquals("Service did not match", SERVICE_NAME,
                target.service(SERVICE_UID).getMetadata().getName());

        target.updateService(SERVICE_UPDATED);

        assertEquals("Number of services did not match", 1, target.services().size());
        assertEquals("Service did not match", UPDATED_NAME,
                target.service(SERVICE_UID).getMetadata().getName());
        validateEvents(K8S_SERVICE_CREATED, K8S_SERVICE_UPDATED);
    }

    /**
     * Tests if creating a null service fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullService() {
        target.createService(null);
    }

    /**
     * Tests if creating a duplicate service fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateService() {
        target.createService(SERVICE);
        target.createService(SERVICE);
    }

    /**
     * Tests if removing service with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveServiceWithNull() {
        target.removeService(null);
    }

    /**
     * Tests if updating an unregistered service fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredService() {
        target.updateService(SERVICE);
    }

    private void createBasicServices() {
        target.createService(SERVICE);
    }

    private static Service createK8sService(String uid, String name) {
        ObjectMeta meta = new ObjectMeta();
        meta.setUid(uid);
        meta.setName(name);

        Service service = new Service();
        service.setApiVersion("v1");
        service.setKind("service");
        service.setMetadata(meta);

        return service;
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestK8sServiceListener implements K8sServiceListener {
        private List<K8sServiceEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sServiceEvent event) {
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
