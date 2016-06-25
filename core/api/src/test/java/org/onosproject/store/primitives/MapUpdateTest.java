/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.primitives;

import com.google.common.testing.EqualsTester;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit Tests for MapUpdate class.
 */

public class MapUpdateTest {

    private final MapUpdate<String, byte[]> stats1 = MapUpdate.<String, byte[]>newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(MapUpdate.Type.PUT)
            .build();

    private final MapUpdate<String, byte[]> stats2 = MapUpdate.<String, byte[]>newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(MapUpdate.Type.REMOVE)
            .build();

    private final MapUpdate<String, byte[]> stats3 = MapUpdate.<String, byte[]>newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(MapUpdate.Type.REMOVE_IF_VALUE_MATCH)
            .build();

    private final MapUpdate<String, byte[]> stats4 = MapUpdate.<String, byte[]>newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
            .build();

    private final MapUpdate<String, byte[]> stats5 = MapUpdate.<String, byte[]>newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(MapUpdate.Type.PUT_IF_VALUE_MATCH)
            .build();

    private final MapUpdate<String, byte[]> stats6 = MapUpdate.<String, byte[]>newBuilder()
            .withCurrentValue("1".getBytes())
            .withValue("2".getBytes())
            .withCurrentVersion(3)
            .withKey("4")
            .withMapName("5")
            .withType(MapUpdate.Type.PUT_IF_VERSION_MATCH)
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
        assertThat(stats1.type(), is(MapUpdate.Type.PUT));
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
