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
import org.onosproject.kubevirtnetworking.api.KubevirtPodEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPodListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.kubevirtnetworking.api.KubevirtPodEvent.Type.KUBEVIRT_POD_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPodEvent.Type.KUBEVIRT_POD_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPodEvent.Type.KUBEVIRT_POD_UPDATED;

/**
 * Unit tests for kubevirt pod manager.
 */
public class KubevirtPodManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_UID = "unknown_uid";
    private static final String UPDATED_UID = "updated_uid";
    private static final String UPDATED_NAME = "updated_name";

    private static final String POD_UID = "pod_uid";
    private static final String POD_NAME = "pod_name";

    private static final Pod POD = createKubevirtPod(POD_UID, POD_NAME);
    private static final Pod POD_UPDATED = createKubevirtPod(POD_UID, UPDATED_NAME);

    private final TestKubevirtPodListener testListener = new TestKubevirtPodListener();

    private KubevirtPodManager target;
    private DistributedKubevirtPodStore kubevirtPodStore;

    @Before
    public void setUp() throws Exception {
        kubevirtPodStore = new DistributedKubevirtPodStore();
        TestUtils.setField(kubevirtPodStore, "coreService", new TestCoreService());
        TestUtils.setField(kubevirtPodStore, "storageService", new TestStorageService());
        TestUtils.setField(kubevirtPodStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        kubevirtPodStore.activate();

        target = new KubevirtPodManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.kubevirtPodStore = kubevirtPodStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        kubevirtPodStore.deactivate();
        target.deactivate();
        kubevirtPodStore = null;
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

        validateEvents(KUBEVIRT_POD_CREATED, KUBEVIRT_POD_REMOVED);
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
        validateEvents(KUBEVIRT_POD_CREATED, KUBEVIRT_POD_UPDATED);
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

    private static Pod createKubevirtPod(String uid, String name) {
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

    private static class TestKubevirtPodListener implements KubevirtPodListener {
        private List<KubevirtPodEvent> events = Lists.newArrayList();

        @Override
        public void event(KubevirtPodEvent event) {
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
