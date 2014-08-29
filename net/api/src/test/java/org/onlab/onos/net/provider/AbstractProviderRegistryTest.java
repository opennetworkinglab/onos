package org.onlab.onos.net.provider;

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

        ProviderId fooId = new ProviderId("foo");
        TestProvider pFoo = new TestProvider(fooId);
        TestProviderService psFoo = registry.register(pFoo);
        assertEquals("incorrect provider count", 1, registry.getProviders().size());
        assertThat("provider not found", registry.getProviders().contains(fooId));
        assertEquals("incorrect provider", psFoo.provider(), pFoo);

        ProviderId barId = new ProviderId("bar");
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

    @Test(expected = IllegalStateException.class)
    public void duplicateRegistration() {
        TestProviderRegistry registry = new TestProviderRegistry();
        TestProvider pFoo = new TestProvider(new ProviderId("foo"));
        registry.register(pFoo);
        registry.register(pFoo);
    }

    @Test
    public void voidUnregistration() {
        TestProviderRegistry registry = new TestProviderRegistry();
        registry.unregister(new TestProvider(new ProviderId("foo")));
    }

    @Test(expected = IllegalStateException.class)
    public void unregistration() {
        TestProviderRegistry registry = new TestProviderRegistry();
        TestProvider pFoo = new TestProvider(new ProviderId("foo"));
        TestProviderService psFoo = registry.register(pFoo);
        registry.unregister(pFoo);
        psFoo.checkValidity();
    }
}
