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

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiMatchFieldId;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DOT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.IPV4_HEADER_NAME;
import static org.onosproject.net.pi.runtime.PiConstantsTest.SRC_ADDR;

/**
 * Unit tests for PiMatchKey class.
 */
public class PiMatchKeyTest {

    private final ImmutableByteSequence value1 = copyFrom(0x0a010101);
    private final ImmutableByteSequence value2 = copyFrom(0x0a010102);
    private int prefixLength = 24;
    private final PiMatchFieldId piMatchField1 = PiMatchFieldId.of(IPV4_HEADER_NAME + DOT + SRC_ADDR);
    private final PiMatchFieldId piMatchField2 = PiMatchFieldId.of(IPV4_HEADER_NAME + DOT + DST_ADDR);
    private PiLpmFieldMatch piLpmFieldMatch1 = new PiLpmFieldMatch(piMatchField1, value1, prefixLength);
    private PiLpmFieldMatch piLpmFieldMatch2 = new PiLpmFieldMatch(piMatchField2, value2, prefixLength);

    private final PiMatchKey piMatchKey1 = PiMatchKey.builder()
            .addFieldMatch(piLpmFieldMatch1)
            .build();
    private final PiMatchKey sameAsPiMatchKey1 = PiMatchKey.builder()
            .addFieldMatch(piLpmFieldMatch1)
            .build();
    private final PiMatchKey piMatchKey2 = PiMatchKey.builder()
            .addFieldMatch(piLpmFieldMatch2)
            .build();

    /**
     * Checks that the PiMatchKey class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiMatchKey.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(piMatchKey1, sameAsPiMatchKey1)
                .addEqualityGroup(PiMatchKey.EMPTY, PiMatchKey.EMPTY)
                .addEqualityGroup(piMatchKey2)
                .testEquals();
    }

    /**
     * Checks the methods of PiMatchKey.
     */
    @Test
    public void testMethods() {

        Collection<PiFieldMatch> piFieldMatches = Lists.newArrayList();
        piFieldMatches.add(piLpmFieldMatch1);
        piFieldMatches.add(piLpmFieldMatch2);

        final PiMatchKey piMatchKey = PiMatchKey.builder()
                .addFieldMatches(piFieldMatches)
                .build();

        assertThat(piMatchKey, is(notNullValue()));
        assertThat("Incorrect members value",
                   CollectionUtils.isEqualCollection(piMatchKey.fieldMatches(), piFieldMatches));
    }
}
