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
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snetworking.api.K8sNamespaceEvent;
import org.onosproject.k8snetworking.api.K8sNamespaceListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.k8snetworking.api.K8sNamespaceEvent.Type.K8S_NAMESPACE_CREATED;
import static org.onosproject.k8snetworking.api.K8sNamespaceEvent.Type.K8S_NAMESPACE_REMOVED;
import static org.onosproject.k8snetworking.api.K8sNamespaceEvent.Type.K8S_NAMESPACE_UPDATED;

/**
 * Unit tests for kubernetes network policy manager.
 */
public class K8sNamespaceManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_UID = "unknown_uid";
    private static final String UPDATED_UID = "updated_uid";
    private static final String UPDATED_NAME = "updated_name";

    private static final String NAMESPACE_UID = "namespace_uid";
    private static final String NAMESPACE_NAME = "namespace_name";

    private static final Namespace NAMESPACE =
            createK8sNamespace(NAMESPACE_UID, NAMESPACE_NAME);
    private static final Namespace NAMESPACE_UPDATED =
            createK8sNamespace(NAMESPACE_UID, UPDATED_NAME);

    private final TestK8sNamespaceListener testListener = new TestK8sNamespaceListener();

    private K8sNamespaceManager target;
    private DistributedK8sNamespaceStore k8sNamespaceStore;

    @Before
    public void setUp() throws Exception {
        k8sNamespaceStore = new DistributedK8sNamespaceStore();
        TestUtils.setField(k8sNamespaceStore, "coreService", new TestCoreService());
        TestUtils.setField(k8sNamespaceStore, "storageService", new TestStorageService());
        TestUtils.setField(k8sNamespaceStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        k8sNamespaceStore.activate();

        target = new K8sNamespaceManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.k8sNamespaceStore = k8sNamespaceStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        k8sNamespaceStore.deactivate();
        target.deactivate();
        k8sNamespaceStore = null;
        target = null;
    }

    /**
     * Tests if getting all namespaces return correct set of namespaces.
     */
    @Test
    public void testGetNamespaces() {
        createBasicNamespaces();
        assertEquals("Number of namespaces did not match", 1, target.namespaces().size());
    }

    /**
     * Tests if getting a namespace with UID returns the correct namespace.
     */
    @Test
    public void testGetNamespaceByUid() {
        createBasicNamespaces();
        assertNotNull("Namespace did not match", target.namespace(NAMESPACE_UID));
        assertNull("Namespace did not match", target.namespace(UNKNOWN_UID));
    }

    /**
     * Tests creating and removing a namespace, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveNetworkPolicy() {
        target.createNamespace(NAMESPACE);
        assertEquals("Number of namespaces did not match", 1, target.namespaces().size());
        assertNotNull("Namespace was not created", target.namespace(NAMESPACE_UID));

        target.removeNamespace(NAMESPACE_UID);
        assertEquals("Number of namespaces did not match", 0, target.namespaces().size());
        assertNull("Namespace was not removed", target.namespace(NAMESPACE_UID));
        validateEvents(K8S_NAMESPACE_CREATED, K8S_NAMESPACE_REMOVED);
    }

    /**
     * Tests updating a namespace, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateNamespace() {
        target.createNamespace(NAMESPACE);
        assertEquals("Number of namespaces did not match", 1, target.namespaces().size());
        assertEquals("Namespace did not match", NAMESPACE_NAME,
                target.namespace(NAMESPACE_UID).getMetadata().getName());

        target.updateNamespace(NAMESPACE_UPDATED);
        assertEquals("Number of namespaces did not match", 1, target.namespaces().size());
        assertEquals("Namespace did not match", UPDATED_NAME,
                target.namespace(NAMESPACE_UID).getMetadata().getName());
        validateEvents(K8S_NAMESPACE_CREATED, K8S_NAMESPACE_UPDATED);
    }

    /**
     * Tests if creating a null namespace fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullNamespace() {
        target.createNamespace(null);
    }

    /**
     * Tests if creating a duplicate namespaces fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicatedNamespace() {
        target.createNamespace(NAMESPACE);
        target.createNamespace(NAMESPACE);
    }

    /**
     * Tests if removing namespace with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNamespaceWithNull() {
        target.removeNamespace(null);
    }

    /**
     * Tests if updating an unregistered namespace fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredNamespace() {
        target.updateNamespace(NAMESPACE);
    }

    private void createBasicNamespaces() {
        target.createNamespace(NAMESPACE);
    }

    private static Namespace createK8sNamespace(String uid, String name) {
        ObjectMeta meta = new ObjectMeta();
        meta.setUid(uid);
        meta.setName(name);

        Namespace namespace = new Namespace();
        namespace.setApiVersion("v1");
        namespace.setKind("Namespace");
        namespace.setMetadata(meta);

        return namespace;
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestK8sNamespaceListener implements K8sNamespaceListener {
        private List<K8sNamespaceEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sNamespaceEvent event) {
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
