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
import org.onlab.util.Tools;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.utils.MeteringAgent;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


public class DefaultAsyncAtomicValueTest {
    DefaultAsyncAtomicValue<String> defaultAsyncAtomicValue;

    private AsyncConsistentMap<String, byte[]> asyncMap;

    private Serializer serializer;
    private MeteringAgent meteringAgent;

    private static final String MAPNAME = "map1";


    private static final String NAME = "atomicValue";
    private static final String NAME1 = "atomicValue1";
    private static final String TEST = "foo";
    private static final String TEST1 = "bar";
    private static final int INTNAME = 20;
    private static final long VERSION1 = 1;

    private final byte[] value1 = Tools.getBytesUtf8(NAME);
    private final byte[] value2 = Tools.getBytesUtf8(NAME1);
    private final byte[] value3 = Tools.getBytesUtf8("tester");
    private final byte[] defaultValue = Tools.getBytesUtf8("default");

    @Before
    public void setUp() throws Exception {
        asyncMap = new AsyncConsistentMapMock<>();
        serializer = Serializer.using(KryoNamespaces.BASIC);
        meteringAgent = new MeteringAgent(NAME, "*", false);
        defaultAsyncAtomicValue = new DefaultAsyncAtomicValue(MAPNAME, serializer,
                asyncMap, meteringAgent);
    }

    @After
    public void tearDown() throws Exception {
        defaultAsyncAtomicValue.destroy();
    }

    @Test
    public void testAsyncMapping() {
        assertThat(asyncMap.size().join(), is(0));
        asyncMap.put(TEST, value1);
        asyncMap.put(TEST1, value2);
        asyncMap.put("default", defaultValue);

        assertThat(asyncMap.getOrDefault("noMatch", defaultValue).join().value(),
                is(asyncMap.get("default").join().value()));

        assertThat(asyncMap.size().join(), is(3));
        assertThat(asyncMap.get(TEST).join().value(), is(value1));

        assertThat(asyncMap.getOrDefault(TEST, Tools.getBytesUtf8("newTest")).join().value(),
                is(asyncMap.get(TEST).join().value()));

        assertThat(asyncMap.containsKey(TEST).join(), is(true));

        asyncMap.put(TEST, value3);
        assertThat(asyncMap.get(TEST).join().value(), is(value3));
        asyncMap.putIfAbsent(TEST, value3);
        assertThat(asyncMap.size().join(), is(3));

        asyncMap.replace(TEST, value3, value1);
        assertThat(asyncMap.get(TEST).join().value(), is(value1));

        asyncMap.replace(TEST, VERSION1, value3);
        assertThat(asyncMap.get(TEST).join().value(), is(value3));

        asyncMap.replace(TEST, value3, defaultValue);
        assertThat(asyncMap.get(TEST).join().value(), is(defaultValue));
        asyncMap.replace(TEST, value1);
        assertThat(asyncMap.get(TEST).join().value(), is(value1));

        asyncMap.remove(TEST, value2);

        assertThat(asyncMap.size().join(), is(3));
    }

    @Test
    public void testAsync() {
        asyncMap.put(TEST, value1);
        asyncMap.put(TEST1, value2);

        assertNull(defaultAsyncAtomicValue.get().join());
        defaultAsyncAtomicValue = new DefaultAsyncAtomicValue(NAME, serializer,
                asyncMap, meteringAgent);
        assertThat(defaultAsyncAtomicValue.name(), is(NAME));
        defaultAsyncAtomicValue.set(null).join();
        assertNull(defaultAsyncAtomicValue.get().join());

        defaultAsyncAtomicValue.set(NAME).join();
        assertThat(defaultAsyncAtomicValue.get().join(), is(NAME));

        defaultAsyncAtomicValue.set(NAME1).join();
        assertThat(defaultAsyncAtomicValue.get().join(), is(NAME1));
        defaultAsyncAtomicValue.compareAndSet(NAME1, NAME).join();
        assertThat(defaultAsyncAtomicValue.get().join(), is(NAME));


        defaultAsyncAtomicValue.getAndSet(null).join();
        assertNull(defaultAsyncAtomicValue.get().join());

        defaultAsyncAtomicValue.set(NAME1).join();
        assertThat(defaultAsyncAtomicValue.getAndSet(NAME).join(), is(NAME1));
        assertThat(defaultAsyncAtomicValue.getAndSet("new").join(), is(NAME));

        assertThat(defaultAsyncAtomicValue.get().join(), is("new"));
    }
}