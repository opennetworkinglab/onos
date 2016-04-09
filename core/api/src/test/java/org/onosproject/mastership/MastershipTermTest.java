/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.mastership;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;
import org.onosproject.cluster.NodeId;

import com.google.common.testing.EqualsTester;

public class MastershipTermTest {

    private static final NodeId N1 = new NodeId("foo");
    private static final NodeId N2 = new NodeId("bar");

    private static final MastershipTerm TERM1 = MastershipTerm.of(N1, 0);
    private static final MastershipTerm TERM2 = MastershipTerm.of(N2, 1);
    private static final MastershipTerm TERM3 = MastershipTerm.of(N2, 1);
    private static final MastershipTerm TERM4 = MastershipTerm.of(N1, 1);

    @Test
    public void basics() {
        assertEquals("incorrect term number", 0, TERM1.termNumber());
        assertEquals("incorrect master", new NodeId("foo"), TERM1.master());
    }

    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(MastershipTerm.of(N1, 0), TERM1)
        .addEqualityGroup(TERM2, TERM3)
        .addEqualityGroup(TERM4)
        .testEquals();
    }

    /**
     * Checks that the MembershipTerm class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(MastershipTerm.class);
    }
}
