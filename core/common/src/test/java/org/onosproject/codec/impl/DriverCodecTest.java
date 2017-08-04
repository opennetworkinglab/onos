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
package org.onosproject.codec.impl;


import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.TestBehaviour;
import org.onosproject.net.driver.TestBehaviourImpl;
import org.onosproject.net.driver.TestBehaviourTwo;
import org.onosproject.net.driver.TestBehaviourTwoImpl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.codec.impl.DriverJsonMatcher.matchesDriver;

/**
 * Unit tests for the driver codec.
 */
public class DriverCodecTest {

    @Test
    public void codecTest() {
        Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours =
            ImmutableMap.of(TestBehaviour.class,
                    TestBehaviourImpl.class,
                    TestBehaviourTwo.class,
                    TestBehaviourTwoImpl.class);
        Map<String, String> properties =
                ImmutableMap.of("key1", "value1", "key2", "value2");

        DefaultDriver parent = new DefaultDriver("parent", new ArrayList<>(), "Acme",
                "HW1.2.3", "SW1.2.3",
                behaviours,
                properties);
        DefaultDriver child = new DefaultDriver("child", ImmutableList.of(parent), "Acme",
                "HW1.2.3.1", "SW1.2.3.1",
                behaviours,
                properties);

        MockCodecContext context = new MockCodecContext();
        ObjectNode driverJson = context.codec(Driver.class).encode(child, context);

        assertThat(driverJson, matchesDriver(child));
    }
}
