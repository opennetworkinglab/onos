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

package org.onosproject.net.flow.criteria;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiOptionalFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.util.ImmutableByteSequence.copyFrom;

/**
 * Unit tests for the PiCriteria class.
 */
public class PiCriteriaTest {

    private PiMatchFieldId ethMatchFieldId = PiMatchFieldId.of("ethernet_t.etherType");
    private byte[] matchExactBytes1 = {0x08, 0x00};
    private byte[] matchExactBytes2 = {0x08, 0x06};
    private Criterion matchPiExactByte1 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactBytes1).build();
    private Criterion sameAsMatchPiExactByte1 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactBytes1).build();
    private Criterion matchPiExactByte2 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactBytes2).build();

    private short matchExactShort1 = 0x800;
    private short matchExactShort2 = 0x806;
    private Criterion matchPiExactShort1 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactShort1).build();
    private Criterion sameAsMatchPiExactShort1 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactShort1).build();
    private Criterion matchPiExactShort2 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactShort2).build();

    private int matchExactInt1 = 0x800;
    private int matchExactInt2 = 0x806;
    private Criterion matchPiExactInt1 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactInt1).build();
    private Criterion sameAsMatchPiExactInt1 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactInt1).build();
    private Criterion matchPiExactInt2 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactInt2).build();

    private long matchExactLong1 = 0x800;
    private long matchExactLong2 = 0x806;
    private Criterion matchPiExactLong1 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactLong1).build();
    private Criterion sameAsMatchPiExactLong1 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactLong1).build();
    private Criterion matchPiExactLong2 = PiCriterion.builder()
            .matchExact(ethMatchFieldId, matchExactLong2).build();

    private Criterion matchPiOptionalByte1 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchExactBytes1).build();
    private Criterion sameAsMatchPiOptionalByte1 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchExactBytes1).build();
    private Criterion matchPiOptionalByte2 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchExactBytes2).build();

    private short matchOptionalShort1 = 0x800;
    private short matchOptionalShort2 = 0x806;
    private Criterion matchPiOptionalShort1 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalShort1).build();
    private Criterion sameAsMatchPiOptionalShort1 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalShort1).build();
    private Criterion matchPiOptionalShort2 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalShort2).build();

    private int matchOptionalInt1 = 0x800;
    private int matchOptionalInt2 = 0x806;
    private Criterion matchPiOptionalInt1 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalInt1).build();
    private Criterion sameAsMatchPiOptionalInt1 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalInt1).build();
    private Criterion matchPiOptionalInt2 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalInt2).build();

    private long matchOptionalLong1 = 0x800;
    private long matchOptionalLong2 = 0x806;
    private Criterion matchPiOptionalLong1 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalLong1).build();
    private Criterion sameAsMatchPiOptionalLong1 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalLong1).build();
    private Criterion matchPiOptionalLong2 = PiCriterion.builder()
            .matchOptional(ethMatchFieldId, matchOptionalLong2).build();

    private PiMatchFieldId ipv4MatchFieldId = PiMatchFieldId.of("ipv4_t.dstAddr");
    private int mask = 0x00ffffff;
    private byte[] matchLpmBytes1 = {0x0a, 0x01, 0x01, 0x01};
    private byte[] matchLpmBytes2 = {0x0a, 0x01, 0x01, 0x02};
    private Criterion matchPiLpmByte1 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmBytes1, mask).build();
    private Criterion sameAsMatchPiLpmByte1 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmBytes1, mask).build();
    private Criterion matchPiLpmByte2 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmBytes2, mask).build();

    private short matchLpmShort1 = 0x0a0a;
    private short matchLpmShort2 = 0x0a0b;
    private Criterion matchPiLpmShort1 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmShort1, mask).build();
    private Criterion sameAsMatchPiLpmShort1 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmShort1, mask).build();
    private Criterion matchPiLpmShort2 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmShort2, mask).build();

    private int matchLpmInt1 = 0x0a010101;
    private int matchLpmInt2 = 0x0a010102;
    private Criterion matchPiLpmInt1 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmInt1, mask).build();
    private Criterion sameAsMatchPiLpmInt1 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmInt1, mask).build();
    private Criterion matchPiLpmInt2 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmInt2, mask).build();

    private long matchLpmLong1 = 0x0a010101;
    private long matchLpmLong2 = 0x0a010102;
    private Criterion matchPiLpmLong1 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmLong1, mask).build();
    private Criterion sameAsMatchPiLpmLong1 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmLong1, mask).build();
    private Criterion matchPiLpmLong2 = PiCriterion.builder()
            .matchLpm(ipv4MatchFieldId, matchLpmLong2, mask).build();


    private byte[] matchTernaryBytes1 = {0x0a, 0x01, 0x01, 0x01};
    private byte[] matchTernaryBytes2 = {0x0a, 0x01, 0x01, 0x02};
    private byte[] matchTernaryMaskBytes = {0x7f, 0x7f, 0x7f, 0x00};
    private Criterion matchPiTernaryByte1 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryBytes1, matchTernaryMaskBytes).build();
    private Criterion sameAsMatchPiTernaryByte1 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryBytes1, matchTernaryMaskBytes).build();
    private Criterion matchPiTernaryByte2 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryBytes2, matchTernaryMaskBytes).build();

    private short matchTernaryShort1 = 0x0a0a;
    private short matchTernaryShort2 = 0x0a0b;
    private short matchTernaryMaskShort = 0xff0;
    private Criterion matchPiTernaryShort1 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryShort1, matchTernaryMaskShort).build();
    private Criterion sameAsMatchPiTernaryShort1 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryShort1, matchTernaryMaskShort).build();
    private Criterion matchPiTernaryShort2 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryShort2, matchTernaryMaskShort).build();

    private int matchTernaryInt1 = 0x0a010101;
    private int matchTernaryInt2 = 0x0a010102;
    private int matchTernaryMaskInt = 0xffff;
    private Criterion matchPiTernaryInt1 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryInt1, matchTernaryMaskInt).build();
    private Criterion sameAsMatchPiTernaryInt1 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryInt1, matchTernaryMaskInt).build();
    private Criterion matchPiTernaryInt2 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryInt2, matchTernaryMaskInt).build();

    private long matchTernaryLong1 = 0x0a010101;
    private long matchTernaryLong2 = 0x0a010102;
    private long matchTernaryMaskLong = 0xffff;
    private Criterion matchPiTernaryLong1 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryLong1, matchTernaryMaskLong).build();
    private Criterion sameAsMatchPiTernaryLong1 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryLong1, matchTernaryMaskLong).build();
    private Criterion matchPiTernaryLong2 = PiCriterion.builder()
            .matchTernary(ipv4MatchFieldId, matchTernaryLong2, matchTernaryMaskLong).build();

    private byte[] matchRangeBytes1 = {0x10};
    private byte[] matchRangeBytes2 = {0x20};
    private byte[] matchRangeHighBytes = {0x30};
    private Criterion matchPiRangeByte1 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeBytes1, matchRangeHighBytes).build();
    private Criterion sameAsMatchPiRangeByte1 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeBytes1, matchRangeHighBytes).build();
    private Criterion matchPiRangeByte2 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeBytes2, matchRangeHighBytes).build();

    private short matchRangeShort1 = 0x100;
    private short matchRangeShort2 = 0x200;
    private short matchRangeHighShort = 0x300;
    private Criterion matchPiRangeShort1 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeShort1, matchRangeHighShort).build();
    private Criterion sameAsMatchPiRangeShort1 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeShort1, matchRangeHighShort).build();
    private Criterion matchPiRangeShort2 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeShort2, matchRangeHighShort).build();

    private int matchRangeInt1 = 0x100;
    private int matchRangeInt2 = 0x200;
    private int matchRangeHighInt = 0x300;
    private Criterion matchPiRangeInt1 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeInt1, matchRangeHighInt).build();
    private Criterion sameAsMatchPiRangeInt1 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeInt1, matchRangeHighInt).build();
    private Criterion matchPiRangeInt2 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeInt2, matchRangeHighInt).build();

    private long matchRangeLong1 = 0x100;
    private long matchRangeLong2 = 0x200;
    private long matchRangeHighLong = 0x300;
    private Criterion matchPiRangeLong1 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeLong1, matchRangeHighLong).build();
    private Criterion sameAsMatchPiRangeLong1 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeLong1, matchRangeHighLong).build();
    private Criterion matchPiRangeLong2 = PiCriterion.builder()
            .matchRange(ipv4MatchFieldId, matchRangeLong2, matchRangeHighLong).build();

    /**
     * Checks that a Criterion object has the proper type, and then converts it to the proper type.
     *
     * @param criterion Criterion object to convert
     * @param type      Enumerated type value for the Criterion class
     * @param clazz     Desired Criterion class
     * @param <T>       The type the caller wants returned
     * @return converted object
     */
    @SuppressWarnings("unchecked")
    private <T> T checkAndConvert(Criterion criterion, Criterion.Type type, Class clazz) {
        assertThat(criterion, is(notNullValue()));
        assertThat(criterion.type(), is(equalTo(type)));
        assertThat(criterion, instanceOf(clazz));
        return (T) criterion;
    }

    /**
     * Test the ExactMatchPi method.
     */
    @Test
    public void testExactMatchPiMethod() {

        Criterion matchPiBytes = PiCriterion.builder().matchExact(ethMatchFieldId, matchExactBytes1).build();
        PiCriterion piCriterionBytes = checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchBytes = new PiExactFieldMatch(ethMatchFieldId, copyFrom(matchExactBytes1));
        assertThat(piCriterionBytes.fieldMatches().iterator().next(), is(expectedMatchBytes));

        Criterion matchPiShort = PiCriterion.builder().matchExact(ethMatchFieldId, matchExactShort1).build();
        PiCriterion piCriterionShort = checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchShort = new PiExactFieldMatch(ethMatchFieldId, copyFrom(matchExactShort1));
        assertThat(piCriterionShort.fieldMatches().iterator().next(), is(expectedMatchShort));

        Criterion matchPiInt = PiCriterion.builder().matchExact(ethMatchFieldId, matchExactInt1).build();
        PiCriterion piCriterionInt = checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                     PiCriterion.class);
        PiFieldMatch expectedMatchInt = new PiExactFieldMatch(ethMatchFieldId, copyFrom(matchExactInt1));
        assertThat(piCriterionInt.fieldMatches().iterator().next(), is(expectedMatchInt));

        Criterion matchPiLong = PiCriterion.builder().matchExact(ethMatchFieldId, matchExactLong1).build();
        PiCriterion piCriterionLong = checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                      PiCriterion.class);
        PiFieldMatch expectedMatchLong = new PiExactFieldMatch(ethMatchFieldId, copyFrom(matchExactLong1));
        assertThat(piCriterionLong.fieldMatches().iterator().next(), is(expectedMatchLong));
    }

    /**
     * Test the OptionalMatchPi method.
     */
    @Test
    public void testOptionalMatchPiMethod() {

        Criterion matchPiBytes = PiCriterion.builder().matchOptional(ethMatchFieldId, matchExactBytes1).build();
        PiCriterion piCriterionBytes = checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchBytes = new PiOptionalFieldMatch(ethMatchFieldId, copyFrom(matchExactBytes1));
        assertThat(piCriterionBytes.fieldMatches().iterator().next(), is(expectedMatchBytes));

        Criterion matchPiShort = PiCriterion.builder().matchOptional(ethMatchFieldId, matchExactShort1).build();
        PiCriterion piCriterionShort = checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchShort = new PiOptionalFieldMatch(ethMatchFieldId, copyFrom(matchExactShort1));
        assertThat(piCriterionShort.fieldMatches().iterator().next(), is(expectedMatchShort));

        Criterion matchPiInt = PiCriterion.builder().matchOptional(ethMatchFieldId, matchExactInt1).build();
        PiCriterion piCriterionInt = checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                     PiCriterion.class);
        PiFieldMatch expectedMatchInt = new PiOptionalFieldMatch(ethMatchFieldId, copyFrom(matchExactInt1));
        assertThat(piCriterionInt.fieldMatches().iterator().next(), is(expectedMatchInt));

        Criterion matchPiLong = PiCriterion.builder().matchOptional(ethMatchFieldId, matchExactLong1).build();
        PiCriterion piCriterionLong = checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                      PiCriterion.class);
        PiFieldMatch expectedMatchLong = new PiOptionalFieldMatch(ethMatchFieldId, copyFrom(matchExactLong1));
        assertThat(piCriterionLong.fieldMatches().iterator().next(), is(expectedMatchLong));
    }

    /**
     * Test the LpmMatchPi method.
     */
    @Test
    public void testLpmMatchPiMethod() {

        Criterion matchPiBytes = PiCriterion.builder().matchLpm(ipv4MatchFieldId, matchLpmBytes1, mask).build();
        PiCriterion piCriterionBytes = checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchBytes = new PiLpmFieldMatch(ipv4MatchFieldId, copyFrom(matchLpmBytes1), mask);
        assertThat(piCriterionBytes.fieldMatches().iterator().next(), is(expectedMatchBytes));

        Criterion matchPiShort = PiCriterion.builder().matchLpm(ipv4MatchFieldId, matchLpmShort1, mask).build();
        PiCriterion piCriterionShort = checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchShort = new PiLpmFieldMatch(ipv4MatchFieldId, copyFrom(matchLpmShort1), mask);
        assertThat(piCriterionShort.fieldMatches().iterator().next(), is(expectedMatchShort));

        Criterion matchPiInt = PiCriterion.builder().matchLpm(ipv4MatchFieldId, matchLpmInt1, mask).build();
        PiCriterion piCriterionInt = checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                     PiCriterion.class);
        PiFieldMatch expectedMatchInt = new PiLpmFieldMatch(ipv4MatchFieldId, copyFrom(matchLpmInt1), mask);
        assertThat(piCriterionInt.fieldMatches().iterator().next(), is(expectedMatchInt));

        Criterion matchPiLong = PiCriterion.builder().matchLpm(ipv4MatchFieldId, matchLpmLong1, mask).build();
        PiCriterion piCriterionLong = checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                      PiCriterion.class);
        PiFieldMatch expectedMatchLong = new PiLpmFieldMatch(ipv4MatchFieldId, copyFrom(matchLpmLong1), mask);
        assertThat(piCriterionLong.fieldMatches().iterator().next(), is(expectedMatchLong));
    }

    /**
     * Test the TernaryMatchPi method.
     */
    @Test
    public void testTernaryMatchPiMethod() {

        Criterion matchPiBytes = PiCriterion.builder().matchTernary(ipv4MatchFieldId, matchTernaryBytes1,
                                                                    matchTernaryMaskBytes)
                .build();
        PiCriterion piCriterionBytes = checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchBytes = new PiTernaryFieldMatch(ipv4MatchFieldId, copyFrom(matchTernaryBytes1),
                                                                  copyFrom(matchTernaryMaskBytes));
        assertThat(piCriterionBytes.fieldMatches().iterator().next(), is(expectedMatchBytes));

        Criterion matchPiShort = PiCriterion.builder().matchTernary(ipv4MatchFieldId, matchTernaryShort1,
                                                                    matchTernaryMaskShort)
                .build();
        PiCriterion piCriterionShort = checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchShort = new PiTernaryFieldMatch(ipv4MatchFieldId, copyFrom(matchTernaryShort1),
                                                                  copyFrom(matchTernaryMaskShort));
        assertThat(piCriterionShort.fieldMatches().iterator().next(), is(expectedMatchShort));

        Criterion matchPiInt = PiCriterion.builder().matchTernary(ipv4MatchFieldId, matchTernaryInt1,
                                                                  matchTernaryMaskInt)
                .build();
        PiCriterion piCriterionInt = checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                     PiCriterion.class);
        PiFieldMatch expectedMatchInt = new PiTernaryFieldMatch(ipv4MatchFieldId, copyFrom(matchTernaryInt1),
                                                                copyFrom(matchTernaryMaskInt));
        assertThat(piCriterionInt.fieldMatches().iterator().next(), is(expectedMatchInt));

        Criterion matchPiLong = PiCriterion.builder().matchTernary(ipv4MatchFieldId, matchTernaryLong1,
                                                                   matchTernaryMaskLong)
                .build();
        PiCriterion piCriterionLong = checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                      PiCriterion.class);
        PiFieldMatch expectedMatchLong = new PiTernaryFieldMatch(ipv4MatchFieldId, copyFrom(matchTernaryLong1),
                                                                 copyFrom(matchTernaryMaskLong));
        assertThat(piCriterionLong.fieldMatches().iterator().next(), is(expectedMatchLong));
    }

    /**
     * Test the RangeMatchPi method.
     */
    @Test
    public void testRangeMatchPiMethod() {

        Criterion matchPiBytes = PiCriterion.builder().matchRange(ipv4MatchFieldId, matchRangeBytes1,
                                                                  matchRangeHighBytes)
                .build();
        PiCriterion piCriterionBytes = checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchBytes = new PiRangeFieldMatch(ipv4MatchFieldId, copyFrom(matchRangeBytes1),
                                                                copyFrom(matchRangeHighBytes));
        assertThat(piCriterionBytes.fieldMatches().iterator().next(), is(expectedMatchBytes));

        Criterion matchPiShort = PiCriterion.builder().matchRange(ipv4MatchFieldId, matchRangeShort1,
                                                                  matchRangeHighShort)
                .build();
        PiCriterion piCriterionShort = checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                       PiCriterion.class);
        PiFieldMatch expectedMatchShort = new PiRangeFieldMatch(ipv4MatchFieldId, copyFrom(matchRangeShort1),
                                                                copyFrom(matchRangeHighShort));
        assertThat(piCriterionShort.fieldMatches().iterator().next(), is(expectedMatchShort));

        Criterion matchPiInt = PiCriterion.builder().matchRange(ipv4MatchFieldId, matchRangeInt1,
                                                                matchRangeHighInt)
                .build();
        PiCriterion piCriterionInt = checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                     PiCriterion.class);
        PiFieldMatch expectedMatchInt = new PiRangeFieldMatch(ipv4MatchFieldId, copyFrom(matchRangeInt1),
                                                              copyFrom(matchRangeHighInt));
        assertThat(piCriterionInt.fieldMatches().iterator().next(), is(expectedMatchInt));

        Criterion matchPiLong = PiCriterion.builder().matchRange(ipv4MatchFieldId, matchRangeLong1,
                                                                 matchRangeHighLong)
                .build();
        PiCriterion piCriterionLong = checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT,
                                                      PiCriterion.class);
        PiFieldMatch expectedMatchLong = new PiRangeFieldMatch(ipv4MatchFieldId, copyFrom(matchRangeLong1),
                                                               copyFrom(matchRangeHighLong));
        assertThat(piCriterionLong.fieldMatches().iterator().next(), is(expectedMatchLong));
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiExactCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiExactByte1, sameAsMatchPiExactByte1)
                .addEqualityGroup(matchPiExactByte2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiExactShort1, sameAsMatchPiExactShort1)
                .addEqualityGroup(matchPiExactShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiExactInt1, sameAsMatchPiExactInt1)
                .addEqualityGroup(matchPiExactInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiExactLong1, sameAsMatchPiExactLong1)
                .addEqualityGroup(matchPiExactLong2)
                .testEquals();
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiOptionalCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiOptionalByte1, sameAsMatchPiOptionalByte1)
                .addEqualityGroup(matchPiOptionalByte2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiOptionalShort1, sameAsMatchPiOptionalShort1)
                .addEqualityGroup(matchPiOptionalShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiOptionalInt1, sameAsMatchPiOptionalInt1)
                .addEqualityGroup(matchPiOptionalInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiOptionalLong1, sameAsMatchPiOptionalLong1)
                .addEqualityGroup(matchPiOptionalLong2)
                .testEquals();
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiLpmCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiLpmByte1, sameAsMatchPiLpmByte1)
                .addEqualityGroup(matchPiLpmByte2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiLpmShort1, sameAsMatchPiLpmShort1)
                .addEqualityGroup(matchPiLpmShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiLpmInt1, sameAsMatchPiLpmInt1)
                .addEqualityGroup(matchPiLpmInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiLpmLong1, sameAsMatchPiLpmLong1)
                .addEqualityGroup(matchPiLpmLong2)
                .testEquals();
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiTernaryCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiTernaryByte1, sameAsMatchPiTernaryByte1)
                .addEqualityGroup(matchPiTernaryByte2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiTernaryShort1, sameAsMatchPiTernaryShort1)
                .addEqualityGroup(matchPiTernaryShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiTernaryInt1, sameAsMatchPiTernaryInt1)
                .addEqualityGroup(matchPiTernaryInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiTernaryLong1, sameAsMatchPiTernaryLong1)
                .addEqualityGroup(matchPiTernaryLong2)
                .testEquals();
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiRangeCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiRangeByte1, sameAsMatchPiRangeByte1)
                .addEqualityGroup(matchPiRangeByte2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiRangeShort1, sameAsMatchPiRangeShort1)
                .addEqualityGroup(matchPiRangeShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiRangeInt1, sameAsMatchPiRangeInt1)
                .addEqualityGroup(matchPiRangeInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiRangeLong1, sameAsMatchPiRangeLong1)
                .addEqualityGroup(matchPiRangeLong2)
                .testEquals();
    }
}
