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
import static org.onosproject.net.pi.runtime.PiConstantsTest.VID;
import static org.onosproject.net.pi.runtime.PiConstantsTest.VLAN_HEADER_NAME;

/**
 * Unit tests for PiRangeFieldMatch class.
 */
public class PiRangeFieldMatchTest {
    private final ImmutableByteSequence high1 = copyFrom(0x10);
    private final ImmutableByteSequence low1 = copyFrom(0x00);
    private final ImmutableByteSequence high2 = copyFrom(0x30);
    private final ImmutableByteSequence low2 = copyFrom(0x40);

    private final PiMatchFieldId piMatchField = PiMatchFieldId.of(VLAN_HEADER_NAME + DOT + VID);
    private PiRangeFieldMatch piRangeFieldMatch1 = new PiRangeFieldMatch(piMatchField, low1, high1);
    private PiRangeFieldMatch sameAsPiRangeFieldMatch1 = new PiRangeFieldMatch(piMatchField, low1, high1);
    private PiRangeFieldMatch piRangeFieldMatch2 = new PiRangeFieldMatch(piMatchField, low2, high2);

    /**
     * Checks that the PiRangeFieldMatch class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiRangeFieldMatch.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piRangeFieldMatch1, sameAsPiRangeFieldMatch1)
                .addEqualityGroup(piRangeFieldMatch2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiRangeFieldMatch object.
     */
    @Test
    public void testConstruction() {
        final ImmutableByteSequence high = copyFrom(0x50);
        final ImmutableByteSequence low = copyFrom(0x00);
        final PiMatchFieldId piMatchField = PiMatchFieldId.of(VLAN_HEADER_NAME + DOT + VID);
        PiRangeFieldMatch piRangeFieldMatch = new PiRangeFieldMatch(piMatchField, low, high);
        assertThat(piRangeFieldMatch, is(notNullValue()));
        assertThat(piRangeFieldMatch.lowValue(), is(low));
        assertThat(piRangeFieldMatch.highValue(), is(high));
        assertThat(piRangeFieldMatch.type(), is(PiMatchType.RANGE));
    }
}
