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
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.EGRESS_PORT;

/**
 * Unit tests for PiControlMetadata class.
 */
public class PiControlMetadataTest {

    final PiControlMetadataId piControlMetadataId = PiControlMetadataId.of(EGRESS_PORT);

    final PiControlMetadata piControlMetadata1 = PiControlMetadata.builder()
            .withId(piControlMetadataId)
            .withValue(copyFrom(0x10))
            .build();
    final PiControlMetadata sameAsPiControlMetadata1 = PiControlMetadata.builder()
            .withId(piControlMetadataId)
            .withValue(copyFrom(0x10))
            .build();
    final PiControlMetadata piControlMetadata2 = PiControlMetadata.builder()
            .withId(piControlMetadataId)
            .withValue(copyFrom(0x20))
            .build();

    /**
     * Checks that the PiControlMetadata class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiControlMetadata.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(piControlMetadata1, sameAsPiControlMetadata1)
                .addEqualityGroup(piControlMetadata2)
                .testEquals();
    }

    /**
     * Checks the methods of PiControlMetadata.
     */
    @Test
    public void testMethods() {

        assertThat(piControlMetadata1, is(notNullValue()));
        assertThat(piControlMetadata1.id(), is(PiControlMetadataId.of(EGRESS_PORT)));
        assertThat(piControlMetadata1.value(), is(copyFrom(0x10)));
    }
}
