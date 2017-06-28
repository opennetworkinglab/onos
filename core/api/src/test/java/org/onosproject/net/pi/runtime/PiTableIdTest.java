/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.runtime;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for PiTableId class.
 */
public class PiTableIdTest {
    final String table10 = "table10";
    final String table20 = "table20";
    final PiTableId piTableId1 = PiTableId.of(table10);
    final PiTableId sameAsPiTableId1 = PiTableId.of(table10);
    final PiTableId piTableId2 = PiTableId.of(table20);

    final String tableScope = "local";
    final PiTableId piTableIdWithScope1 = PiTableId.of(tableScope, table10);
    final PiTableId sameAsPiTableIdWithScope1 = PiTableId.of(tableScope, table10);
    final PiTableId piTableIdWithScope2 = PiTableId.of(tableScope, table20);
    /**
     * Checks that the PiTableId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiTableId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piTableId1, sameAsPiTableId1)
                .addEqualityGroup(piTableId2)
                .testEquals();
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEqualsWithScope() {
        new EqualsTester()
                .addEqualityGroup(piTableIdWithScope1, sameAsPiTableIdWithScope1)
                .addEqualityGroup(piTableIdWithScope2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiTableId object.
     */
    @Test
    public void testConstruction() {
        final String name = "table1";
        final PiTableId piTableId = PiTableId.of(name);
        assertThat(piTableId, is(notNullValue()));
        assertThat(piTableId.name(), is(name));
    }

    /**
     * Checks the construction of a PiTableId object.
     */
    @Test
    public void testConstructionWithScope() {
        final String name = "table1";
        final String scope = "local";
        final PiTableId piTableId = PiTableId.of(scope, name);
        assertThat(piTableId, is(notNullValue()));
        assertThat(piTableId.name(), is(name));
        assertThat(piTableId.scope().get(), is(scope));
    }
}
