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
import org.onosproject.net.pi.model.PiMatchFieldId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DOT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.ETH_HEADER_NAME;
import static org.onosproject.net.pi.runtime.PiConstantsTest.ETH_TYPE;
import static org.onosproject.net.pi.runtime.PiConstantsTest.IPV4_HEADER_NAME;

/**
 * Unit tests for PiMatchFieldId class.
 */
public class PiMatchFieldIdTest {
    private final String headerName = ETH_HEADER_NAME;
    private final String dstAddr = DST_ADDR;
    private final String etherType = ETH_TYPE;

    private final PiMatchFieldId piMatchFieldId1 = PiMatchFieldId.of(headerName + DOT + dstAddr);
    private final PiMatchFieldId sameAsPiMatchFieldId1 = PiMatchFieldId.of(headerName + DOT + dstAddr);
    private final PiMatchFieldId piMatchFieldId2 = PiMatchFieldId.of(headerName + DOT + etherType);

    private int index = 10;
    private final PiMatchFieldId piMatchFieldId1WithIndex = PiMatchFieldId
            .of(headerName + DOT + dstAddr + "[" + index + "]");
    private final PiMatchFieldId sameAsPiMatchFieldId1WithIndex = PiMatchFieldId
            .of(headerName + DOT + dstAddr + "[" + index + "]");
    private final PiMatchFieldId piMatchFieldId2WithIndex = PiMatchFieldId
            .of(headerName + DOT + etherType + "[" + index + "]");

    /**
     * Checks that the PiMatchFieldId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiMatchFieldId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piMatchFieldId1, sameAsPiMatchFieldId1)
                .addEqualityGroup(piMatchFieldId2)
                .testEquals();
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEqualsWithIndex() {
        new EqualsTester()
                .addEqualityGroup(piMatchFieldId1WithIndex, sameAsPiMatchFieldId1WithIndex)
                .addEqualityGroup(piMatchFieldId2WithIndex)
                .testEquals();
    }

    /**
     * Checks the construction of a PiMatchFieldId object.
     */
    @Test
    public void testConstruction() {
        final String name = IPV4_HEADER_NAME  + DOT + DST_ADDR;
        final PiMatchFieldId piMatchFieldId = PiMatchFieldId.of(name);
        assertThat(piMatchFieldId, is(notNullValue()));
        assertThat(piMatchFieldId.id(), is(name));
    }

    /**
     * Checks the construction of a PiMatchFieldId object with index.
     */
    @Test
    public void testConstructionWithIndex() {
        final String name = IPV4_HEADER_NAME + DOT + DST_ADDR + "[1]";
        final PiMatchFieldId piMatchFieldId = PiMatchFieldId.of(name);
        assertThat(piMatchFieldId, is(notNullValue()));
        assertThat(piMatchFieldId.id(), is(name));
    }
}
