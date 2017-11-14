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
import static org.onosproject.net.pi.runtime.PiConstantsTest.ETH_HEADER_NAME;
import static org.onosproject.net.pi.runtime.PiConstantsTest.ETH_TYPE;

/**
 * Unit tests for PiExactFieldMatch class.
 */
public class PiExactFieldMatchTest {

    private final ImmutableByteSequence value1 = copyFrom(0x0800);
    private final ImmutableByteSequence value2 = copyFrom(0x0806);
    private final PiMatchFieldId piMatchField = PiMatchFieldId.of(ETH_HEADER_NAME + DOT + ETH_TYPE);
    private PiExactFieldMatch piExactFieldMatch1 = new PiExactFieldMatch(piMatchField, value1);
    private PiExactFieldMatch sameAsPiExactFieldMatch1 = new PiExactFieldMatch(piMatchField, value1);
    private PiExactFieldMatch piExactFieldMatch2 = new PiExactFieldMatch(piMatchField, value2);

    /**
     * Checks that the PiExactFieldMatch class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiExactFieldMatch.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piExactFieldMatch1, sameAsPiExactFieldMatch1)
                .addEqualityGroup(piExactFieldMatch2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiExactFieldMatch object.
     */
    @Test
    public void testConstruction() {
        final ImmutableByteSequence value = copyFrom(0x0806);
        final PiMatchFieldId piMatchField = PiMatchFieldId.of(ETH_HEADER_NAME + DOT + ETH_TYPE);
        PiExactFieldMatch piExactFieldMatch = new PiExactFieldMatch(piMatchField, value);
        assertThat(piExactFieldMatch, is(notNullValue()));
        assertThat(piExactFieldMatch.value(), is(value));
        assertThat(piExactFieldMatch.type(), is(PiMatchType.EXACT));
    }
}
