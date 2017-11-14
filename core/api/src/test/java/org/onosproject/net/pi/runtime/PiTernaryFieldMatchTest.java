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
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DOT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.IPV4_HEADER_NAME;

/**
 * Unit tests for PiTernaryFieldMatch class.
 */
public class PiTernaryFieldMatchTest {
    private final ImmutableByteSequence value1 = copyFrom(0x0a010101);
    private final ImmutableByteSequence mask1 = copyFrom(0x00ffffff);
    private final ImmutableByteSequence value2 = copyFrom(0x0a010102);
    private final ImmutableByteSequence mask2 = copyFrom(0x0000ffff);

    private final PiMatchFieldId piMatchField = PiMatchFieldId.of(IPV4_HEADER_NAME + DOT + DST_ADDR);
    private PiTernaryFieldMatch piTernaryFieldMatch1 = new PiTernaryFieldMatch(piMatchField, value1, mask1);
    private PiTernaryFieldMatch sameAsPiTernaryFieldMatch1 = new PiTernaryFieldMatch(piMatchField, value1, mask1);
    private PiTernaryFieldMatch piTernaryFieldMatch2 = new PiTernaryFieldMatch(piMatchField, value2, mask2);

    /**
     * Checks that the PiTernaryFieldMatch class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiTernaryFieldMatch.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piTernaryFieldMatch1, sameAsPiTernaryFieldMatch1)
                .addEqualityGroup(piTernaryFieldMatch2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiTernaryFieldMatch object.
     */
    @Test
    public void testConstruction() {
        final ImmutableByteSequence value = copyFrom(0x0a01010a);
        final ImmutableByteSequence mask = copyFrom(0x00ffffff);
        final PiMatchFieldId piMatchField = PiMatchFieldId.of(IPV4_HEADER_NAME + DOT + DST_ADDR);
        PiTernaryFieldMatch piTernaryFieldMatch = new PiTernaryFieldMatch(piMatchField, value, mask);
        assertThat(piTernaryFieldMatch, is(notNullValue()));
        assertThat(piTernaryFieldMatch.value(), is(value));
        assertThat(piTernaryFieldMatch.mask(), is(mask));
        assertThat(piTernaryFieldMatch.type(), is(PiMatchType.TERNARY));
    }
}
