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

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiPacketMetadataId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.model.PiPacketOperationType.PACKET_OUT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.EGRESS_PORT;

/**
 * Unit tests for PiPacketOperation class.
 */
public class PiPacketOperationTest {

    private final PiPacketOperation piPacketOperation1 = PiPacketOperation.builder()
            .withData(ImmutableByteSequence.ofOnes(512))
            .withType(PACKET_OUT)
            .withMetadata(PiPacketMetadata.builder()
                                  .withId(PiPacketMetadataId.of(EGRESS_PORT))
                                  .withValue(copyFrom((short) 255))
                                  .build())
            .build();

    private final PiPacketOperation sameAsPiPacketOperation1 = PiPacketOperation.builder()
            .withData(ImmutableByteSequence.ofOnes(512))
            .withType(PACKET_OUT)
            .withMetadata(PiPacketMetadata.builder()
                                  .withId(PiPacketMetadataId.of(EGRESS_PORT))
                                  .withValue(copyFrom((short) 255))
                                  .build())
            .build();

    private final PiPacketOperation piPacketOperation2 = PiPacketOperation.builder()
            .withData(ImmutableByteSequence.ofOnes(512))
            .withType(PACKET_OUT)
            .withMetadata(PiPacketMetadata.builder()
                                  .withId(PiPacketMetadataId.of(EGRESS_PORT))
                                  .withValue(copyFrom((short) 200))
                                  .build())
            .build();

    /**
     * Checks that the PiPacketOperation class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiPacketOperation.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(piPacketOperation1, sameAsPiPacketOperation1)
                .addEqualityGroup(piPacketOperation2)
                .testEquals();
    }

    /**
     * Checks the methods of PiPacketOperation.
     */
    @Test
    public void testMethods() {

        final PiPacketOperation piPacketOperation = PiPacketOperation.builder()
                .withData(ImmutableByteSequence.ofOnes(512))
                .withType(PACKET_OUT)
                .withMetadata(PiPacketMetadata.builder()
                                      .withId(PiPacketMetadataId.of(EGRESS_PORT))
                                      .withValue(copyFrom((short) 10))
                                      .build())
                .build();

        assertThat(piPacketOperation, is(notNullValue()));
        assertThat(piPacketOperation.type(), is(PACKET_OUT));
        assertThat(piPacketOperation.data(), is(ImmutableByteSequence.ofOnes(512)));
        assertThat("Incorrect metadatas value",
                   CollectionUtils.isEqualCollection(piPacketOperation.metadatas(),
                                                     ImmutableList.of(PiPacketMetadata.builder()
                                                                              .withId(PiPacketMetadataId
                                                                                              .of(EGRESS_PORT))
                                                                              .withValue(copyFrom((short) 10))
                                                                              .build())));
    }
}
