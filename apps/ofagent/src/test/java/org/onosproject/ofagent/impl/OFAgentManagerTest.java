/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.ofagent.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentEvent;
import org.onosproject.ofagent.api.OFAgentListener;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.store.service.TestStorageService;

import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.ofagent.api.OFAgent.State.STARTED;
import static org.onosproject.ofagent.api.OFAgent.State.STOPPED;
import static org.onosproject.ofagent.api.OFAgentEvent.Type.*;

/**
 * Junit tests for OFAgent target.
 */
public class OFAgentManagerTest {

    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "test");
    private static final ControllerNode LOCAL_NODE =
            new DefaultControllerNode(new NodeId("local"), IpAddress.valueOf("127.0.0.1"));

    private static final Set<OFController> CONTROLLER_1 = Sets.newHashSet(
            DefaultOFController.of(
                    IpAddress.valueOf("192.168.0.3"),
                    TpPort.tpPort(6653)));

    private static final Set<OFController> CONTROLLER_2 = Sets.newHashSet(
            DefaultOFController.of(
                    IpAddress.valueOf("192.168.0.3"),
                    TpPort.tpPort(6653)),
            DefaultOFController.of(
                    IpAddress.valueOf("192.168.0.4"),
                    TpPort.tpPort(6653)));

    private static final NetworkId NETWORK_1 = NetworkId.networkId(1);
    private static final NetworkId NETWORK_2 = NetworkId.networkId(2);

    private static final TenantId TENANT_1 = TenantId.tenantId("Tenant_1");
    private static final TenantId TENANT_2 = TenantId.tenantId("Tenant_2");

    private static final OFAgent OFAGENT_1 = DefaultOFAgent.builder()
            .networkId(NETWORK_1)
            .tenantId(TENANT_1)
            .state(STOPPED)
            .build();

    private static final OFAgent OFAGENT_1_CTRL_1 = DefaultOFAgent.builder()
            .networkId(NETWORK_1)
            .tenantId(TENANT_1)
            .controllers(CONTROLLER_1)
            .state(STOPPED)
            .build();

    private static final OFAgent OFAGENT_1_CTRL_2 = DefaultOFAgent.builder()
            .networkId(NETWORK_1)
            .tenantId(TENANT_1)
            .controllers(CONTROLLER_2)
            .state(STOPPED)
            .build();

    private static final OFAgent OFAGENT_2 = DefaultOFAgent.builder()
            .networkId(NETWORK_2)
            .tenantId(TENANT_2)
            .state(STOPPED)
            .build();

    private final TestOFAgentListener testListener = new TestOFAgentListener();
    private final CoreService mockCoreService = createMock(CoreService.class);
    private final LeadershipService mockLeadershipService = createMock(LeadershipService.class);
    private final VirtualNetworkService mockVirtualNetService = createMock(VirtualNetworkService.class);
    private final ClusterService mockClusterService = createMock(ClusterService.class);

    private OFAgentManager target;
    private DistributedOFAgentStore ofAgentStore;

    @Before
    public void setUp() throws Exception {
        ofAgentStore = new DistributedOFAgentStore();
        TestUtils.setField(ofAgentStore, "coreService", createMock(CoreService.class));
        TestUtils.setField(ofAgentStore, "storageService", new TestStorageService());
        TestUtils.setField(ofAgentStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        ofAgentStore.activate();

        expect(mockCoreService.registerApplication(anyObject()))
                .andReturn(APP_ID)
                .anyTimes();
        replay(mockCoreService);

        expect(mockClusterService.getLocalNode())
                .andReturn(LOCAL_NODE)
                .anyTimes();
        replay(mockClusterService);

        expect(mockLeadershipService.runForLeadership(anyObject()))
                .andReturn(null)
                .anyTimes();
        mockLeadershipService.addListener(anyObject());
        mockLeadershipService.removeListener(anyObject());
        mockLeadershipService.withdraw(anyObject());
        replay(mockLeadershipService);

        target = new OFAgentManager();
        target.coreService = mockCoreService;
        target.leadershipService = mockLeadershipService;
        target.virtualNetService = mockVirtualNetService;
        target.clusterService = mockClusterService;
        target.ofAgentStore = ofAgentStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        ofAgentStore.deactivate();
        target.deactivate();
        ofAgentStore = null;
        target = null;
    }

    @Test
    public void testCreateAndRemoveAgent() {
        target.createAgent(OFAGENT_1);
        Set<OFAgent> agents = target.agents();
        assertEquals("OFAgent set size did not match", 1, agents.size());

        target.createAgent(OFAGENT_2);
        agents = target.agents();
        assertEquals("OFAgent set size did not match", 2, agents.size());

        target.removeAgent(NETWORK_1);
        agents = target.agents();
        assertEquals("OFAgent set size did not match", 1, agents.size());

        target.removeAgent(NETWORK_2);
        agents = target.agents();
        assertEquals("OFAgent set size did not match", 0, agents.size());

        validateEvents(OFAGENT_CREATED, OFAGENT_CREATED, OFAGENT_REMOVED, OFAGENT_REMOVED);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateNullAgent() {
        target.createAgent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateAgent() {
        target.createAgent(OFAGENT_1);
        target.createAgent(OFAGENT_1);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveNullAgent() {
        target.removeAgent(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveNotFoundAgent() {
        target.removeAgent(NETWORK_1);
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveStartedAgent() {
        target.createAgent(OFAGENT_1);
        target.startAgent(NETWORK_1);
        target.removeAgent(NETWORK_1);
    }

    @Test
    public void testStartAndStopAgent() {
        target.createAgent(OFAGENT_1);
        target.startAgent(NETWORK_1);
        OFAgent ofAgent = target.agent(NETWORK_1);
        assertEquals("OFAgent state did not match", STARTED, ofAgent.state());

        target.stopAgent(NETWORK_1);
        ofAgent = target.agent(NETWORK_1);
        assertEquals("OFAgent state did not match", STOPPED, ofAgent.state());

        validateEvents(OFAGENT_CREATED, OFAGENT_STARTED, OFAGENT_STOPPED);
    }

    @Test
    public void testAddController() {
        target.createAgent(OFAGENT_1);
        target.updateAgent(OFAGENT_1_CTRL_1);
        OFAgent ofAgent = target.agent(NETWORK_1);
        assertEquals("OFAgent controller did not match", CONTROLLER_1, ofAgent.controllers());

        target.updateAgent(OFAGENT_1_CTRL_2);
        ofAgent = target.agent(NETWORK_1);
        assertEquals("OFAgent controller did not match", CONTROLLER_2, ofAgent.controllers());

        validateEvents(OFAGENT_CREATED, OFAGENT_CONTROLLER_ADDED, OFAGENT_CONTROLLER_ADDED);
    }

    @Test
    public void testRemoveController() {
        target.createAgent(OFAGENT_1_CTRL_2);
        target.updateAgent(OFAGENT_1_CTRL_1);
        OFAgent ofAgent = target.agent(NETWORK_1);
        assertEquals("OFAgent controller did not match", CONTROLLER_1, ofAgent.controllers());

        target.updateAgent(OFAGENT_1);
        ofAgent = target.agent(NETWORK_1);
        assertTrue("OFAgent controller did not match", ofAgent.controllers().isEmpty());

        validateEvents(OFAGENT_CREATED, OFAGENT_CONTROLLER_REMOVED, OFAGENT_CONTROLLER_REMOVED);
    }

    private void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("Number of events does not match", types.length, testListener.events.size());
        for (Event event : testListener.events) {
            assertEquals("Incorrect event received", types[i], event.type());
            i++;
        }
        testListener.events.clear();
    }

    private static class TestOFAgentListener implements OFAgentListener {

        private List<OFAgentEvent> events = Lists.newArrayList();

        @Override
        public void event(OFAgentEvent event) {
            events.add(event);
        }
    }
}
