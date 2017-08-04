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
package org.onosproject.store.service;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;

/**
 * Unit tests for the EventuallyConsistentMapEvent class.
 */
public class EventuallyConsistentMapEventTest {

    EventuallyConsistentMapEvent<String, String> event1 =
            new EventuallyConsistentMapEvent<>("map1", PUT, "k1", "v1");
    EventuallyConsistentMapEvent<String, String> event2 =
            new EventuallyConsistentMapEvent<>("map1", REMOVE, "k1", "v1");
    EventuallyConsistentMapEvent<String, String> sameAsEvent2 =
            new EventuallyConsistentMapEvent<>("map1", REMOVE, "k1", "v1");
    EventuallyConsistentMapEvent<String, String> event3 =
            new EventuallyConsistentMapEvent<>("map1", PUT, "k2", "v1");
    EventuallyConsistentMapEvent<String, String> event4 =
            new EventuallyConsistentMapEvent<>("map1", PUT, "k1", "v2");
    EventuallyConsistentMapEvent<String, String> event5 =
            new EventuallyConsistentMapEvent<>("map2", REMOVE, "k1", "v2");
    EventuallyConsistentMapEvent<String, String> event6 =
            new EventuallyConsistentMapEvent<>("map3", REMOVE, "k1", "v2");


    /**
     * Checks the equals(), hashCode() and toString() operations.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(event1)
                .addEqualityGroup(event2, sameAsEvent2)
                .addEqualityGroup(event3)
                .addEqualityGroup(event4)
                .addEqualityGroup(event5)
                .addEqualityGroup(event6)
                .testEquals();
    }

    /**
     * Checks that the EventuallyConsistentMapEvent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(EventuallyConsistentMapEvent.class);
    }

    /**
     * Checks that construction of the object is correct.
     */
    @Test
    public void testConstruction() {
        assertThat(event1.type(), is(PUT));
        assertThat(event1.key(), is("k1"));
        assertThat(event1.value(), is("v1"));
        assertThat(event1.name(), is("map1"));
    }
}
