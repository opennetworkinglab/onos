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
package org.onosproject.vtnrsc;

import java.util.UUID;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for PortPairId class.
 */
public class PortPairIdTest {

    final PortPairId portPairId1 = PortPairId.of("78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae");
    final PortPairId sameAsPortPairId1 = PortPairId.of("78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae");
    final PortPairId portPairId2 = PortPairId.of("dace4513-24fc-4fae-af4b-321c5e2eb3d1");

    /**
     * Checks that the PortPairId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PortPairId.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(portPairId1, sameAsPortPairId1).addEqualityGroup(portPairId2).testEquals();
    }

    /**
     * Checks the construction of a PortPairId object.
     */
    @Test
    public void testConstruction() {
        final String portPairIdValue = "dace4513-24fc-4fae-af4b-321c5e2eb3d1";
        final PortPairId portPairId = PortPairId.of(portPairIdValue);
        assertThat(portPairId, is(notNullValue()));
        assertThat(portPairId.value(), is(UUID.fromString(portPairIdValue)));
    }
}
