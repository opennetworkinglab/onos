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

import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DOT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DROP;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.IPV4_HEADER_NAME;

/**
 * Unit tests for PiTableEntry class.
 */
public class PiTableEntryTest {
    private final PiTableEntry piTableEntry1 = PiTableEntry.builder()
            .forTable(PiTableId.of("Table10"))
            .withCookie(0xac)
            .withPriority(10)
            .withAction(PiAction.builder().withId(PiActionId.of(DROP)).build())
            .withTimeout(100)
            .build();
    private final PiTableEntry sameAsPiTableEntry1 = PiTableEntry.builder()
            .forTable(PiTableId.of("Table10"))
            .withCookie(0xac)
            .withPriority(10)
            .withAction(PiAction.builder().withId(PiActionId.of(DROP)).build())
            .withTimeout(100)
            .build();
    private final PiTableEntry piTableEntry2 = PiTableEntry.builder()
            .forTable(PiTableId.of("Table20"))
            .withCookie(0xac)
            .withPriority(10)
            .withAction(PiAction.builder().withId(PiActionId.of(DROP)).build())
            .withTimeout(1000)
            .build();

    /**
     * Checks that the PiTableEntry class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiTableEntry.class);
    }

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piTableEntry1, sameAsPiTableEntry1)
                .addEqualityGroup(piTableEntry2)
                .testEquals();
    }

    /**
     * Tests creation of a DefaultFlowRule using a FlowRule constructor.
     */
    @Test
    public void testBuilder() {

        PiTableId piTableId = PiTableId.of("table10");
        long cookie = 0xfff0323;
        int priority = 100;
        double timeout = 1000;
        PiMatchFieldId piMatchFieldId = PiMatchFieldId.of(IPV4_HEADER_NAME + DOT + DST_ADDR);
        PiFieldMatch piFieldMatch = new PiExactFieldMatch(piMatchFieldId, ImmutableByteSequence.copyFrom(0x0a010101));
        PiAction piAction = PiAction.builder().withId(PiActionId.of(DROP)).build();
        final Map<PiMatchFieldId, PiFieldMatch> fieldMatches = Maps.newHashMap();
        fieldMatches.put(piMatchFieldId, piFieldMatch);
        final PiTableEntry piTableEntry = PiTableEntry.builder()
                .forTable(piTableId)
                .withMatchKey(PiMatchKey.builder()
                                      .addFieldMatches(fieldMatches.values())
                                      .build())
                .withAction(piAction)
                .withCookie(cookie)
                .withPriority(priority)
                .withTimeout(timeout)
                .build();

        assertThat(piTableEntry.table(), is(piTableId));
        assertThat(piTableEntry.cookie(), is(cookie));
        assertThat("Priority must be set", piTableEntry.priority().isPresent());
        assertThat("Timeout must be set", piTableEntry.timeout().isPresent());
        assertThat(piTableEntry.priority().getAsInt(), is(priority));
        assertThat(piTableEntry.timeout().get(), is(timeout));
        assertThat("Incorrect match param value",
                   CollectionUtils.isEqualCollection(piTableEntry.matchKey().fieldMatches(), fieldMatches.values()));
        assertThat(piTableEntry.action(), is(piAction));
    }
}
