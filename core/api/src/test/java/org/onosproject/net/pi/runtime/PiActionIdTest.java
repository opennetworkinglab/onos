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
import org.onosproject.net.pi.model.PiActionId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DEC_TTL;
import static org.onosproject.net.pi.runtime.PiConstantsTest.MOD_VLAN_VID;

/**
 * Unit tests for PiActionId class.
 */
public class PiActionIdTest {
    final String id1 = DEC_TTL;
    final String id2 = MOD_VLAN_VID;
    final PiActionId actionId1 = PiActionId.of(id1);
    final PiActionId sameAsActionId1 = PiActionId.of(id1);
    final PiActionId actionId2 = PiActionId.of(id2);

    /**
     * Checks that the PiActionId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiActionId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(actionId1, sameAsActionId1)
                .addEqualityGroup(actionId2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiActionId object.
     */
    @Test
    public void testConstruction() {
        final PiActionId actionId = PiActionId.of(DEC_TTL);
        assertThat(actionId, is(notNullValue()));
        assertThat(actionId.id(), is(DEC_TTL));
    }
}
