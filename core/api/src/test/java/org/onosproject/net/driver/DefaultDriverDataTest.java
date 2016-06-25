/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.driver;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;

public class DefaultDriverDataTest {

    public static final DeviceId DEVICE_ID = deviceId("of:0011223344556677");

    DefaultDriver ddc;
    DefaultDriverData data;

    @Before
    public void setUp() {
        ddc = new DefaultDriver("foo.bar", new ArrayList<>(), "Circus", "lux", "1.2a",
                                ImmutableMap.of(TestBehaviour.class,
                                                TestBehaviourImpl.class),
                                ImmutableMap.of("foo", "bar"));
        data = new DefaultDriverData(ddc, DEVICE_ID);
    }

    @Test
    public void basics() {
        assertSame("incorrect driver", ddc, data.driver());
        assertEquals("incorrect device id", DEVICE_ID, data.deviceId());
        assertTrue("incorrect toString", data.toString().contains("foo.bar"));
    }

    @Test
    public void behaviour() {
        TestBehaviour behaviour = data.behaviour(TestBehaviour.class);
        assertTrue("incorrect behaviour", behaviour instanceof TestBehaviourImpl);
    }

    @Test
    public void setAndClearAnnotations() {
        data.set("croc", "aqua");
        data.set("roo", "mars");
        data.set("dingo", "bat");
        assertEquals("incorrect property", "bat", data.value("dingo"));
        data.clear("dingo", "roo");
        assertNull("incorrect property", data.value("dingo"));
        assertNull("incorrect property", data.value("root"));
        assertEquals("incorrect property", "aqua", data.value("croc"));
        assertEquals("incorrect properties", 1, data.keys().size());
    }

    @Test
    public void clearAllAnnotations() {
        data.set("croc", "aqua");
        data.set("roo", "mars");
        data.set("dingo", "bat");
        assertEquals("incorrect property", "bat", data.value("dingo"));
        data.clear();
        assertEquals("incorrect properties", 0, data.keys().size());
    }

}