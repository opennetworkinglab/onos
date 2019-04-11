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
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_CREATED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_REMOVED;
import static org.onosproject.k8snetworking.api.K8sPodEvent.Type.K8S_POD_UPDATED;

/**
 * Unit tests for kubernetes pod manager.
 */
public class K8sPodManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_UID = "unknown_uid";
    private static final String UPDATED_UID = "updated_uid";
    private static final String UPDATED_NAME = "updated_name";

    private static final String POD_UID = "pod_uid";
    private static final String POD_NAME = "pod_name";

    private static final Pod POD = createK8sPod(POD_UID, POD_NAME);
    private static final Pod POD_UPDATED = createK8sPod(POD_UID, UPDATED_NAME);

    private final TestK8sPodListener testListener = new TestK8sPodListener();

    private K8sPodManager target;
    private DistributedK8sPodStore k8sPodStore;

    @Before
    public void setUp() throws Exception {
        k8sPodStore = new DistributedK8sPodStore();
        TestUtils.setField(k8sPodStore, "coreService", new TestCoreService());
        TestUtils.setField(k8sPodStore, "storageService", new TestStorageService());
        TestUtils.setField(k8sPodStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        k8sPodStore.activate();

        target = new K8sPodManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.k8sPodStore = k8sPodStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        k8sPodStore.deactivate();
        target.deactivate();
        k8sPodStore = null;
        target = null;
    }

    /**
     * Tests if getting all pods return correct set of pods.
     */
    @Test
    public void testGetPods() {
        createBasicPods();
        assertEquals("Number of pods did not match", 1, target.pods().size());
    }

    /**
     * Tests if getting a pod with UID returns the correct pod.
     */
    @Test
    public void testGetPodByUid() {
        createBasicPods();
        assertNotNull("Pod did not match", target.pod(POD_UID));
        assertNull("Pod did not match", target.pod(UNKNOWN_UID));
    }

    /**
     * Tests creating and removing a pod, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemovePod() {
        target.createPod(POD);
        assertEquals("Number of pods did not match", 1, target.pods().size());
        assertNotNull("Pod was not created", target.pod(POD_UID));

        target.removePod(POD_UID);
        assertEquals("Number of pods did not match", 0, target.pods().size());
        assertNull("Pod was not removed", target.pod(POD_UID));

        validateEvents(K8S_POD_CREATED, K8S_POD_REMOVED);
    }

    /**
     * Tests updating a pod, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdatePod() {
        target.createPod(POD);
        assertEquals("Number of pods did not match", 1, target.pods().size());
        assertEquals("Pod did not match", POD_NAME,
                target.pod(POD_UID).getMetadata().getName());

        target.updatePod(POD_UPDATED);

        assertEquals("Number of pods did not match", 1, target.pods().size());
        assertEquals("Pod did not match", UPDATED_NAME,
                target.pod(POD_UID).getMetadata().getName());
        validateEvents(K8S_POD_CREATED, K8S_POD_UPDATED);
    }

    /**
     * Tests if creating a null pod fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullPod() {
        target.createPod(null);
    }

    /**
     * Tests if creating a duplicate pod fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicatePod() {
        target.createPod(POD);
        target.createPod(POD);
    }

    /**
     * Tests if removing pod with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemovePodWithNull() {
        target.removePod(null);
    }

    /**
     * Tests if updating an unregistered pod fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredPod() {
        target.updatePod(POD);
    }

    private void createBasicPods() {
        target.createPod(POD);
    }

    private static Pod createK8sPod(String uid, String name) {
        ObjectMeta meta = new ObjectMeta();
        meta.setUid(uid);
        meta.setName(name);

        PodStatus status = new PodStatus();
        status.setPhase("Running");

        Pod pod = new Pod();
        pod.setApiVersion("v1");
        pod.setKind("pod");
        pod.setMetadata(meta);
        pod.setStatus(status);

        return pod;
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestK8sPodListener implements K8sPodListener {
        private List<K8sPodEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sPodEvent event) {
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
