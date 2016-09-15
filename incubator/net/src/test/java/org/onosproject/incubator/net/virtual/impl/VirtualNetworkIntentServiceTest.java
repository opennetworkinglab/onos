/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.TestApplicationId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.net.intent.WorkPartitionServiceAdapter;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.store.service.TestStorageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Junit tests for VirtualNetworkIntentService.
 */
public class VirtualNetworkIntentServiceTest extends TestDeviceParams {

    private final String tenantIdValue1 = "TENANT_ID1";
    private static final ApplicationId APP_ID =
            new TestApplicationId("MyAppId");

    private ConnectPoint cp1;
    private ConnectPoint cp2;
    private ConnectPoint cp3;
    private ConnectPoint cp4;
    private ConnectPoint cp5;
    private ConnectPoint cp6;
    private VirtualLink link1;
    private VirtualLink link2;
    private VirtualLink link3;
    private VirtualLink link4;
    private VirtualLink link5;
    private VirtualLink link6;

    private VirtualNetworkManager manager;
    private static DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService;
    private TestableIntentService intentService = new FakeIntentManager();
    private VirtualNetworkIntentService vnetIntentService;
    private TestIntentCompiler compiler = new TestIntentCompiler();
    private IntentExtensionService intentExtensionService;
    private WorkPartitionService workPartitionService;
    private ServiceDirectory testDirectory;
    private TestListener listener = new TestListener();
    private IdGenerator idGenerator = new MockIdGenerator();
    private static final int MAX_WAIT_TIME = 5;
    private static final int MAX_PERMITS = 1;
    private static Semaphore created;
    private static Semaphore withdrawn;
    private static Semaphore purged;

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        coreService = new VirtualNetworkIntentServiceTest.TestCoreService();

        Intent.unbindIdGenerator(idGenerator);
        Intent.bindIdGenerator(idGenerator);

        virtualNetworkManagerStore.setCoreService(coreService);
        TestUtils.setField(coreService, "coreService", new VirtualNetworkIntentServiceTest.TestCoreService());
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
        manager.intentService = intentService;
        manager.activate();
        intentService.addListener(listener);

        // Register a compiler and an installer both setup for success.
        intentExtensionService = intentService;
        intentExtensionService.registerCompiler(VirtualNetworkIntent.class, compiler);

        created = new Semaphore(0, true);
        withdrawn = new Semaphore(0, true);
        purged = new Semaphore(0, true);

        workPartitionService = new WorkPartitionServiceAdapter();
        testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(IntentService.class, intentService)
                .add(WorkPartitionService.class, workPartitionService);
        BaseResource.setServiceDirectory(testDirectory);
    }

    @After
    public void tearDown() {
        virtualNetworkManagerStore.deactivate();
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
        Intent.unbindIdGenerator(idGenerator);
        intentService.removeListener(listener);
        created = null;
        withdrawn = null;
        purged = null;
    }

    /**
     * Method to create the virtual network for further testing.
     *
     * @return virtual network
     */
    private VirtualNetwork setupVirtualNetworkTopology() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);
        VirtualDevice virtualDevice3 =
                manager.createVirtualDevice(virtualNetwork.id(), DID3);
        VirtualDevice virtualDevice4 =
                manager.createVirtualDevice(virtualNetwork.id(), DID4);

        Port port1 = new DefaultPort(virtualDevice1, PortNumber.portNumber(1), true);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice1.id(), port1.number(), port1);
        cp1 = new ConnectPoint(virtualDevice1.id(), port1.number());

        Port port2 = new DefaultPort(virtualDevice1, PortNumber.portNumber(2), true);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice1.id(), port2.number(), port2);
        cp2 = new ConnectPoint(virtualDevice1.id(), port2.number());

        Port port3 = new DefaultPort(virtualDevice2, PortNumber.portNumber(3), true);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice2.id(), port3.number(), port3);
        cp3 = new ConnectPoint(virtualDevice2.id(), port3.number());

        Port port4 = new DefaultPort(virtualDevice2, PortNumber.portNumber(4), true);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice2.id(), port4.number(), port4);
        cp4 = new ConnectPoint(virtualDevice2.id(), port4.number());

        Port port5 = new DefaultPort(virtualDevice3, PortNumber.portNumber(5), true);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice3.id(), port5.number(), port5);
        cp5 = new ConnectPoint(virtualDevice3.id(), port5.number());

        Port port6 = new DefaultPort(virtualDevice3, PortNumber.portNumber(6), true);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice3.id(), port6.number(), port6);
        cp6 = new ConnectPoint(virtualDevice3.id(), port6.number());

        link1 = manager.createVirtualLink(virtualNetwork.id(), cp1, cp3);
        virtualNetworkManagerStore.updateLink(link1, link1.tunnelId(), Link.State.ACTIVE);
        link2 = manager.createVirtualLink(virtualNetwork.id(), cp3, cp1);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);
        link3 = manager.createVirtualLink(virtualNetwork.id(), cp4, cp5);
        virtualNetworkManagerStore.updateLink(link3, link3.tunnelId(), Link.State.ACTIVE);
        link4 = manager.createVirtualLink(virtualNetwork.id(), cp5, cp4);
        virtualNetworkManagerStore.updateLink(link4, link4.tunnelId(), Link.State.ACTIVE);

        vnetIntentService = new VirtualNetworkIntentService(manager, virtualNetwork, testDirectory);
        vnetIntentService.intentService = intentService;
        vnetIntentService.store = virtualNetworkManagerStore;
        vnetIntentService.partitionService = workPartitionService;
        return virtualNetwork;
    }

    /**
     * Tests the submit(), withdraw(), and purge() methods.
     */
    @Test
    public void testCreateAndRemoveIntent() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        Key intentKey = Key.of("test", APP_ID);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new EncapsulationConstraint(EncapsulationType.VLAN));

        VirtualNetworkIntent virtualIntent = VirtualNetworkIntent.builder()
                .networkId(virtualNetwork.id())
                .key(intentKey)
                .appId(APP_ID)
                .ingressPoint(cp1)
                .egressPoint(cp5)
                .constraints(constraints)
                .build();
        // Test the submit() method.
        vnetIntentService.submit(virtualIntent);

        // Wait for the both intents to go into an INSTALLED state.
        try {
            if (!created.tryAcquire(MAX_PERMITS, MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                fail("Failed to wait for intent to get installed.");
            }
        } catch (InterruptedException e) {
            fail("Semaphore exception during intent installation." + e.getMessage());
        }

        // Test the getIntentState() method
        assertEquals("The intent state did not match as expected.", IntentState.INSTALLED,
                     vnetIntentService.getIntentState(virtualIntent.key()));

        // Test the withdraw() method.
        vnetIntentService.withdraw(virtualIntent);
        // Wait for the both intents to go into a WITHDRAWN state.
        try {
            if (!withdrawn.tryAcquire(MAX_PERMITS, MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                fail("Failed to wait for intent to get withdrawn.");
            }
        } catch (InterruptedException e) {
            fail("Semaphore exception during intent withdrawal." + e.getMessage());
        }

        // Test the getIntentState() method
        assertEquals("The intent state did not match as expected.", IntentState.WITHDRAWN,
                     vnetIntentService.getIntentState(virtualIntent.key()));

        // Test the purge() method.
        vnetIntentService.purge(virtualIntent);
        // Wait for the both intents to be removed/purged.
        try {
            if (!purged.tryAcquire(MAX_PERMITS, MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                fail("Failed to wait for intent to get purged.");
            }
        } catch (InterruptedException e) {
            fail("Semaphore exception during intent purging." + e.getMessage());
        }

    }

    /**
     * Tests the getIntents, getIntent(), getIntentData(), getIntentCount(),
     * isLocal() methods.
     */
    @Test
    public void testGetIntents() {
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        Key intentKey = Key.of("test", APP_ID);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new EncapsulationConstraint(EncapsulationType.VLAN));

        VirtualNetworkIntent virtualIntent = VirtualNetworkIntent.builder()
                .networkId(virtualNetwork.id())
                .key(intentKey)
                .appId(APP_ID)
                .ingressPoint(cp1)
                .egressPoint(cp5)
                .constraints(constraints)
                .build();
        // Test the submit() method.
        vnetIntentService.submit(virtualIntent);

        // Wait for the both intents to go into an INSTALLED state.
        try {
            if (!created.tryAcquire(MAX_PERMITS, MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                fail("Failed to wait for intent to get installed.");
            }
        } catch (InterruptedException e) {
            fail("Semaphore exception during intent installation." + e.getMessage());
        }

        // Test the getIntents() method
        assertEquals("The intents size did not match as expected.", 1,
                     Iterators.size(vnetIntentService.getIntents().iterator()));

        // Test the getIntent() method
        assertNotNull("The intent should have been found.", vnetIntentService.getIntent(virtualIntent.key()));

        // Test the getIntentData() method
        assertEquals("The intent data size did not match as expected.", 1,
                     Iterators.size(vnetIntentService.getIntentData().iterator()));

        // Test the getIntentCount() method
        assertEquals("The intent count did not match as expected.", 1,
                     vnetIntentService.getIntentCount());

        // Test the isLocal() method
        assertTrue("The intent should be local.", vnetIntentService.isLocal(virtualIntent.key()));

    }

    /**
     * Test listener to listen for intent events.
     */
    private static class TestListener implements IntentListener {

        @Override
        public void event(IntentEvent event) {
            switch (event.type()) {
                case INSTALLED:
                    // Release one permit on the created semaphore since the Intent event was received.
                    virtualNetworkManagerStore.addOrUpdateIntent(event.subject(), IntentState.INSTALLED);
                    created.release();
                    break;
                case WITHDRAWN:
                    // Release one permit on the removed semaphore since the Intent event was received.
                    virtualNetworkManagerStore.addOrUpdateIntent(event.subject(), IntentState.WITHDRAWN);
                    withdrawn.release();
                    break;
                case PURGED:
                    // Release one permit on the purged semaphore since the Intent event was received.
                    purged.release();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Core service test class.
     */
    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }
    }

    private static class TestIntentCompiler implements IntentCompiler<VirtualNetworkIntent> {
        @Override
        public List<Intent> compile(VirtualNetworkIntent intent, List<Intent> installable) {
            return Lists.newArrayList(new MockInstallableIntent());
        }
    }

    private static class MockInstallableIntent extends FlowRuleIntent {

        public MockInstallableIntent() {
            super(APP_ID, Collections.singletonList(new IntentTestsMocks.MockFlowRule(100)), Collections.emptyList());
        }
    }
}
