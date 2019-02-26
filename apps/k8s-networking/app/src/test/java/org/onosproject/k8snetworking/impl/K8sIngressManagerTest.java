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
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snetworking.api.K8sIngressEvent;
import org.onosproject.k8snetworking.api.K8sIngressListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.k8snetworking.api.K8sIngressEvent.Type.K8S_INGRESS_CREATED;
import static org.onosproject.k8snetworking.api.K8sIngressEvent.Type.K8S_INGRESS_REMOVED;
import static org.onosproject.k8snetworking.api.K8sIngressEvent.Type.K8S_INGRESS_UPDATED;

/**
 * Unit tests for kubernetes ingress manager.
 */
public class K8sIngressManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_UID = "unknown_uid";
    private static final String UPDATED_UID = "updated_uid";
    private static final String UPDATED_NAME = "updated_name";

    private static final String INGRESS_UID = "ingress_uid";
    private static final String INGRESS_NAME = "ingress_name";

    private static final Ingress INGRESS = createK8sIngress(INGRESS_UID, INGRESS_NAME);
    private static final Ingress INGRESS_UPDATED = createK8sIngress(INGRESS_UID, UPDATED_NAME);

    private final TestK8sIngressListener testListener = new TestK8sIngressListener();

    private K8sIngressManager target;
    private DistributedK8sIngressStore k8sIngressStore;

    @Before
    public void setUp() throws Exception {
        k8sIngressStore = new DistributedK8sIngressStore();
        TestUtils.setField(k8sIngressStore, "coreService", new TestCoreService());
        TestUtils.setField(k8sIngressStore, "storageService", new TestStorageService());
        TestUtils.setField(k8sIngressStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        k8sIngressStore.activate();

        target = new K8sIngressManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.k8sIngressStore = k8sIngressStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        k8sIngressStore.deactivate();
        target.deactivate();
        k8sIngressStore = null;
        target = null;
    }

    /**
     * Tests if getting all ingresses return correct set of ingresses.
     */
    @Test
    public void testGetIngresses() {
        createBasicIngresses();
        assertEquals("Number of ingresses did not match", 1, target.ingresses().size());
    }

    /**
     * Tests if getting an ingress with UID returns the correct ingress.
     */
    @Test
    public void testGetIngressByUid() {
        createBasicIngresses();
        assertNotNull("Ingress did not match", target.ingress(INGRESS_UID));
        assertNull("Ingress did not match", target.ingress(UNKNOWN_UID));
    }

    /**
     * Tests creating and removing a ingress, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveIngress() {
        target.createIngress(INGRESS);
        assertEquals("Number of ingresses did not match", 1, target.ingresses().size());
        assertNotNull("Ingress was not created", target.ingress(INGRESS_UID));

        target.removeIngress(INGRESS_UID);
        assertEquals("Number of ingresses did not match", 0, target.ingresses().size());
        assertNull("Ingress was not removed", target.ingress(INGRESS_UID));

        validateEvents(K8S_INGRESS_CREATED, K8S_INGRESS_REMOVED);
    }

    /**
     * Tests updating a ingress, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateIngress() {
        target.createIngress(INGRESS);
        assertEquals("Number of ingresses did not match", 1, target.ingresses().size());
        assertEquals("Ingress did not match", INGRESS_NAME,
                target.ingress(INGRESS_UID).getMetadata().getName());

        target.updateIngress(INGRESS_UPDATED);

        assertEquals("Number of ingresses did not match", 1, target.ingresses().size());
        assertEquals("Ingress did not match", UPDATED_NAME,
                target.ingress(INGRESS_UID).getMetadata().getName());
        validateEvents(K8S_INGRESS_CREATED, K8S_INGRESS_UPDATED);
    }

    /**
     * Tests if creating a null ingress fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullIngress() {
        target.createIngress(null);
    }

    /**
     * Tests if creating a duplicate ingress fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateIngress() {
        target.createIngress(INGRESS);
        target.createIngress(INGRESS);
    }

    /**
     * Tests if removing ingress with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveIngressWithNull() {
        target.removeIngress(null);
    }

    /**
     * Tests if updating an unregistered ingress fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredIngress() {
        target.updateIngress(INGRESS);
    }

    private void createBasicIngresses() {
        target.createIngress(INGRESS);
    }

    private static Ingress createK8sIngress(String uid, String name) {
        ObjectMeta meta = new ObjectMeta();
        meta.setUid(uid);
        meta.setName(name);

        Ingress ingress = new Ingress();
        ingress.setApiVersion("v1");
        ingress.setKind("Ingress");
        ingress.setMetadata(meta);

        return ingress;
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestK8sIngressListener implements K8sIngressListener {
        private List<K8sIngressEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sIngressEvent event) {
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
