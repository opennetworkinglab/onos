/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.impl;

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
import org.onosproject.kubevirtnode.api.DefaultKubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.State.DISCONNECTED;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent.Type.KUBEVIRT_API_CONFIG_CREATED;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent.Type.KUBEVIRT_API_CONFIG_REMOVED;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.endpoint;


/**
 * Unit tests for KubeVirt API config manager.
 */
public class KubevirtApiConfigManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ERR_SIZE = "Number of configs did not match";
    private static final String ERR_NOT_MATCH = "Config did not match";
    private static final String ERR_NOT_FOUND = "Config did not exist";

    private KubevirtApiConfig apiConfig1;
    private KubevirtApiConfig apiConfig2;

    private final TestKubevirtApiConfigListener testListener = new TestKubevirtApiConfigListener();

    private KubevirtApiConfigManager target;
    private DistributedKubevirtApiConfigStore configStore;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        apiConfig1 = DefaultKubevirtApiConfig.builder()
                .scheme(KubevirtApiConfig.Scheme.HTTP)
                .ipAddress(IpAddress.valueOf("10.10.10.2"))
                .port(6443)
                .state(DISCONNECTED)
                .datacenterId("BD")
                .clusterId("BD-MEH-CT01")
                .build();
        apiConfig2 = DefaultKubevirtApiConfig.builder()
                .scheme(KubevirtApiConfig.Scheme.HTTP)
                .ipAddress(IpAddress.valueOf("10.10.10.3"))
                .port(6443)
                .state(DISCONNECTED)
                .datacenterId("BD")
                .clusterId("BD-MEH-CT01")
                .build();

        configStore = new DistributedKubevirtApiConfigStore();
        TestUtils.setField(configStore, "coreService", new TestCoreService());
        TestUtils.setField(configStore, "storageService", new TestStorageService());
        TestUtils.setField(configStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        configStore.activate();

        target = new KubevirtApiConfigManager();
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
        assertNotNull(target.apiConfig());

        target.removeApiConfig(endpoint(apiConfig1));
        assertNull(target.apiConfig());

        validateEvents(KUBEVIRT_API_CONFIG_CREATED, KUBEVIRT_API_CONFIG_REMOVED);
    }

    /**
     * Checks if the stored config is unique or not.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConfigUniqueness() {
        target.createApiConfig(apiConfig1);
        target.createApiConfig(apiConfig2);
        validateEvents(KUBEVIRT_API_CONFIG_CREATED);
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

    private static class TestKubevirtApiConfigListener implements KubevirtApiConfigListener {
        private List<KubevirtApiConfigEvent> events = Lists.newArrayList();

        @Override
        public void event(KubevirtApiConfigEvent event) {
            events.add(event);
        }
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
