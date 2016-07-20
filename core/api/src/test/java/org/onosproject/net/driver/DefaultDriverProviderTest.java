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
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;

public class DefaultDriverProviderTest {

    @Test
    public void basics() {
        DefaultDriverProvider ddp = new DefaultDriverProvider();
        DefaultDriver one = new DefaultDriver("foo.bar", new ArrayList<>(), "Circus", "lux", "1.2a",
                                              ImmutableMap.of(TestBehaviour.class,
                                                              TestBehaviourImpl.class),
                                              ImmutableMap.of("foo", "bar"));
        DefaultDriver two = new DefaultDriver("foo.bar", new ArrayList<>(), "", "", "",
                                              ImmutableMap.of(TestBehaviourTwo.class,
                                                              TestBehaviourTwoImpl.class),
                                              ImmutableMap.of("goo", "wee"));
        DefaultDriver three = new DefaultDriver("goo.foo", new ArrayList<>(), "BigTop", "better", "2.2",
                                                ImmutableMap.of(TestBehaviourTwo.class,
                                                                TestBehaviourTwoImpl.class),
                                                ImmutableMap.of("goo", "gee"));

        ddp.addDrivers(of(one, two, three));

        Set<Driver> drivers = ddp.getDrivers();
        assertEquals("incorrect types", 2, drivers.size());
    }
}