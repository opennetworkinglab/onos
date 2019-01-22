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
import org.onosproject.net.pi.model.PiPacketMetadataId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.EGRESS_PORT;

/**
 * Unit tests for PiPacketMetadata class.
 */
public class PiPacketMetadataTest {

    final PiPacketMetadataId piPacketMetadataId = PiPacketMetadataId.of(EGRESS_PORT);

    final PiPacketMetadata piPacketMetadata1 = PiPacketMetadata.builder()
            .withId(piPacketMetadataId)
            .withValue(copyFrom(0x10))
            .build();
    final PiPacketMetadata sameAsPiPacketMetadata1 = PiPacketMetadata.builder()
            .withId(piPacketMetadataId)
            .withValue(copyFrom(0x10))
            .build();
    final PiPacketMetadata piPacketMetadata2 = PiPacketMetadata.builder()
            .withId(piPacketMetadataId)
            .withValue(copyFrom(0x20))
            .build();

    /**
     * Checks that the PiPacketMetadata class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiPacketMetadata.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(piPacketMetadata1, sameAsPiPacketMetadata1)
                .addEqualityGroup(piPacketMetadata2)
                .testEquals();
    }

    /**
     * Checks the methods of PiPacketMetadata.
     */
    @Test
    public void testMethods() {

        assertThat(piPacketMetadata1, is(notNullValue()));
        assertThat(piPacketMetadata1.id(), is(PiPacketMetadataId.of(EGRESS_PORT)));
        assertThat(piPacketMetadata1.value(), is(copyFrom(0x10)));
    }
}
