package org.onlab.onos.net.provider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test of the base provider implementation.
 */
public class AbstractProviderTest {

    @Test
    public void basics() {
        ProviderId id = new ProviderId("foo.bar");
        TestProvider provider = new TestProvider(id);
        assertEquals("incorrect id", id, provider.id());
    }
}
