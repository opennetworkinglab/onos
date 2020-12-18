/*
 * Copyright 2020-present Open Networking Foundation
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
import static org.onosproject.net.pi.runtime.PiConstantsTest.*;

/**
 * Unit tests for PiOptionalFieldMatch class.
 */
public class PiOptionalFieldMatchTest {

    private final ImmutableByteSequence value1 = copyFrom(0x0800);
    private final ImmutableByteSequence value2 = copyFrom(0x0806);
    private final PiMatchFieldId piMatchField = PiMatchFieldId.of(ETH_HEADER_NAME + DOT + ETH_TYPE);
    private PiOptionalFieldMatch piOptionalFieldMatch1 = new PiOptionalFieldMatch(piMatchField, value1);
    private PiOptionalFieldMatch sameAsPiOptionalFieldMatch1 = new PiOptionalFieldMatch(piMatchField, value1);
    private PiOptionalFieldMatch piOptionalFieldMatch2 = new PiOptionalFieldMatch(piMatchField, value2);

    /**
     * Checks that the PiOptionalFieldMatch class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiOptionalFieldMatch.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piOptionalFieldMatch1, sameAsPiOptionalFieldMatch1)
                .addEqualityGroup(piOptionalFieldMatch2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiOptionalFieldMatch object.
     */
    @Test
    public void testConstruction() {
        final ImmutableByteSequence value = copyFrom(0x0806);
        final PiMatchFieldId piMatchField = PiMatchFieldId.of(ETH_HEADER_NAME + DOT + ETH_TYPE);
        PiOptionalFieldMatch piOptionalFieldMatch = new PiOptionalFieldMatch(piMatchField, value);
        assertThat(piOptionalFieldMatch, is(notNullValue()));
        assertThat(piOptionalFieldMatch.value(), is(value));
        assertThat(piOptionalFieldMatch.type(), is(PiMatchType.OPTIONAL));
    }
}
