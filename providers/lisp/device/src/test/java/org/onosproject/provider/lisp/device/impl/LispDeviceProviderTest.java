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
package org.onosproject.provider.lisp.device.impl;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.lisp.ctl.LispController;
import org.onosproject.lisp.ctl.LispControllerAdapter;
import org.onosproject.lisp.ctl.LispRouter;
import org.onosproject.lisp.ctl.LispRouterListener;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderRegistryAdapter;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceProviderServiceAdapter;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * LISP device provider unit test.
 */
public class LispDeviceProviderTest {

    private static final String APP_NAME = "org.onosproject.lisp";

    private final LispDeviceProvider provider = new LispDeviceProvider();
    private final LispController controller = new MockLispController();

    // provider mocks
    private final DeviceProviderRegistry providerRegistry = new MockDeviceProviderRegistry();
    private final DeviceProviderService providerService = new MockDeviceProviderService();
    private final DeviceService deviceService = new MockDeviceService();

    private final Set<LispRouterListener> routerListeners = Sets.newCopyOnWriteArraySet();

    private CoreService coreService;
    private ApplicationId appId = new DefaultApplicationId(200, APP_NAME);
    private Set<DeviceListener> deviceListeners = Sets.newHashSet();

    @Before
    public void setUp() {
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(APP_NAME))
                .andReturn(appId).anyTimes();
        replay(coreService);
        provider.coreService = coreService;
        provider.providerRegistry = providerRegistry;
        provider.deviceService = deviceService;
        provider.controller = controller;
        provider.activate();
    }

    @Test
    public void activate() throws Exception {

        assertEquals("Provider should be registered", 1,
                            providerRegistry.getProviders().size());
        assertTrue("LISP device provider should be registered",
                            providerRegistry.getProviders().contains(provider.id()));
        assertEquals("Incorrect provider service",
                            providerService, provider.providerService);
        assertEquals("LISP router listener should be registered", 1,
                            routerListeners.size());
    }

    @Test
    public void deactivate() throws Exception {
        provider.deactivate();

        assertFalse("Provider should not be registered",
                            providerRegistry.getProviders().contains(provider.id()));
        assertNull("Provider service should be null",
                            provider.providerService);
        assertEquals("Controller listener should be removed", 0,
                            routerListeners.size());
    }

    /**
     * Mock class for LispController.
     */
    private class MockLispController extends LispControllerAdapter {

        Iterable<LispRouter> routers = Sets.newHashSet();

        @Override
        public Iterable<LispRouter> getRouters() {
            return routers;
        }

        @Override
        public void addRouterListener(LispRouterListener listener) {
            if (!routerListeners.contains(listener)) {
                routerListeners.add(listener);
            }
        }

        @Override
        public void removeRouterListener(LispRouterListener listener) {
            routerListeners.remove(listener);
        }
    }

    /**
     * Mock class for DeviceProviderRegistry.
     */
    private class MockDeviceProviderRegistry extends DeviceProviderRegistryAdapter {
        Set<ProviderId> providers = Sets.newHashSet();

        @Override
        public DeviceProviderService register(DeviceProvider provider) {
            providers.add(provider.id());
            return providerService;
        }

        @Override
        public void unregister(DeviceProvider provider) {
            providers.remove(provider.id());
        }

        @Override
        public Set<ProviderId> getProviders() {
            return providers;
        }
    }

    /**
     * Mock class for DeviceService.
     */
    private class MockDeviceService extends DeviceServiceAdapter {
        @Override
        public void addListener(DeviceListener listener) {
            deviceListeners.add(listener);
        }

        @Override
        public void removeListener(DeviceListener listener) {
            deviceListeners.remove(listener);
        }
    }

    /**
     * Mock class for DeviceProviderService.
     */
    private class MockDeviceProviderService extends DeviceProviderServiceAdapter {

    }
}
