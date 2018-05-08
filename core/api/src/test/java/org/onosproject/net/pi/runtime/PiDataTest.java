/*
 * Copyright 2018-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiData;
import org.onosproject.net.pi.runtime.data.PiBitString;
import org.onosproject.net.pi.runtime.data.PiBool;
import org.onosproject.net.pi.runtime.data.PiEnumString;
import org.onosproject.net.pi.runtime.data.PiErrorString;
import org.onosproject.net.pi.runtime.data.PiHeader;
import org.onosproject.net.pi.runtime.data.PiHeaderStack;
import org.onosproject.net.pi.runtime.data.PiHeaderUnion;
import org.onosproject.net.pi.runtime.data.PiHeaderUnionStack;
import org.onosproject.net.pi.runtime.data.PiStruct;
import org.onosproject.net.pi.runtime.data.PiTuple;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for PiData class.
 */
public class PiDataTest {
    private final PiData bitString1 = PiBitString.of(ImmutableByteSequence.copyFrom(10));
    private final PiData sameAsBitString1 = PiBitString.of(ImmutableByteSequence.copyFrom(10));
    private final PiData bitString2 = PiBitString.of(ImmutableByteSequence.copyFrom(20));

    private final PiData bool1 = PiBool.of(true);
    private final PiData sameAsBool1 = PiBool.of(true);
    private final PiData bool2 = PiBool.of(false);

    private final PiData tuple1 = PiTuple.of(Lists.newArrayList(bool1));
    private final PiData sameAsTuple1 = PiTuple.of(Lists.newArrayList(bool1));
    private final PiData tuple2 = PiTuple.of(Lists.newArrayList(bitString1));

    private final PiData struct1 = PiStruct.of(Lists.newArrayList(bool1));
    private final PiData sameAsStruct1 = PiStruct.of(Lists.newArrayList(bool1));
    private final PiData struct2 = PiStruct.of(Lists.newArrayList(bitString1));

    private final PiData header1 = PiHeader.of(true,
                                               Lists.newArrayList(ImmutableByteSequence.copyFrom(10)));
    private final PiData sameAsHeader1 = PiHeader.of(true,
                                                     Lists.newArrayList(ImmutableByteSequence.copyFrom(10)));
    private final PiData header2 = PiHeader.of(true,
                                               Lists.newArrayList(ImmutableByteSequence.copyFrom(20)));

    private final PiData headerUnion1 = PiHeaderUnion.of("port", (PiHeader) header1);
    private final PiData sameAsHeaderUnion1 = PiHeaderUnion.of("port", (PiHeader) header1);
    private final PiData headerUnion2 =  PiHeaderUnion.of("port", (PiHeader) header2);

    private final PiData inValidHeaderUnion1 = PiHeaderUnion.ofInvalid();
    private final PiData sameAsInvalidHeaderUnion1 = PiHeaderUnion.ofInvalid();

    private final PiData headerStack1 = PiHeaderStack.of(Lists.newArrayList((PiHeader) header1));
    private final PiData sameAsHeaderStack1 = PiHeaderStack.of(Lists.newArrayList((PiHeader) header1));
    private final PiData headerStack2 = PiHeaderStack.of(Lists.newArrayList((PiHeader) header2));

    private final PiData headerUnionStack1 = PiHeaderUnionStack.of(Lists.newArrayList(
            (PiHeaderUnion) headerUnion1));
    private final PiData sameAsHeaderUnionStack1 = PiHeaderUnionStack.of(Lists.newArrayList(
            (PiHeaderUnion) headerUnion1));
    private final PiData headerUnionStack2 = PiHeaderUnionStack.of(Lists.newArrayList(
            (PiHeaderUnion) headerUnion2));

    private final PiData enumString1 = PiEnumString.of("test");
    private final PiData sameAsEnumString1 = PiEnumString.of("test");
    private final PiData enumString2 = PiEnumString.of("test1");

    private final PiData errorString1 = PiErrorString.of("failed");
    private final PiData sameAsErrorString1 = PiErrorString.of("failed");
    private final PiData errorString2 = PiErrorString.of("success");

    /**
     * Checks that the PiData classes is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiBitString.class);
        assertThatClassIsImmutable(PiBool.class);
        assertThatClassIsImmutable(PiTuple.class);
        assertThatClassIsImmutable(PiStruct.class);
        assertThatClassIsImmutable(PiHeader.class);
        assertThatClassIsImmutable(PiHeaderStack.class);
        assertThatClassIsImmutable(PiHeaderUnion.class);
        assertThatClassIsImmutable(PiHeaderUnionStack.class);
        assertThatClassIsImmutable(PiEnumString.class);
        assertThatClassIsImmutable(PiErrorString.class);
    }

    /**
     * Checks the PiData type.
     */
    @Test
    public void testPiDataType() {
        assertEquals(bitString1.type(), PiData.Type.BITSTRING);
        assertEquals(bool1.type(), PiData.Type.BOOL);
        assertEquals(tuple1.type(), PiData.Type.TUPLE);
        assertEquals(struct1.type(), PiData.Type.STRUCT);
        assertEquals(header1.type(), PiData.Type.HEADER);
        assertEquals(headerUnion1.type(), PiData.Type.HEADERUNION);
        assertEquals(headerStack1.type(), PiData.Type.HEADERSTACK);
        assertEquals(headerUnionStack1.type(), PiData.Type.HEADERUNIONSTACK);
        assertEquals(enumString1.type(), PiData.Type.ENUMSTRING);
        assertEquals(errorString1.type(), PiData.Type.ERRORSTRING);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(bitString1, sameAsBitString1)
                .addEqualityGroup(bitString2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(bool1, sameAsBool1)
                .addEqualityGroup(bool2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(tuple1, sameAsTuple1)
                .addEqualityGroup(tuple2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(struct1, sameAsStruct1)
                .addEqualityGroup(struct2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(header1, sameAsHeader1)
                .addEqualityGroup(header2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(headerUnion1, sameAsHeaderUnion1)
                .addEqualityGroup(headerUnion2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(inValidHeaderUnion1, sameAsInvalidHeaderUnion1)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(headerStack1, sameAsHeaderStack1)
                .addEqualityGroup(headerStack2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(headerUnionStack1, sameAsHeaderUnionStack1)
                .addEqualityGroup(headerUnionStack2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(enumString1, sameAsEnumString1)
                .addEqualityGroup(enumString2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(errorString1, sameAsErrorString1)
                .addEqualityGroup(errorString2)
                .testEquals();
    }

}
