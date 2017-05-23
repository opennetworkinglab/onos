/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.vpls;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.api.VplsOperation;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;
import static org.onlab.junit.TestTools.delay;

/**
 * Tests for {@link VplsOperationManager}.
 */
public class VplsOperationManagerTest extends VplsTest {

    VplsOperationManager vplsOperationManager;
    private static final int OPERATION_DELAY = 1000;
    private static final int OPERATION_DURATION = 1500;

    @Before
    public void setup() {
        MockIdGenerator.cleanBind();
        vplsOperationManager = new VplsOperationManager();
        vplsOperationManager.coreService = new TestCoreService();
        vplsOperationManager.intentService = new TestIntentService();
        vplsOperationManager.leadershipService = new TestLeadershipService();
        vplsOperationManager.clusterService = new ClusterServiceAdapter();
        vplsOperationManager.hostService = new TestHostService();
        vplsOperationManager.vplsStore = new TestVplsStore();
        vplsOperationManager.isLeader = true;
        vplsOperationManager.activate();
    }

    @After
    public void tearDown() {
        MockIdGenerator.unbind();
        vplsOperationManager.deactivate();
    }

    /**
     * Sends leadership event to the manager and checks if the manager is
     * leader or not.
     */
    @Test
    public void testLeadershipEvent() {
        vplsOperationManager.isLeader = false;
        vplsOperationManager.localNodeId = NODE_ID_1;

        // leader changed to self
        Leader leader = new Leader(NODE_ID_1, 0, 0);
        Leadership leadership = new Leadership(APP_NAME, leader, ImmutableList.of());
        LeadershipEvent event = new LeadershipEvent(LeadershipEvent.Type.LEADER_CHANGED, leadership);
        ((TestLeadershipService) vplsOperationManager.leadershipService).sendEvent(event);
        assertTrue(vplsOperationManager.isLeader);

        // leader changed to other
        leader = new Leader(NODE_ID_2, 0, 0);
        leadership = new Leadership(APP_NAME, leader, ImmutableList.of());
        event = new LeadershipEvent(LeadershipEvent.Type.LEADER_CHANGED, leadership);
        ((TestLeadershipService) vplsOperationManager.leadershipService).sendEvent(event);
        assertFalse(vplsOperationManager.isLeader);
    }

    /**
     * Submits an ADD operation to the operation manager; check if the VPLS
     * store changed after a period.
     */
    @Test
    public void testSubmitAddOperation() {
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));

        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.ADD);

        vplsOperationManager.submit(vplsOperation);
        assertAfter(OPERATION_DELAY, OPERATION_DURATION, () -> {
            Collection<VplsData> vplss = vplsOperationManager.vplsStore.getAllVpls();
            assertEquals(1, vplss.size());
            VplsData result = vplss.iterator().next();

            assertEquals(vplsData, result);
            assertEquals(VplsData.VplsState.ADDED, result.state());

            Set<Intent> intentsInstalled =
                    Sets.newHashSet(vplsOperationManager.intentService.getIntents());
            assertEquals(4, intentsInstalled.size());
        });
    }

    /**
     * Submits an ADD operation to the operation manager; check the VPLS state
     * from store if Intent install failed.
     */
    @Test
    public void testSubmitAddOperationFail() {
        vplsOperationManager.intentService = new AlwaysFailureIntentService();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));

        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.ADD);
        vplsOperationManager.submit(vplsOperation);
        assertAfter(OPERATION_DELAY, OPERATION_DURATION, () -> {
            Collection<VplsData> vplss = vplsOperationManager.vplsStore.getAllVpls();
            assertEquals(1, vplss.size());
            VplsData result = vplss.iterator().next();

            assertEquals(vplsData, result);
            assertEquals(VplsData.VplsState.FAILED, result.state());
        });
    }

    /**
     * Submits an REMOVE operation to the operation manager; check if the VPLS
     * store changed after a period.
     */
    @Test
    public void testSubmitRemoveOperation() {
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsData.state(VplsData.VplsState.REMOVING);

        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.REMOVE);

        vplsOperationManager.submit(vplsOperation);

        assertAfter(OPERATION_DELAY, OPERATION_DURATION, () -> {
            Collection<VplsData> vplss = vplsOperationManager.vplsStore.getAllVpls();
            assertEquals(0, vplss.size());
        });
    }

    /**
     * Submits an UPDATE operation with VPLS interface update to the operation manager; check if the VPLS
     * store changed after a period.
     */
    @Test
    public void testSubmitUpdateOperation() {
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        vplsData.state(VplsData.VplsState.ADDED);
        vplsOperationManager.vplsStore.addVpls(vplsData);

        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsData.state(VplsData.VplsState.UPDATING);

        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.UPDATE);

        vplsOperationManager.submit(vplsOperation);

        assertAfter(OPERATION_DELAY, OPERATION_DURATION, () -> {
            Collection<VplsData> vplss = vplsOperationManager.vplsStore.getAllVpls();
            VplsData result = vplss.iterator().next();
            VplsData expected = VplsData.of(VPLS1, EncapsulationType.VLAN);
            expected.addInterfaces(ImmutableSet.of(V100H1, V100H2));
            expected.state(VplsData.VplsState.ADDED);

            assertEquals(1, vplss.size());
            assertEquals(expected, result);

            Set<Intent> intentsInstalled =
                    Sets.newHashSet(vplsOperationManager.intentService.getIntents());
            assertEquals(4, intentsInstalled.size());
        });
    }

    /**
     * Submits an UPDATE operation with VPLS host update to the operation manager; check if the VPLS
     * store changed after a period.
     */
    @Test
    public void testSubmitUpdateHostOperation() {
        vplsOperationManager.hostService = new EmptyHostService();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));

        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.ADD);
        vplsOperationManager.submit(vplsOperation);
        delay(1000);
        vplsOperationManager.hostService = new TestHostService();

        vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsData.state(VplsData.VplsState.UPDATING);

        vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.UPDATE);

        vplsOperationManager.submit(vplsOperation);

        assertAfter(OPERATION_DELAY, OPERATION_DURATION, () -> {
            Collection<VplsData> vplss = vplsOperationManager.vplsStore.getAllVpls();
            VplsData result = vplss.iterator().next();
            VplsData expected = VplsData.of(VPLS1);
            expected.addInterfaces(ImmutableSet.of(V100H1, V100H2));
            expected.state(VplsData.VplsState.ADDED);
            assertEquals(1, vplss.size());
            assertEquals(expected, result);

            assertEquals(4, vplsOperationManager.intentService.getIntentCount());
        });
    }

    /**
     * Submits same operation twice to the manager; the manager should ignore
     * duplicated operation.
     */
    @Test
    public void testDuplicateOperationInQueue() {
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));

        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.ADD);

        vplsOperationManager.submit(vplsOperation);
        vplsOperationManager.submit(vplsOperation);
        Deque<VplsOperation> opQueue = vplsOperationManager.pendingVplsOperations.get(VPLS1);
        assertEquals(1, opQueue.size());

        // Clear operation queue before scheduler process it
        opQueue.clear();
    }

    /**
     * Submits REMOVE operation after submits ADD operation; there should be no
     * pending or running operation in the manager.
     */
    @Test
    public void testDoNothingOperation() {
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));

        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.ADD);
        vplsOperationManager.submit(vplsOperation);
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.REMOVE);
        vplsOperationManager.submit(vplsOperation);
        assertAfter(OPERATION_DELAY, OPERATION_DURATION, () -> {
            assertEquals(0, vplsOperationManager.pendingVplsOperations.size());

            // Should not have any running operation
            assertEquals(0, vplsOperationManager.runningOperations.size());
        });
    }

    /**
     * Optimize operations which don't need to be optimized.
     */
    @Test
    public void testOptimizeOperationsNoOptimize() {
        // empty queue
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsOperation vplsOperation =
                VplsOperationManager.getOptimizedVplsOperation(operations);
        assertNull(vplsOperation);

        // one operation
        VplsData vplsData = VplsData.of(VPLS1);
        vplsOperation = VplsOperation.of(vplsData, VplsOperation.Operation.ADD);
        operations.add(vplsOperation);
        VplsOperation result =
                VplsOperationManager.getOptimizedVplsOperation(operations);
        assertEquals(vplsOperation, result);

    }

    /**
     * Optimize operations with first is ADD operation and last is also ADD
     * operation.
     */
    @Test
    public void testOptimizeOperationsAToA() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.ADD);
        operations.add(vplsOperation);
        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.ADD);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        assertEquals(VplsOperation.of(vplsData, VplsOperation.Operation.ADD), vplsOperation);
    }

    /**
     * Optimize operations with first is ADD operation and last is REMOVE
     * operation.
     */
    @Test
    public void testOptimizeOperationsAToR() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.ADD);
        operations.add(vplsOperation);
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.REMOVE);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        assertNull(vplsOperation);
    }

    /**
     * Optimize operations with first is ADD operation and last is UPDATE
     * operation.
     */
    @Test
    public void testOptimizeOperationsAToU() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.ADD);
        operations.add(vplsOperation);
        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.UPDATE);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        assertEquals(VplsOperation.of(vplsData, VplsOperation.Operation.ADD), vplsOperation);
    }

    /**
     * Optimize operations with first is REMOVE operation and last is ADD
     * operation.
     */
    @Test
    public void testOptimizeOperationsRToA() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.REMOVE);
        operations.add(vplsOperation);
        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.ADD);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        assertEquals(VplsOperation.of(vplsData, VplsOperation.Operation.UPDATE), vplsOperation);
    }

    /**
     * Optimize operations with first is REMOVE operation and last is also
     * REMOVE operation.
     */
    @Test
    public void testOptimizeOperationsRToR() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.REMOVE);
        operations.add(vplsOperation);
        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.REMOVE);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        assertEquals(VplsOperation.of(vplsData, VplsOperation.Operation.REMOVE), vplsOperation);
    }

    /**
     * Optimize operations with first is REMOVE operation and last is UPDATE
     * operation.
     */
    @Test
    public void testOptimizeOperationsRToU() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.REMOVE);
        operations.add(vplsOperation);
        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.UPDATE);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        assertEquals(VplsOperation.of(vplsData, VplsOperation.Operation.UPDATE), vplsOperation);
    }

    /**
     * Optimize operations with first is UPDATE operation and last is ADD
     * operation.
     */
    @Test
    public void testOptimizeOperationsUToA() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.UPDATE);
        operations.add(vplsOperation);
        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.ADD);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        assertEquals(VplsOperation.of(vplsData, VplsOperation.Operation.UPDATE), vplsOperation);
    }

    /**
     * Optimize operations with first is UPDATE operation and last is REMOVE
     * operation.
     */
    @Test
    public void testOptimizeOperationsUToR() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.UPDATE);
        operations.add(vplsOperation);
        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.REMOVE);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        assertEquals(VplsOperation.of(vplsData, VplsOperation.Operation.REMOVE), vplsOperation);
    }

    /**
     * Optimize operations with first is UPDATE operation and last is also
     * UPDATE operation.
     */
    @Test
    public void testOptimizeOperationsUToU() {
        Deque<VplsOperation> operations = new ArrayDeque<>();
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1));
        VplsOperation vplsOperation = VplsOperation.of(vplsData,
                                                       VplsOperation.Operation.UPDATE);
        operations.add(vplsOperation);
        vplsData = VplsData.of(VPLS1, EncapsulationType.VLAN);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        vplsOperation = VplsOperation.of(vplsData,
                                         VplsOperation.Operation.UPDATE);
        operations.add(vplsOperation);
        vplsOperation = VplsOperationManager.getOptimizedVplsOperation(operations);
        assertEquals(VplsOperation.of(vplsData, VplsOperation.Operation.UPDATE), vplsOperation);
    }

    /**
     * Test Intent service which always fail when submit or withdraw Intents.
     */
    class AlwaysFailureIntentService extends TestIntentService {
        @Override
        public void submit(Intent intent) {
            intents.add(new IntentData(intent, IntentState.FAILED, new WallClockTimestamp()));
            if (listener != null) {
                IntentEvent event = IntentEvent.getEvent(IntentState.FAILED, intent).get();
                listener.event(event);
            }
        }

        @Override
        public void withdraw(Intent intent) {
            intents.forEach(intentData -> {
                if (intentData.intent().key().equals(intent.key())) {
                    intentData.setState(IntentState.FAILED);

                    if (listener != null) {
                        IntentEvent event = IntentEvent.getEvent(IntentState.FAILED, intent).get();
                        listener.event(event);
                    }
                }
            });
        }
    }

    /**
     * Test host service without any hosts.
     */
    class EmptyHostService extends HostServiceAdapter {
        @Override
        public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
            return ImmutableSet.of();
        }
    }

}
