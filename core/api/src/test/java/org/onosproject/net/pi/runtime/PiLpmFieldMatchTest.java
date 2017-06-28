/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.net.pi.model.PiMatchType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.IPV4_HEADER_NAME;

/**
 * Unit tests for PiLpmFieldMatch class.
 */
public class PiLpmFieldMatchTest {
    final ImmutableByteSequence value1 = copyFrom(0x0a010101);
    final ImmutableByteSequence value2 = copyFrom(0x0a010102);
    int prefixLength = 24;
    final PiHeaderFieldId piHeaderField = PiHeaderFieldId.of(IPV4_HEADER_NAME, DST_ADDR);
    PiLpmFieldMatch piLpmFieldMatch1 = new PiLpmFieldMatch(piHeaderField, value1, prefixLength);
    PiLpmFieldMatch sameAsPiLpmFieldMatch1 = new PiLpmFieldMatch(piHeaderField, value1, prefixLength);
    PiLpmFieldMatch piLpmFieldMatch2 = new PiLpmFieldMatch(piHeaderField, value2, prefixLength);

    /**
     * Checks that the PiLpmFieldMatch class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiLpmFieldMatch.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piLpmFieldMatch1, sameAsPiLpmFieldMatch1)
                .addEqualityGroup(piLpmFieldMatch2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiLpmFieldMatch object.
     */
    @Test
    public void testConstruction() {
        final ImmutableByteSequence value = copyFrom(0x0a01010a);
        int prefix = 24;
        final PiHeaderFieldId piHeaderField = PiHeaderFieldId.of(IPV4_HEADER_NAME, DST_ADDR);
        PiLpmFieldMatch piLpmFieldMatch = new PiLpmFieldMatch(piHeaderField, value, prefix);
        assertThat(piLpmFieldMatch, is(notNullValue()));
        assertThat(piLpmFieldMatch.value(), is(value));
        assertThat(piLpmFieldMatch.prefixLength(), is(prefix));
        assertThat(piLpmFieldMatch.type(), is(PiMatchType.LPM));
    }
}
