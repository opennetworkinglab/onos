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
package org.onosproject.provider.lisp.mapping.impl;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.lisp.ctl.LispController;
import org.onosproject.lisp.ctl.LispControllerAdapter;
import org.onosproject.lisp.ctl.LispRouter;
import org.onosproject.lisp.ctl.LispRouterListener;
import org.onosproject.mapping.MappingProvider;
import org.onosproject.mapping.MappingProviderRegistry;
import org.onosproject.mapping.MappingProviderRegistryAdapter;
import org.onosproject.mapping.MappingProviderService;
import org.onosproject.mapping.MappingProviderServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.util.Set;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * LISP mapping provider unit test.
 */
public class LispMappingProviderTest {


    private final LispMappingProvider provider = new LispMappingProvider();
    private final LispController controller = new MockLispController();

    // provider mocks
    private final MappingProviderRegistry providerRegistry =
                                            new MockMappingProviderRegistry();
    private final MappingProviderService providerService =
                                            new MockMappingProviderService();

    private final Set<LispRouterListener> routerListeners = Sets.newCopyOnWriteArraySet();


    @Before
    public void setUp() {
        provider.providerRegistry = providerRegistry;
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
                            providerRegistry.getProviders().contains(provider));
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
     * Mock class for MappingProviderRegistry.
     */
    private class MockMappingProviderRegistry extends MappingProviderRegistryAdapter {
        Set<ProviderId> providers = Sets.newHashSet();

        @Override
        public MappingProviderService register(MappingProvider provider) {
            providers.add(provider.id());
            return providerService;
        }

        @Override
        public void unregister(MappingProvider provider) {
            providers.remove(provider.id());
        }

        @Override
        public Set<ProviderId> getProviders() {
            return providers;
        }
    }

    /**
     * Mock class for MappingService.
     */
    private class MockMappingProviderService extends MappingProviderServiceAdapter {

    }
}
