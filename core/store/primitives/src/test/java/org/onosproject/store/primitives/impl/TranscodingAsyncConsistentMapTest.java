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

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.Serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TranscodingAsyncConsistentMapTest {

    private static Serializer serializer;
    private static AsyncConsistentMap<String, DeviceId> transcodingMap;
    private static AsyncConsistentMap<String, byte[]> baseMap;
    private static Map<String, byte[]> map;


    private static final String KEY1 = "Key1";
    private static final String KEY2 = "Key2";
    private static final String KEY3 = "Key3";
    private static final DeviceId DEV1 = DeviceId.deviceId("Device1");
    private static final DeviceId DEV2 = DeviceId.deviceId("Device2");
    private static final DeviceId DEV3 = DeviceId.deviceId("foo");
    private static final DeviceId DEV4 = DeviceId.deviceId("bar");

    @Before
    public void setUp() throws Exception {
        serializer = Serializer.using(KryoNamespaces.API);
        map = new HashMap<>();
        baseMap = new AsyncConsistentMapMock<>();

        transcodingMap =  DistributedPrimitives.newTranscodingMap(
                baseMap,
                Function.identity(),
                Function.identity(),
                serializer::encode,
                serializer::decode);
    }

    @Test
    public void testSize() throws Exception {

        assertThat(transcodingMap.size().join(), is(0));
        transcodingMap.put(KEY1, DEV1).join();
        assertThat(transcodingMap.size().join(), is(1));
        transcodingMap.put(KEY1, DEV2).join();
        assertThat(transcodingMap.size().join(), is(1));

        transcodingMap.put(KEY2, DEV2).join();
        assertThat(transcodingMap.size().join(), is(2));
        for (int i = 0; i < 20; i++) {
            transcodingMap.put("KEY" + i + 1, DeviceId.deviceId("Device" + i + 1)).join();
        }
        assertThat(transcodingMap.size().join(), is(22));
    }

    @Test
    public void testEmpty() throws Exception {
        assertTrue(transcodingMap.isEmpty().join());
        transcodingMap.put(KEY1, DEV1).join();
        assertFalse(transcodingMap.isEmpty().join());
        transcodingMap.remove(KEY1).join();
        assertTrue(transcodingMap.isEmpty().join());
        transcodingMap.put(KEY1, DEV1).join();

        transcodingMap.remove(KEY1, DEV1).join();
        transcodingMap.put(KEY2, DEV2).join();

        transcodingMap.remove(KEY1, 1).join();
        assertThat(transcodingMap.size().join(), is(1));

        transcodingMap.clear().join();
        assertThat(transcodingMap.isEmpty().join(), is(true));

    }

    @Test
    public void testContains() throws Exception {
        assertFalse(transcodingMap.containsKey(KEY1).join());
        transcodingMap.put(KEY1, DEV1);
        assertTrue(transcodingMap.containsKey(KEY1).join());
        assertTrue(transcodingMap.containsValue(DEV1).join());

        transcodingMap.put(KEY2, DEV2);
        assertTrue(transcodingMap.containsValue(DEV2).join());

        transcodingMap.remove(KEY1);
        assertFalse(transcodingMap.containsKey(KEY1).join());
        assertFalse(transcodingMap.containsValue(DEV1).join());
    }

    @Test
    public void testGet() throws Exception {
        assertNull(transcodingMap.get(KEY1).join().value());
        transcodingMap.put(KEY2, DEV1).join();
        transcodingMap.put(KEY2, DEV3).join();

        assertThat(transcodingMap.get(KEY2).join().value(), is(DEV3));

        assertThat(transcodingMap.getOrDefault(KEY1, DeviceId.deviceId("bar")).join().value(),
                is(DEV4));
        transcodingMap.put(KEY1, DEV2).join();
        assertThat(transcodingMap.getOrDefault(KEY1, DEV1).join().value(), is(DEV2));
        assertThat(transcodingMap.get(KEY1).join().value(), is(DEV2));
    }

    @Test
    public void testSwitch() throws Exception {
        transcodingMap.put(KEY1, DEV1).join();
        transcodingMap.put(KEY2, DEV2).join();

        transcodingMap.replace(KEY1, DEV1, DEV3).join();
        assertThat(transcodingMap.containsValue(DEV2).join(), is(true));
        transcodingMap.putAndGet(KEY1, DEV3).join();
        assertThat(transcodingMap.get(KEY1).join().value(), is(DEV3));

        transcodingMap.replace(KEY1, DEV1);
        assertThat(transcodingMap.get(KEY1).join().value(), is(DEV1));

        transcodingMap.replace(KEY1, 1, DEV4);
        assertThat(transcodingMap.get(KEY1).join().value(), is(DEV4));

        assertNull(transcodingMap.remove("keyTest").join().value());

    }

    @Test
    public void testEntry() throws Exception {
        assertThat(transcodingMap.entrySet().join().size(), is(0));
        transcodingMap.put(KEY2, DEV2).join();
        transcodingMap.put(KEY1, DEV1).join();
        assertThat(transcodingMap.entrySet().join().size(), is(2));
        transcodingMap.put(KEY3, DEV3).join();
        transcodingMap.put(KEY3, DEV4).join();
        assertThat(transcodingMap.entrySet().join().size(), (is(3)));

        transcodingMap.put(KEY3, null).join();
        transcodingMap.putIfAbsent(KEY3, DEV3).join();
        assertThat(transcodingMap.entrySet().join().size(), is(3));
        assertThat(transcodingMap.get(KEY3).join().value(), is(DEV3));
    }

    @Test
    public void keyTest() throws Exception {
        assertThat(transcodingMap.keySet().join().size(), is(0));
        transcodingMap.putIfAbsent(KEY1, DEV1).join();
        transcodingMap.putIfAbsent(KEY2, DEV2).join();
        transcodingMap.putIfAbsent(KEY3, DEV3).join();
        assertThat(transcodingMap.keySet().join().size(), is(3));
        assertThat(transcodingMap.keySet().join(), hasItem(KEY1));
        assertThat(transcodingMap.keySet().join(), hasItem(KEY2));
    }

}