package org.onosproject.store.serializers;

import org.junit.Test;
import org.onlab.util.KryoNamespace;

import static org.junit.Assert.assertTrue;

/**
 * Tests pre-defined Kryo namespaces to catch basic errors such as someone
 * adding too many registrations which may flow into another namespace.
 */
public class KryoNamespacesTest {

    /**
     * Verifies that the BASIC namespace has not exceeded its allocated size.
     */
    @Test
    public void basicNamespaceSizeTest() {
        testNamespaceSize(KryoNamespaces.BASIC, KryoNamespaces.BASIC_MAX_SIZE);
    }

    /**
     * Verifies that the MISC namespace has not exceeded its allocated size.
     */
    @Test
    public void miscNamespaceSizeTest() {
        testNamespaceSize(KryoNamespaces.MISC, KryoNamespaces.MISC_MAX_SIZE);
    }

    /**
     * Verifies that the API namespace has not exceeded its allocated size.
     */
    @Test
    public void apiNamespaceSizeTest() {
        testNamespaceSize(KryoNamespaces.API, KryoNamespaces.API_MAX_SIZE);
    }

    private void testNamespaceSize(KryoNamespace namespace, int maxSize) {
        assertTrue("Kryo namespace has exceeded its allocated size",
                namespace.size() < maxSize);
    }
}
