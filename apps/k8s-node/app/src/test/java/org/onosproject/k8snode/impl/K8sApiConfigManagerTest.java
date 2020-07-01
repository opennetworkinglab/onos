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
package org.onosproject.k8snode.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snode.api.DefaultK8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigEvent;
import org.onosproject.k8snode.api.K8sApiConfigListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.k8snode.api.K8sApiConfig.State.DISCONNECTED;
import static org.onosproject.k8snode.api.K8sApiConfigEvent.Type.K8S_API_CONFIG_CREATED;
import static org.onosproject.k8snode.api.K8sApiConfigEvent.Type.K8S_API_CONFIG_REMOVED;
import static org.onosproject.k8snode.util.K8sNodeUtil.endpoint;

/**
 * Unit tests for kubernetes API config manager.
 */
public class K8sApiConfigManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ERR_SIZE = "Number of configs did not match";
    private static final String ERR_NOT_MATCH = "Config did not match";
    private static final String ERR_NOT_FOUND = "Config did not exist";

    private K8sApiConfig apiConfig1;
    private K8sApiConfig apiConfig2;
    private K8sApiConfig apiConfig3;

    private final TestK8sApiConfigListener testListener = new TestK8sApiConfigListener();

    private K8sApiConfigManager target;
    private DistributedK8sApiConfigStore configStore;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {

        apiConfig1 = DefaultK8sApiConfig.builder()
                .clusterName("kubernetes1")
                .segmentId(1)
                .mode(K8sApiConfig.Mode.NORMAL)
                .scheme(K8sApiConfig.Scheme.HTTP)
                .ipAddress(IpAddress.valueOf("10.10.10.2"))
                .port(6443)
                .state(DISCONNECTED)
                .build();
        apiConfig2 = DefaultK8sApiConfig.builder()
                .clusterName("kubernetes2")
                .segmentId(2)
                .mode(K8sApiConfig.Mode.NORMAL)
                .scheme(K8sApiConfig.Scheme.HTTPS)
                .ipAddress(IpAddress.valueOf("10.10.10.3"))
                .port(6443)
                .state(DISCONNECTED)
                .token("token")
                .caCertData("caCertData")
                .clientCertData("clientCertData")
                .clientKeyData("clientKeyData")
                .build();
        apiConfig3 = DefaultK8sApiConfig.builder()
                .clusterName("kubernetes3")
                .segmentId(3)
                .mode(K8sApiConfig.Mode.PASSTHROUGH)
                .scheme(K8sApiConfig.Scheme.HTTP)
                .ipAddress(IpAddress.valueOf("10.10.10.4"))
                .port(8080)
                .state(DISCONNECTED)
                .build();

        configStore = new DistributedK8sApiConfigStore();
        TestUtils.setField(configStore, "coreService", new TestCoreService());
        TestUtils.setField(configStore, "storageService", new TestStorageService());
        TestUtils.setField(configStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        configStore.activate();

        configStore.createApiConfig(apiConfig2);
        configStore.createApiConfig(apiConfig3);

        target = new K8sApiConfigManager();
        target.storageService = new TestStorageService();
        target.coreService = new TestCoreService();
        target.clusterService = new TestClusterService();
        target.leadershipService = new TestLeadershipService();
        target.configStore = configStore;
        target.addListener(testListener);
        target.activate();
        testListener.events.clear();
    }

    /**
     * Clean up unit test.
     */
    @After
    public void tearDown() {
        target.removeListener(testListener);
        target.deactivate();
        configStore.deactivate();
        configStore = null;
        target = null;
    }

    /**
     * Checks if creating and removing a config work well with proper events.
     */
    @Test
    public void testCreateAndRemoveConfig() {
        target.createApiConfig(apiConfig1);
        assertEquals(ERR_SIZE, 3, target.apiConfigs().size());
        assertNotNull(target.apiConfig(endpoint(apiConfig1)));

        target.removeApiConfig(endpoint(apiConfig1));
        assertEquals(ERR_SIZE, 2, target.apiConfigs().size());
        assertNull(target.apiConfig(endpoint(apiConfig1)));

        validateEvents(K8S_API_CONFIG_CREATED, K8S_API_CONFIG_REMOVED);
    }

    /**
     * Checks if creating null config fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullConfig() {
        target.createApiConfig(null);
    }

    private static class TestK8sApiConfigListener implements K8sApiConfigListener {
        private List<K8sApiConfigEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sApiConfigEvent event) {
            events.add(event);
        }
    }

    /**
     * Checks if creating a duplicated config fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateConfig() {
        target.createApiConfig(apiConfig1);
        target.createApiConfig(apiConfig1);
    }

    /**
     * Checks if removing null config fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNullConfig() {
        target.removeApiConfig(null);
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

    /**
     * Checks if updating a null config fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNullConfig() {
        target.updateApiConfig(null);
    }

    /**
     * Checks if updating not existing config fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNotExistingConfig() {
        target.updateApiConfig(apiConfig1);
    }

    /**
     * Checks if getting all nodes method returns correct set of nodes.
     */
    @Test
    public void testGetAllNodes() {
        assertEquals(ERR_SIZE, 2, target.apiConfigs().size());
        assertTrue(ERR_NOT_FOUND, target.apiConfigs().contains(apiConfig2));
        assertTrue(ERR_NOT_FOUND, target.apiConfigs().contains(apiConfig3));
    }

    private static class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private class TestClusterService extends ClusterServiceAdapter {

    }

    private static class TestLeadershipService extends LeadershipServiceAdapter {

    }
}
