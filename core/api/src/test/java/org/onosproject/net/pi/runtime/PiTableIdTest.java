/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onosproject.net.pi.model.PiTableId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for PiTableId class.
 */
public class PiTableIdTest {
    private final String table10 = "table10";
    private final String table20 = "table20";
    private final PiTableId piTableId1 = PiTableId.of(table10);
    private final PiTableId sameAsPiTableId1 = PiTableId.of(table10);
    private final PiTableId piTableId2 = PiTableId.of(table20);

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
     * Checks the construction of a PiTableId object.
     */
    @Test
    public void testConstruction() {
        assertThat(piTableId1, is(notNullValue()));
        assertThat(piTableId1.id(), is(table10));
    }
}
