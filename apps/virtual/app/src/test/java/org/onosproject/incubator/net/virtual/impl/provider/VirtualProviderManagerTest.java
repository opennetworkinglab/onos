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

package org.onosproject.incubator.net.virtual.impl.provider;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProviderService;
import org.onosproject.net.provider.ProviderId;

import static org.junit.Assert.assertEquals;

public class VirtualProviderManagerTest {

    private static final String TEST_SCHEME1 = "test1";
    private static final String TEST_SCHEME2 = "test2";
    private static final String TEST_ID1 = "org.onosproject.virtual.testprovider1";
    private static final String TEST_ID2 = "org.onosproject.virtual.testprovider1";
    private static final NetworkId NETWORK_ID1 = NetworkId.networkId(1);
    private static final NetworkId NETWORK_ID2 = NetworkId.networkId(2);

    VirtualProviderManager virtualProviderManager;

    @Before
    public void setUp() throws Exception {
        virtualProviderManager = new VirtualProviderManager();
    }

    /**
     * Tests registerProvider() and unregisterProvider().
     */
    @Test
    public void registerProviderTest() {
        TestProvider1 provider1 = new TestProvider1();
        virtualProviderManager.registerProvider(provider1);

        assertEquals("The number of registered provider did not match.", 1,
                     virtualProviderManager.getProviders().size());

        assertEquals("The registered provider did not match", provider1,
                     virtualProviderManager.getProvider(TEST_SCHEME1));

        virtualProviderManager.unregisterProvider(provider1);

        TestProvider2 provider2 = new TestProvider2();
        virtualProviderManager.registerProvider(provider2);

        assertEquals("The number of registered provider did not match.", 1,
                     virtualProviderManager.getProviders().size());

        virtualProviderManager.unregisterProvider(provider2);

        assertEquals("The number of registered provider did not match.", 0,
                     virtualProviderManager.getProviders().size());
    }

    /**
     * Tests registerProviderService() and getProviderService().
     */
    @Test
    public void registerProviderServiceTest() {
        TestProvider1 provider1 = new TestProvider1();
        virtualProviderManager.registerProvider(provider1);

        TestProviderService1 providerService1 = new TestProviderService1();
        virtualProviderManager.registerProviderService(NETWORK_ID1, providerService1);

        assertEquals(providerService1,
                     virtualProviderManager.getProviderService(NETWORK_ID1, TestProvider1.class));
    }

    private class TestProvider1 extends AbstractVirtualProvider {
        protected TestProvider1() {
            super(new ProviderId(TEST_SCHEME1, TEST_ID1));
        }
    }

    private class TestProvider2 extends AbstractVirtualProvider {
        protected TestProvider2() {
            super(new ProviderId(TEST_SCHEME2, TEST_ID2));
        }
    }

    private class TestProviderService1 extends AbstractVirtualProviderService<TestProvider1> {
    }

    private class TestProviderService2 extends AbstractVirtualProviderService<TestProvider2> {
    }
}
