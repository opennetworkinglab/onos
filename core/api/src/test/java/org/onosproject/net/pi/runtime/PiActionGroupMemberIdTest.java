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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for PiActionGroupMemberId class.
 */
public class PiActionGroupMemberIdTest {

    final PiActionGroupMemberId piActionGroupMemberId1 = PiActionGroupMemberId.of(10);
    final PiActionGroupMemberId sameAsPiActionGroupMemberId1 = PiActionGroupMemberId.of(10);
    final PiActionGroupMemberId piActionGroupMemberId2 = PiActionGroupMemberId.of(20);

    /**
     * Checks that the PiActionGroupMemberId class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiActionGroupMemberId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(piActionGroupMemberId1, sameAsPiActionGroupMemberId1)
                .addEqualityGroup(piActionGroupMemberId2)
                .testEquals();
    }

    /**
     * Checks the methods of PiActionGroupMemberId.
     */
    @Test
    public void testMethods() {

        assertThat(piActionGroupMemberId1, is(notNullValue()));
        assertThat(piActionGroupMemberId1.type(), is(PiTableAction.Type.GROUP_MEMBER_ID));
        assertThat(piActionGroupMemberId1.id(), is(10));
    }
}
