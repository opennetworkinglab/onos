/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.driver.DefaultDriverDataTest.DEVICE_ID;

public class DefaultDriverTest {
    public static final String MFR = "mfr";
    public static final String HW = "hw";
    public static final String SW = "sw";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String ROOT = "rootDriver";
    public static final String CHILD = "childDriver";
    public static final String GRAND_CHILD = "grandChildDriver";

    @Test
    public void basics() {
        DefaultDriver ddp = new DefaultDriver("foo.base", new ArrayList<>(), "Circus", "lux", "1.2a",
                                              ImmutableMap.of(TestBehaviour.class,
                                                              TestBehaviourImpl.class,
                                                              TestBehaviourTwo.class,
                                                              TestBehaviourTwoImpl.class),
                                              ImmutableMap.of("foo", "bar"));

        DefaultDriver ddc = new DefaultDriver("foo.bar", ImmutableList.of(ddp), "Circus", "lux", "1.2a",
                                              ImmutableMap.of(),
                                              ImmutableMap.of("foo", "bar"));
        assertEquals("incorrect name", "foo.bar", ddc.name());
        assertEquals("incorrect parent", ddp, ddc.parent());
        assertEquals("incorrect empty parent", ImmutableList.of(), ddp.parents());
        assertEquals("incorrect mfr", "Circus", ddc.manufacturer());
        assertEquals("incorrect hw", "lux", ddc.hwVersion());
        assertEquals("incorrect sw", "1.2a", ddc.swVersion());

        assertEquals("incorrect behaviour count", 2, ddp.behaviours().size());
        assertEquals("incorrect behaviour count", 0, ddc.behaviours().size());
        assertTrue("incorrect behaviour", ddc.hasBehaviour(TestBehaviour.class));

        Behaviour b1 = ddc.createBehaviour(new DefaultDriverData(ddc, DEVICE_ID), TestBehaviour.class);
        assertTrue("incorrect behaviour class", b1 instanceof TestBehaviourImpl);

        Behaviour b2 = ddc.createBehaviour(new DefaultDriverHandler(new DefaultDriverData(ddc, DEVICE_ID)),
                                           TestBehaviourTwo.class);
        assertTrue("incorrect behaviour class", b2 instanceof TestBehaviourTwoImpl);

        assertEquals("incorrect property count", 1, ddc.properties().size());
        assertEquals("incorrect key count", 1, ddc.keys().size());
        assertEquals("incorrect property", "bar", ddc.value("foo"));

        assertTrue("incorrect toString", ddc.toString().contains("lux"));
    }

    @Test
    public void merge() {
        DefaultDriver one = new DefaultDriver("foo.bar", new ArrayList<>(), "Circus", "lux", "1.2a",
                                              ImmutableMap.of(TestBehaviour.class,
                                                              TestBehaviourImpl.class),
                                              ImmutableMap.of("foo", "bar"));
        Driver ddc =
                one.merge(new DefaultDriver("foo.bar", new ArrayList<>(), "", "", "",
                                            ImmutableMap.of(TestBehaviourTwo.class,
                                                            TestBehaviourTwoImpl.class),
                                            ImmutableMap.of("goo", "wee")));

        assertEquals("incorrect name", "foo.bar", ddc.name());
        assertEquals("incorrect mfr", "Circus", ddc.manufacturer());
        assertEquals("incorrect hw", "lux", ddc.hwVersion());
        assertEquals("incorrect sw", "1.2a", ddc.swVersion());

        assertEquals("incorrect behaviour count", 2, ddc.behaviours().size());
        assertTrue("incorrect behaviour", ddc.hasBehaviour(TestBehaviourTwo.class));

        assertEquals("incorrect property count", 2, ddc.properties().size());
        assertEquals("incorrect key count", 2, ddc.keys().size());
        assertEquals("incorrect property", "wee", ddc.value("goo"));

        assertTrue("incorrect toString", ddc.toString().contains("Circus"));
    }

    @Test
    public void testGetProperty() throws Exception {
        DefaultDriver root = new DefaultDriver(ROOT, Lists.newArrayList(), MFR, HW, SW,
                ImmutableMap.of(), ImmutableMap.of());

        DefaultDriver child = new DefaultDriver(CHILD, Lists.newArrayList(root), MFR, HW, SW,
                ImmutableMap.of(), ImmutableMap.of(KEY, VALUE));

        DefaultDriver grandChild = new DefaultDriver(GRAND_CHILD, Lists.newArrayList(child),
                MFR, HW, SW, ImmutableMap.of(), ImmutableMap.of());

        assertNull(root.getProperty(KEY));
        assertEquals(VALUE, child.getProperty(KEY));
        assertEquals(VALUE, grandChild.getProperty(KEY));
    }
}
