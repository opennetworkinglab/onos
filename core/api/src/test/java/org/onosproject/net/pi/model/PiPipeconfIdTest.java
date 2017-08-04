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

package org.onosproject.net.pi.model;


import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for PiPipeconfId class.
 */
public class PiPipeconfIdTest {
    final String id1 = "pipeline1";
    final String id2 = "pipeline2";
    final PiPipeconfId piPipeconfId1 = new PiPipeconfId(id1);
    final PiPipeconfId sameAsPiPipeconfId1 = new PiPipeconfId(id1);
    final PiPipeconfId piPipeconfId2 = new PiPipeconfId(id2);

    /**
     * Checks that the PiPipeconfId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiPipeconfId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piPipeconfId1, sameAsPiPipeconfId1)
                .addEqualityGroup(piPipeconfId2)
                .testEquals();
    }
}
