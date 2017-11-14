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
import org.onosproject.net.pi.model.PiControlMetadataId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.pi.runtime.PiConstantsTest.EGRESS_PORT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.INGRESS_PORT;

/**
 * Unit tests for PiControlMetadataId class.
 */
public class PiControlMetadataIdTest {

    final PiControlMetadataId piControlMetadataId1 = PiControlMetadataId.of(EGRESS_PORT);
    final PiControlMetadataId sameAsPiControlMetadataId1 = PiControlMetadataId.of(EGRESS_PORT);
    final PiControlMetadataId piControlMetadataId2 = PiControlMetadataId.of(INGRESS_PORT);

    /**
     * Checks that the PiControlMetadataId class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiControlMetadataId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(piControlMetadataId1, sameAsPiControlMetadataId1)
                .addEqualityGroup(piControlMetadataId2)
                .testEquals();
    }

    /**
     * Checks the methods of PiControlMetadataId.
     */
    @Test
    public void testMethods() {
        assertThat(piControlMetadataId1, is(notNullValue()));
        assertThat(piControlMetadataId1.id(), is(EGRESS_PORT));
    }
}
