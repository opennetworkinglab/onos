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

import com.google.common.testing.EqualsTester;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * MapEvent unit tests.
 */
public class MapEventTest {

    private final Versioned<Integer> vStatsNew = new Versioned<>(2, 2);
    private final Versioned<Integer> vStatsOld = new Versioned<>(1, 1);

    private final MapEvent<String, Integer> stats1 =
            new MapEvent<>(MapEvent.Type.INSERT, "a", "1", vStatsNew, null);

    private final MapEvent<String, Integer> stats2 =
            new MapEvent<>(MapEvent.Type.REMOVE, "a", "1", null, vStatsOld);

    private final MapEvent<String, Integer> stats3 =
            new MapEvent<>(MapEvent.Type.UPDATE, "a", "1", vStatsNew, vStatsOld);

    /**
     * Tests the creation of the MapEvent object.
     */
    @Test
    public void testConstruction() {
        assertThat(stats1.name(), is("a"));
        assertThat(stats1.type(), is(MapEvent.Type.INSERT));
        assertThat(stats1.key(), is("1"));
        assertThat(stats1.newValue(), is(vStatsNew));
        assertThat(stats1.oldValue(), is((Versioned<Integer>) null));

        assertThat(stats2.name(), is("a"));
        assertThat(stats2.type(), is(MapEvent.Type.REMOVE));
        assertThat(stats2.key(), is("1"));
        assertThat(stats2.newValue(), is((Versioned<Integer>) null));
        assertThat(stats2.oldValue(), is(vStatsOld));

        assertThat(stats3.name(), is("a"));
        assertThat(stats3.type(), is(MapEvent.Type.UPDATE));
        assertThat(stats3.key(), is("1"));
        assertThat(stats3.newValue(), is(vStatsNew));
        assertThat(stats3.oldValue(), is(vStatsOld));
    }

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(stats1, stats1)
                .addEqualityGroup(stats2)
                .addEqualityGroup(stats3)
                .testEquals();
    }

}
