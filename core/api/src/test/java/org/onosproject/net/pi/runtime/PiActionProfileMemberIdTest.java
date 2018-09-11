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
 * Unit tests for PiActionProfileMemberId class.
 */
public class PiActionProfileMemberIdTest {

    final PiActionProfileMemberId piActionProfileMemberId1 = PiActionProfileMemberId.of(10);
    final PiActionProfileMemberId sameAsPiActionProfileMemberId1 = PiActionProfileMemberId.of(10);
    final PiActionProfileMemberId piActionProfileMemberId2 = PiActionProfileMemberId.of(20);

    /**
     * Checks that the PiActionProfileMemberId class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiActionProfileMemberId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(piActionProfileMemberId1, sameAsPiActionProfileMemberId1)
                .addEqualityGroup(piActionProfileMemberId2)
                .testEquals();
    }

    /**
     * Checks the methods of PiActionProfileMemberId.
     */
    @Test
    public void testMethods() {

        assertThat(piActionProfileMemberId1, is(notNullValue()));
        assertThat(piActionProfileMemberId1.type(), is(PiTableAction.Type.ACTION_PROFILE_MEMBER_ID));
        assertThat(piActionProfileMemberId1.id(), is(10));
    }
}
