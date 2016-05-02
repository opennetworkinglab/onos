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

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkProvider;
import org.onosproject.incubator.net.virtual.VirtualNetworkProviderRegistry;
import org.onosproject.incubator.net.virtual.VirtualNetworkProviderService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Junit tests for PtToPtIntentVirtualNetworkProvider.
 */
public class PtToPtIntentVirtualNetworkProviderTest {

    private PtToPtIntentVirtualNetworkProvider provider;
    private VirtualNetworkProviderRegistry providerRegistry;

    private final VirtualNetworkRegistryAdapter virtualNetworkRegistry = new VirtualNetworkRegistryAdapter();
    private TestableIntentService intentService = new FakeIntentManager();
    private TestListener listener = new TestListener();
    protected TestIntentCompiler compiler = new TestIntentCompiler();
    private IntentExtensionService intentExtensionService;

    private static final ApplicationId APP_ID =
            TestApplicationId.create(PtToPtIntentVirtualNetworkProvider.PTPT_INTENT_APPID);

    private IdGenerator idGenerator = new MockIdGenerator();
    private static final int MAX_WAIT_TIME = 5;
    private static final int MAX_PERMITS = 2;
    private static Semaphore created;
    private static Semaphore removed;

    @Before
    public void setUp() {
        provider = new PtToPtIntentVirtualNetworkProvider();
        provider.providerRegistry = virtualNetworkRegistry;
        final CoreService mockCoreService = createMock(CoreService.class);
        provider.coreService = mockCoreService;
        expect(mockCoreService.registerApplication(PtToPtIntentVirtualNetworkProvider.PTPT_INTENT_APPID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        Intent.unbindIdGenerator(idGenerator);
        Intent.bindIdGenerator(idGenerator);

        intentService.addListener(listener);
        provider.intentService = intentService;

        // Register a compiler and an installer both setup for success.
        intentExtensionService = intentService;
        intentExtensionService.registerCompiler(PointToPointIntent.class, compiler);

        provider.activate();
        created = new Semaphore(0, true);
        removed = new Semaphore(0, true);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
        intentService.removeListener(listener);
        provider.deactivate();
        provider.providerRegistry = null;
        provider.coreService = null;
        provider.intentService = null;
        created = null;
        removed = null;
    }

    @Test
    public void basics() {
        assertNotNull("registration expected", provider);
    }

    /**
     * Test a null network identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateTunnelNullNetworkId() {
        provider.createTunnel(null, null, null);
    }

    /**
     * Test a null source connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateTunnelNullSrc() {
        ConnectPoint dst = new ConnectPoint(DeviceId.deviceId("device2"), PortNumber.portNumber(2));

        provider.createTunnel(NetworkId.networkId(0), null, dst);
    }

    /**
     * Test a null destination connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateTunnelNullDst() {
        ConnectPoint src = new ConnectPoint(DeviceId.deviceId("device1"), PortNumber.portNumber(1));

        provider.createTunnel(NetworkId.networkId(0), src, null);
    }

    /**
     * Test creating/destroying a valid tunnel.
     */
    @Test
    public void testCreateRemoveTunnel() {
        NetworkId networkId = NetworkId.networkId(0);
        ConnectPoint src = new ConnectPoint(DeviceId.deviceId("device1"), PortNumber.portNumber(1));
        ConnectPoint dst = new ConnectPoint(DeviceId.deviceId("device2"), PortNumber.portNumber(2));

        TunnelId tunnelId = provider.createTunnel(networkId, src, dst);

        // Wait for the tunnel to go into an INSTALLED state, and that the tunnelUp method was called.
        try {
            if (!created.tryAcquire(MAX_PERMITS, MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                fail("Failed to wait for tunnel to get installed.");
            }
        } catch (InterruptedException e) {
            fail("Semaphore exception during tunnel installation." + e.getMessage());
        }

        String key = String.format(PtToPtIntentVirtualNetworkProvider.KEY_FORMAT,
                                   networkId.toString(), src.toString(), dst.toString());

        assertEquals("TunnelId does not match as expected.", key, tunnelId.toString());
        provider.destroyTunnel(networkId, tunnelId);

        // Wait for the tunnel to go into a WITHDRAWN state, and that the tunnelDown method was called.
        try {
            if (!removed.tryAcquire(MAX_PERMITS, MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                fail("Failed to wait for tunnel to get removed.");
            }
        } catch (InterruptedException e) {
            fail("Semaphore exception during tunnel removal." + e.getMessage());
        }
    }

    /**
     * Virtual network registry implementation for this test class.
     */
    private class VirtualNetworkRegistryAdapter implements VirtualNetworkProviderRegistry {
        private VirtualNetworkProvider provider;

        @Override
        public VirtualNetworkProviderService register(VirtualNetworkProvider theProvider) {
            this.provider = theProvider;
            return new TestVirtualNetworkProviderService(theProvider);
        }

        @Override
        public void unregister(VirtualNetworkProvider theProvider) {
            this.provider = null;
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }
    }

    /**
     * Virtual network provider service implementation for this test class.
     */
    private class TestVirtualNetworkProviderService
            extends AbstractProviderService<VirtualNetworkProvider>
            implements VirtualNetworkProviderService {

        protected TestVirtualNetworkProviderService(VirtualNetworkProvider provider) {
            super(provider);
        }

        @Override
        public void tunnelUp(NetworkId networkId, ConnectPoint src, ConnectPoint dst, TunnelId tunnelId) {
            // Release one permit on the created semaphore since the tunnelUp method was called.
            created.release();
        }

        @Override
        public void tunnelDown(NetworkId networkId, ConnectPoint src, ConnectPoint dst, TunnelId tunnelId) {
            // Release one permit on the removed semaphore since the tunnelDown method was called.
            removed.release();
        }
    }

    private static class TestListener implements IntentListener {

        @Override
        public void event(IntentEvent event) {
            switch (event.type()) {
                case INSTALLED:
                    // Release one permit on the created semaphore since the Intent event was received.
                    created.release();
                    break;
                case WITHDRAWN:
                    // Release one permit on the removed semaphore since the Intent event was received.
                    removed.release();
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

    private static class TestIntentCompiler implements IntentCompiler<PointToPointIntent> {
        @Override
        public List<Intent> compile(PointToPointIntent intent, List<Intent> installable) {
            return Lists.newArrayList(new MockInstallableIntent());
        }
    }

    private static class MockInstallableIntent extends FlowRuleIntent {

        public MockInstallableIntent() {
            super(APP_ID, Collections.singletonList(new IntentTestsMocks.MockFlowRule(100)), Collections.emptyList());
        }
    }
}
