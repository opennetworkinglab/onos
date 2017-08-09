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
package org.onosproject.store.primitives.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.Serializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DefaultAsyncDistributedSetTest {
    DefaultAsyncDistributedSet<String> defaultAsyncDistributedSet;

    private static String name;
    private static Serializer serializer;
    private static AsyncConsistentMap<String, Boolean> asyncConsistentMap;
    private static AsyncConsistentMap<String, byte[]> baseMap;
    private static Map<Integer, String> map;
    private static Collection<String> collection;
    private static Collection<String> collection1;
    private static Set<String> set;

    private static final Boolean MAGENT = false;
    private static final String TEST1 = "one";
    private static final String TEST2 = "two";
    private static final String TEST3 = "three";
    private static final String TEST4 = "four";
    private static final String TEST5 = "five";

    @Before
    public void setUp() throws Exception {
        serializer = Serializer.using(KryoNamespaces.API);
        asyncConsistentMap = new AsyncConsistentMapMock<>();
        name = "Default Name";
        map = new HashMap<>();
        collection = new ArrayList<>();
        set = new HashSet<>();

        defaultAsyncDistributedSet = new DefaultAsyncDistributedSet<>(asyncConsistentMap,
                name, MAGENT);
    }
    @After
    public void clear() throws Exception {
        defaultAsyncDistributedSet.clear().join();
        assertThat(defaultAsyncDistributedSet.size().join(), is(0));
        assertTrue(defaultAsyncDistributedSet.isEmpty().join());
    }
    @Test
    public void testProperties() {
        assertThat(defaultAsyncDistributedSet.name(), is(name));
        assertTrue(defaultAsyncDistributedSet.isEmpty().join());

        collection.add(TEST1);
        collection.add(TEST2);
        collection.add(TEST3);
        set.add(TEST4);
        set.add(TEST5);

        assertThat(defaultAsyncDistributedSet.size().join(), is(0));
        defaultAsyncDistributedSet.add(TEST1).join();
        defaultAsyncDistributedSet.add(TEST2).join();
        assertThat(defaultAsyncDistributedSet.size().join(), is(2));
        defaultAsyncDistributedSet.add(TEST3).join();
        assertThat(defaultAsyncDistributedSet.size().join(), is(3));

        defaultAsyncDistributedSet.remove(TEST1);
        assertThat(defaultAsyncDistributedSet.size().join(), is(2));
        assertFalse(defaultAsyncDistributedSet.contains(TEST1).join());
        assertTrue(defaultAsyncDistributedSet.contains(TEST2).join());

        defaultAsyncDistributedSet.addAll(collection).join();
        assertTrue(defaultAsyncDistributedSet.containsAll(collection).join());
        assertFalse(defaultAsyncDistributedSet.retainAll(collection).join());

        assertThat(defaultAsyncDistributedSet.size().join(), is(3));

        defaultAsyncDistributedSet.addAll(set).join();
        assertThat(defaultAsyncDistributedSet.size().join(), is(5));
        assertTrue(defaultAsyncDistributedSet.contains(TEST4).join());
        defaultAsyncDistributedSet.retainAll(set);

        assertTrue(defaultAsyncDistributedSet.contains(TEST4).join());
        assertThat(defaultAsyncDistributedSet.contains(TEST1).join(), is(false));

        assertThat(defaultAsyncDistributedSet.getAsImmutableSet().join().size(),
                is(2));
    }

}