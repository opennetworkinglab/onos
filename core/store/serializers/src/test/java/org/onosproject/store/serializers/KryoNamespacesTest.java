/*
 * Copyright 2016-present Open Networking Foundation
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
