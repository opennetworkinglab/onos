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

import com.google.common.collect.Sets;
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
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Junit tests for PtToPtIntentVirtualNetworkProvider.
 */
public class PtToPtIntentVirtualNetworkProviderTest {

    private PtToPtIntentVirtualNetworkProvider provider;
    private VirtualNetworkProviderRegistry providerRegistry;

    private final VirtualNetworkRegistryAdapter virtualNetworkRegistry = new VirtualNetworkRegistryAdapter();
    private IntentService intentService;

    private static final ApplicationId APP_ID =
            TestApplicationId.create(PtToPtIntentVirtualNetworkProvider.PTPT_INTENT_APPID);

    private IdGenerator idGenerator = new MockIdGenerator();

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

        intentService = new TestIntentService();
        provider.intentService = intentService;
        provider.activate();
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.providerRegistry = null;
        provider.coreService = null;
        provider.intentService = null;
        Intent.unbindIdGenerator(idGenerator);
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
        String key = String.format(PtToPtIntentVirtualNetworkProvider.KEY_FORMAT,
                                   networkId.toString(), src.toString(), dst.toString());

        assertEquals("TunnelId does not match as expected.", key, tunnelId.toString());
        provider.destroyTunnel(networkId, tunnelId);
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
        public void tunnelUp(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {

        }

        @Override
        public void tunnelDown(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {

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

    /**
     * Represents a fake IntentService class that easily allows to store and
     * retrieve intents without implementing the IntentService logic.
     */
    private class TestIntentService extends IntentServiceAdapter {

        private Set<Intent> intents;

        public TestIntentService() {
            intents = Sets.newHashSet();
        }

        @Override
        public void submit(Intent intent) {
            intents.add(intent);
        }

        @Override
        public void withdraw(Intent intent) {
        }

        @Override
        public IntentState getIntentState(Key intentKey) {
            return IntentState.WITHDRAWN;
        }

        @Override
        public void purge(Intent intent) {
            intents.remove(intent);
        }

        @Override
        public long getIntentCount() {
            return intents.size();
        }

        @Override
        public Iterable<Intent> getIntents() {
            return intents;
        }

        @Override
        public Intent getIntent(Key intentKey) {
            for (Intent intent : intents) {
                if (intent.key().equals(intentKey)) {
                    return intent;
                }
            }
            return null;
        }
    }
}
