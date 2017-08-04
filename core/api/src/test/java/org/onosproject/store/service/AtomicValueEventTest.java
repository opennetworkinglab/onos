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
import static org.onosproject.store.service.AtomicValueEvent.Type.UPDATE;

/**
 * Unit tests for the AtomicValueEvent class.
 */
public class AtomicValueEventTest {

    AtomicValueEvent<String> event1 =
            new AtomicValueEvent<>("map1", "e1", "e0");
    AtomicValueEvent<String> event2 =
            new AtomicValueEvent<>("map1", "e2", "e1");
    AtomicValueEvent<String> sameAsEvent2 =
            new AtomicValueEvent<>("map1", "e2", "e1");
    AtomicValueEvent<String> event3 =
            new AtomicValueEvent<>("map2", "e2", "e1");

    /**
     * Checks that the SetEvent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(AtomicValueEvent.class);
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
                .testEquals();
    }

    /**
     * Checks that construction of the object is correct.
     */
    @Test
    public void testConstruction() {
        assertThat(event1.type(), is(UPDATE));
        assertThat(event1.newValue(), is("e1"));
        assertThat(event1.oldValue(), is("e0"));
        assertThat(event1.name(), is("map1"));
    }

}
