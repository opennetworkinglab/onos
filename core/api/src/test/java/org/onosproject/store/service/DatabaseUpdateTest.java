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
package org.onosproject.store.service;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit Tests for DatabseUpdate class.
 */

public class DatabaseUpdateTest {

    private final DatabaseUpdate stats1 = DatabaseUpdate.newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(DatabaseUpdate.Type.PUT)
            .build();

    private final DatabaseUpdate stats2 = DatabaseUpdate.newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(DatabaseUpdate.Type.REMOVE)
            .build();

    private final DatabaseUpdate stats3 = DatabaseUpdate.newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(DatabaseUpdate.Type.REMOVE_IF_VALUE_MATCH)
            .build();

    private final DatabaseUpdate stats4 = DatabaseUpdate.newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(DatabaseUpdate.Type.REMOVE_IF_VERSION_MATCH)
            .build();

    private final DatabaseUpdate stats5 = DatabaseUpdate.newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(DatabaseUpdate.Type.PUT_IF_VALUE_MATCH)
            .build();

    private final DatabaseUpdate stats6 = DatabaseUpdate.newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(DatabaseUpdate.Type.PUT_IF_VERSION_MATCH)
            .build();

    /**
     *  Tests the constructor for the class.
     */
    @Test
    public void testConstruction() {
        assertThat(stats1.currentValue(), is("1".getBytes()));
        assertThat(stats1.value(), is("2".getBytes()));
        assertThat(stats1.currentVersion(), is(3L));
        assertThat(stats1.key(), is("4"));
        assertThat(stats1.mapName(), is("5"));
        assertThat(stats1.type(), is(DatabaseUpdate.Type.PUT));
    }

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(stats1, stats1)
                .addEqualityGroup(stats2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(stats3, stats3)
                .addEqualityGroup(stats4)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(stats5, stats5)
                .addEqualityGroup(stats6)
                .testEquals();
    }

    /**
     * Tests if the toString method returns a consistent value for hashing.
     */
    @Test
    public void testToString() {
        assertThat(stats1.toString(), is(stats1.toString()));
    }

}
