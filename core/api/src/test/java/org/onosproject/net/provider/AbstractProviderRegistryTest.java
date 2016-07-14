/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.provider;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Test of the base provider registry.
 */
public class AbstractProviderRegistryTest {

    private class TestProviderService extends AbstractProviderService<TestProvider> {
        protected TestProviderService(TestProvider provider) {
            super(provider);
        }
    }

    private class TestProviderRegistry extends AbstractProviderRegistry<TestProvider, TestProviderService> {
        @Override
        protected TestProviderService createProviderService(TestProvider provider) {
            return new TestProviderService(provider);
        }
    }

    @Test
    public void basics() {
        TestProviderRegistry registry = new TestProviderRegistry();
        assertEquals("incorrect provider count", 0, registry.getProviders().size());

        ProviderId fooId = new ProviderId("of", "foo");
        TestProvider pFoo = new TestProvider(fooId);
        TestProviderService psFoo = registry.register(pFoo);
        assertEquals("incorrect provider count", 1, registry.getProviders().size());
        assertThat("provider not found", registry.getProviders().contains(fooId));
        assertEquals("incorrect provider", psFoo.provider(), pFoo);

        ProviderId barId = new ProviderId("snmp", "bar");
        TestProvider pBar = new TestProvider(barId);
        TestProviderService psBar = registry.register(pBar);
        assertEquals("incorrect provider count", 2, registry.getProviders().size());
        assertThat("provider not found", registry.getProviders().contains(barId));
        assertEquals("incorrect provider", psBar.provider(), pBar);

        psFoo.checkValidity();
        registry.unregister(pFoo);
        psBar.checkValidity();
        assertEquals("incorrect provider count", 1, registry.getProviders().size());
        assertThat("provider not found", registry.getProviders().contains(barId));
    }

    @Test
    public void ancillaryProviders() {
        TestProviderRegistry registry = new TestProviderRegistry();
        TestProvider pFoo = new TestProvider(new ProviderId("of", "foo"));
        TestProvider pBar = new TestProvider(new ProviderId("of", "bar", true));
        registry.register(pFoo);
        registry.register(pBar);
        assertEquals("incorrect provider count", 2, registry.getProviders().size());
    }

    @Test(expected = IllegalStateException.class)
    public void duplicateRegistration() {
        TestProviderRegistry registry = new TestProviderRegistry();
        TestProvider pFoo = new TestProvider(new ProviderId("of", "foo"));
        registry.register(pFoo);
        registry.register(pFoo);
    }

    @Test(expected = IllegalStateException.class)
    public void duplicateSchemeRegistration() {
        TestProviderRegistry registry = new TestProviderRegistry();
        TestProvider pFoo = new TestProvider(new ProviderId("of", "foo"));
        TestProvider pBar = new TestProvider(new ProviderId("of", "bar"));
        registry.register(pFoo);
        registry.register(pBar);
    }

    @Test
    public void voidUnregistration() {
        TestProviderRegistry registry = new TestProviderRegistry();
        registry.unregister(new TestProvider(new ProviderId("of", "foo")));
    }

    @Test(expected = IllegalStateException.class)
    public void unregistration() {
        TestProviderRegistry registry = new TestProviderRegistry();
        TestProvider pFoo = new TestProvider(new ProviderId("of", "foo"));
        TestProviderService psFoo = registry.register(pFoo);
        registry.unregister(pFoo);
        psFoo.checkValidity();
    }
}
