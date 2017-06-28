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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.ETH_HEADER_NAME;
import static org.onosproject.net.pi.runtime.PiConstantsTest.ETH_TYPE;
import static org.onosproject.net.pi.runtime.PiConstantsTest.IPV4_HEADER_NAME;

/**
 * Unit tests for PiHeaderFieldId class.
 */
public class PiHeaderFieldIdTest {
    final String headerName = ETH_HEADER_NAME;
    final String dstAddr = DST_ADDR;
    final String etherType = ETH_TYPE;

    final PiHeaderFieldId piHeaderFieldId1 = PiHeaderFieldId.of(headerName, dstAddr);
    final PiHeaderFieldId sameAsPiHeaderFieldId1 = PiHeaderFieldId.of(headerName, dstAddr);
    final PiHeaderFieldId piHeaderFieldId2 = PiHeaderFieldId.of(headerName, etherType);

    int index = 10;
    final PiHeaderFieldId piHeaderFieldId1WithIndex = PiHeaderFieldId.of(headerName, dstAddr, index);
    final PiHeaderFieldId sameAsPiHeaderFieldId1WithIndex = PiHeaderFieldId.of(headerName, dstAddr, index);
    final PiHeaderFieldId piHeaderFieldId2WithIndex = PiHeaderFieldId.of(headerName, etherType, index);

    /**
     * Checks that the PiHeaderFieldId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiHeaderFieldId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piHeaderFieldId1, sameAsPiHeaderFieldId1)
                .addEqualityGroup(piHeaderFieldId2)
                .testEquals();
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEqualsWithIndex() {
        new EqualsTester()
                .addEqualityGroup(piHeaderFieldId1WithIndex, sameAsPiHeaderFieldId1WithIndex)
                .addEqualityGroup(piHeaderFieldId2WithIndex)
                .testEquals();
    }

    /**
     * Checks the construction of a PiHeaderFieldId object.
     */
    @Test
    public void testConstruction() {
        final String name = IPV4_HEADER_NAME;
        final String field = DST_ADDR;

        final PiHeaderFieldId piHeaderFieldId = PiHeaderFieldId.of(name, field);
        assertThat(piHeaderFieldId, is(notNullValue()));
        assertThat(piHeaderFieldId.headerName(), is(name));
        assertThat(piHeaderFieldId.fieldName(), is(field));
    }

    /**
     * Checks the construction of a PiHeaderFieldId object with index.
     */
    @Test
    public void testConstructionWithIndex() {
        final String name = IPV4_HEADER_NAME;
        final String field = DST_ADDR;
        final int index = 1;
        final PiHeaderFieldId piHeaderFieldId = PiHeaderFieldId.of(name, field, index);
        assertThat(piHeaderFieldId, is(notNullValue()));
        assertThat(piHeaderFieldId.headerName(), is(name));
        assertThat(piHeaderFieldId.fieldName(), is(field));
        assertThat(piHeaderFieldId.index(), is(index));
    }
}
