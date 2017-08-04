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

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DefaultDriverHandlerTest {

    DefaultDriver ddc;
    DefaultDriverData data;
    DefaultDriverHandler handler;

    @Before
    public void setUp() {
        ddc = new DefaultDriver("foo.bar", new ArrayList<>(), "Circus", "lux", "1.2a",
                                ImmutableMap.of(TestBehaviour.class,
                                                TestBehaviourImpl.class,
                                                TestBehaviourTwo.class,
                                                TestBehaviourTwoImpl.class),
                                ImmutableMap.of("foo", "bar"));
        data = new DefaultDriverData(ddc, DefaultDriverDataTest.DEVICE_ID);
        handler = new DefaultDriverHandler(data);
    }

    @Test
    public void basics() {
        assertSame("incorrect data", data, handler.data());
        assertTrue("incorrect toString", handler.toString().contains("1.2a"));
    }

    @Test
    public void behaviour() {
        TestBehaviourTwo behaviour = handler.behaviour(TestBehaviourTwo.class);
        assertTrue("incorrect behaviour", behaviour instanceof TestBehaviourTwoImpl);
    }

}