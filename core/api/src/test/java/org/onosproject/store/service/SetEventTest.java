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
import static org.onosproject.store.service.SetEvent.Type.ADD;
import static org.onosproject.store.service.SetEvent.Type.REMOVE;

/**
 * Unit tests for the SetEvent class.
 */
public class SetEventTest {

    SetEvent<String> event1 =
            new SetEvent<>("map1", ADD, "e1");
    SetEvent<String> event2 =
            new SetEvent<>("map1", REMOVE, "e1");
    SetEvent<String> sameAsEvent2 =
            new SetEvent<>("map1", REMOVE, "e1");
    SetEvent<String> event3 =
            new SetEvent<>("map1", ADD, "e2");
    SetEvent<String> event4 =
            new SetEvent<>("map1", REMOVE, "e2");

    /**
     * Checks that the SetEvent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(EventuallyConsistentMapEvent.class);
    }

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
                .testEquals();
    }

    /**
     * Checks that construction of the object is correct.
     */
    @Test
    public void testConstruction() {
        assertThat(event1.type(), is(ADD));
        assertThat(event1.entry(), is("e1"));
        assertThat(event1.name(), is("map1"));
    }

}
