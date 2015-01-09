/*
 * Copyright 2015 Open Networking Laboratory
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultDriverTest {

    @Test
    public void basics() {
        DefaultDriver ddc = new DefaultDriver("foo.bar", "Circus", "lux", "1.2a",
                                              ImmutableMap.of(TestBehaviour.class,
                                                              TestBehaviourImpl.class),
                                              ImmutableMap.of("foo", "bar"));
        assertEquals("incorrect name", "foo.bar", ddc.name());
        assertEquals("incorrect mfr", "Circus", ddc.manufacturer());
        assertEquals("incorrect hw", "lux", ddc.hwVersion());
        assertEquals("incorrect sw", "1.2a", ddc.swVersion());

        assertEquals("incorrect behaviour count", 1, ddc.behaviours().size());
        assertTrue("incorrect behaviour", ddc.hasBehaviour(TestBehaviour.class));

        assertEquals("incorrect property count", 1, ddc.properties().size());
        assertEquals("incorrect key count", 1, ddc.keys().size());
        assertEquals("incorrect property", "bar", ddc.value("foo"));

        assertTrue("incorrect toString", ddc.toString().contains("lux"));
    }

    @Test
    public void merge() {
        DefaultDriver one = new DefaultDriver("foo.bar", "Circus", "lux", "1.2a",
                                              ImmutableMap.of(TestBehaviour.class,
                                                              TestBehaviourImpl.class),
                                              ImmutableMap.of("foo", "bar"));
        DefaultDriver ddc =
                one.merge(new DefaultDriver("foo.bar", "", "", "",
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
}