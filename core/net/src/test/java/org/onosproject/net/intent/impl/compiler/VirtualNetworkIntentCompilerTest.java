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

package org.onosproject.net.intent.impl.compiler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.impl.VirtualNetworkManager;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.store.service.TestStorageService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.TestUtils.TestUtilsException;
import static org.onlab.junit.TestUtils.setField;

/**
 * Junit tests for virtual network intent compiler.
 */
public class VirtualNetworkIntentCompilerTest extends TestDeviceParams {

    private CoreService coreService;
    private TestableIntentService intentService = new FakeIntentManager();
    private IntentExtensionService intentExtensionService;
    private final IdGenerator idGenerator = new MockIdGenerator();
    private VirtualNetworkIntentCompiler compiler;
    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private ServiceDirectory testDirectory;

    private final String tenantIdValue1 = "TENANT_ID1";
    private static final ApplicationId APP_ID =
            new TestApplicationId("test");

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

    @Before
    public void setUp() throws TestUtilsException {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        coreService = new TestCoreService();

        Intent.unbindIdGenerator(idGenerator);
        Intent.bindIdGenerator(idGenerator);

        virtualNetworkManagerStore.setCoreService(coreService);
        setField(coreService, "coreService", new TestCoreService());
        setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.setStore(virtualNetworkManagerStore);
        manager.setIntentService(intentService);
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
        manager.activate();

        // Register a compiler and an installer both setup for success.
        intentExtensionService = intentService;

        testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkService.class, manager)
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(IntentService.class, intentService);
        BaseResource.setServiceDirectory(testDirectory);

        compiler = new VirtualNetworkIntentCompiler();
        compiler.manager = manager;
        compiler.intentService = intentService;
        compiler.store = virtualNetworkManagerStore;
        compiler.intentManager = intentExtensionService;
        compiler.serviceDirectory = testDirectory;
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
        manager.deactivate();
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

        return virtualNetwork;
    }

    /**
     * Tests the virtual network intent compiler.
     */
    @Test
    public void testCompiler() {
        compiler.activate();
        VirtualNetwork virtualNetwork = setupVirtualNetworkTopology();

        Key intentKey = Key.of("test", APP_ID);

        VirtualNetworkIntent virtualIntent = VirtualNetworkIntent.builder()
                .networkId(virtualNetwork.id())
                .key(intentKey)
                .appId(APP_ID)
                .ingressPoint(cp2)
                .egressPoint(cp6)
                .build();

        List<Intent> compiled = compiler.compile(virtualIntent, Collections.emptyList());
        assertEquals("The virtual intents size is not as expected.", 5, compiled.size());

        compiler.deactivate();
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

}
