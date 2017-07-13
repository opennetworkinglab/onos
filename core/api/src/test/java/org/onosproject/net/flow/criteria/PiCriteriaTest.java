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

package org.onosproject.net.flow.criteria;

import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.onosproject.net.pi.runtime.PiValidFieldMatch;

import java.util.Map;

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

    PiHeaderFieldId piEthHeaderFieldId = PiHeaderFieldId.of("ethernet_t", "etherType");
    byte[] matchExactBytes1 = {0x08, 0x00};
    byte[] matchExactBytes2 = {0x08, 0x06};
    Criterion matchPiExactByte1 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactBytes1).build();
    Criterion sameAsMatchPiExactByte1 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactBytes1).build();
    Criterion matchPiExactByte2 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactBytes2).build();

    short matchExactShort1 = 0x800;
    short matchExactShort2 = 0x806;
    Criterion matchPiExactShort1 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactShort1).build();
    Criterion sameAsMatchPiExactShort1 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactShort1).build();
    Criterion matchPiExactShort2 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactShort2).build();

    int matchExactInt1 = 0x800;
    int matchExactInt2 = 0x806;
    Criterion matchPiExactInt1 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactInt1).build();
    Criterion sameAsMatchPiExactInt1 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactInt1).build();
    Criterion matchPiExactInt2 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactInt2).build();

    long matchExactLong1 = 0x800;
    long matchExactLong2 = 0x806;
    Criterion matchPiExactLong1 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactLong1).build();
    Criterion sameAsMatchPiExactLong1 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactLong1).build();
    Criterion matchPiExactLong2 = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactLong2).build();

    PiHeaderFieldId piIpv4HeaderFieldId = PiHeaderFieldId.of("ipv4_t", "dstAddr");
    int mask = 0x00ffffff;
    byte[] matchLpmBytes1 = {0x0a, 0x01, 0x01, 0x01};
    byte[] matchLpmBytes2 = {0x0a, 0x01, 0x01, 0x02};
    Criterion matchPiLpmByte1 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmBytes1, mask).build();
    Criterion sameAsMatchPiLpmByte1 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmBytes1, mask).build();
    Criterion matchPiLpmByte2 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmBytes2, mask).build();

    short matchLpmShort1 = 0x0a0a;
    short matchLpmShort2 = 0x0a0b;
    Criterion matchPiLpmShort1 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmShort1, mask).build();
    Criterion sameAsMatchPiLpmShort1 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId,
            matchLpmShort1, mask)
            .build();
    Criterion matchPiLpmShort2 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmShort2, mask).build();

    int matchLpmInt1 = 0x0a010101;
    int matchLpmInt2 = 0x0a010102;
    Criterion matchPiLpmInt1 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmInt1, mask).build();
    Criterion sameAsMatchPiLpmInt1 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmInt1, mask).build();
    Criterion matchPiLpmInt2 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmInt2, mask).build();

    long matchLpmLong1 = 0x0a010101;
    long matchLpmLong2 = 0x0a010102;
    Criterion matchPiLpmLong1 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmLong1, mask).build();
    Criterion sameAsMatchPiLpmLong1 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmLong1, mask).build();
    Criterion matchPiLpmLong2 = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmLong2, mask).build();


    byte[] matchTernaryBytes1 = {0x0a, 0x01, 0x01, 0x01};
    byte[] matchTernaryBytes2 = {0x0a, 0x01, 0x01, 0x02};
    byte[] matchTernaryMaskBytes = {0x7f, 0x7f, 0x7f, 0x00};
    Criterion matchPiTernaryByte1 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryBytes1,
            matchTernaryMaskBytes)
            .build();
    Criterion sameAsMatchPiTernaryByte1 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryBytes1,
            matchTernaryMaskBytes)
            .build();
    Criterion matchPiTernaryByte2 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryBytes2,
            matchTernaryMaskBytes)
            .build();

    short matchTernaryShort1 = 0x0a0a;
    short matchTernaryShort2 = 0x0a0b;
    short matchTernaryMaskShort = 0xff0;
    Criterion matchPiTernaryShort1 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryShort1,
            matchTernaryMaskShort)
            .build();
    Criterion sameAsMatchPiTernaryShort1 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryShort1,
            matchTernaryMaskShort)
            .build();
    Criterion matchPiTernaryShort2 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryShort2,
            matchTernaryMaskShort)
            .build();

    int matchTernaryInt1 = 0x0a010101;
    int matchTernaryInt2 = 0x0a010102;
    int matchTernaryMaskInt = 0xffff;
    Criterion matchPiTernaryInt1 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryInt1,
            matchTernaryMaskInt)
            .build();
    Criterion sameAsMatchPiTernaryInt1 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryInt1,
            matchTernaryMaskInt)
            .build();
    Criterion matchPiTernaryInt2 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryInt2,
            matchTernaryMaskInt)
            .build();

    long matchTernaryLong1 = 0x0a010101;
    long matchTernaryLong2 = 0x0a010102;
    long matchTernaryMaskLong = 0xffff;
    Criterion matchPiTernaryLong1 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryLong1,
            matchTernaryMaskLong)
            .build();
    Criterion sameAsMatchPiTernaryLong1 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryLong1,
            matchTernaryMaskLong)
            .build();
    Criterion matchPiTernaryLong2 = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId,
            matchTernaryLong2,
            matchTernaryMaskLong)
            .build();

    Criterion matchPiValid1 = PiCriterion.builder().matchValid(piIpv4HeaderFieldId, false).build();
    Criterion sameAsMatchPiValid1 = PiCriterion.builder().matchValid(piIpv4HeaderFieldId, false).build();
    Criterion matchPiValid2 = PiCriterion.builder().matchValid(piIpv4HeaderFieldId, true).build();

    byte[] matchRangeBytes1 = {0x10};
    byte[] matchRangeBytes2 = {0x20};
    byte[] matchRangeHighBytes = {0x30};
    Criterion matchPiRangeByte1 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeBytes1,
            matchRangeHighBytes)
            .build();
    Criterion sameAsMatchPiRangeByte1 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeBytes1,
            matchRangeHighBytes)
            .build();
    Criterion matchPiRangeByte2 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeBytes2,
            matchRangeHighBytes)
            .build();

    short matchRangeShort1 = 0x100;
    short matchRangeShort2 = 0x200;
    short matchRangeHighShort = 0x300;
    Criterion matchPiRangeShort1 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeShort1,
            matchRangeHighShort)
            .build();
    Criterion sameAsMatchPiRangeShort1 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeShort1,
            matchRangeHighShort)
            .build();
    Criterion matchPiRangeShort2 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeShort2,
            matchRangeHighShort)
            .build();

    int matchRangeInt1 = 0x100;
    int matchRangeInt2 = 0x200;
    int matchRangeHighInt = 0x300;
    Criterion matchPiRangeInt1 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeInt1,
            matchRangeHighInt)
            .build();
    Criterion sameAsMatchPiRangeInt1 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeInt1,
            matchRangeHighInt)
            .build();
    Criterion matchPiRangeInt2 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeInt2,
            matchRangeHighInt)
            .build();

    long matchRangeLong1 = 0x100;
    long matchRangeLong2 = 0x200;
    long matchRangeHighLong = 0x300;
    Criterion matchPiRangeLong1 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeLong1,
            matchRangeHighLong)
            .build();
    Criterion sameAsMatchPiRangeLong1 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeLong1,
            matchRangeHighLong)
            .build();
    Criterion matchPiRangeLong2 = PiCriterion.builder().matchRange(piIpv4HeaderFieldId,
            matchRangeLong2,
            matchRangeHighLong)
            .build();

    /**
     * Checks that a Criterion object has the proper type, and then converts
     * it to the proper type.
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

        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesExactBytes = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesExactShort = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesExactInt = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesExactLong = Maps.newHashMap();

        Criterion matchPiBytes = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactBytes1).build();
        PiCriterion piCriterionBytes =
                checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesExactBytes.put(piEthHeaderFieldId, new PiExactFieldMatch(piEthHeaderFieldId,
                copyFrom(matchExactBytes1)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionBytes.fieldMatches(), fieldMatchesExactBytes.values()));
        assertThat(piCriterionBytes.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));


        Criterion matchPiShort = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactShort1).build();
        PiCriterion piCriterionShort =
                checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesExactShort.put(piEthHeaderFieldId, new PiExactFieldMatch(piEthHeaderFieldId,
                copyFrom(matchExactShort1)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionShort.fieldMatches(), fieldMatchesExactShort.values()));
        assertThat(piCriterionShort.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));


        Criterion matchPiInt = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactInt1).build();
        PiCriterion piCriterionInt =
                checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesExactInt.put(piEthHeaderFieldId, new PiExactFieldMatch(piEthHeaderFieldId,
                copyFrom(matchExactInt1)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionInt.fieldMatches(), fieldMatchesExactInt.values()));
        assertThat(piCriterionInt.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));


        Criterion matchPiLong = PiCriterion.builder().matchExact(piEthHeaderFieldId, matchExactLong1).build();
        PiCriterion piCriterionLong =
                checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesExactLong.put(piEthHeaderFieldId, new PiExactFieldMatch(piEthHeaderFieldId,
                copyFrom(matchExactLong1)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionLong.fieldMatches(), fieldMatchesExactLong.values()));
        assertThat(piCriterionLong.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));
    }

    /**
     * Test the LpmMatchPi method.
     */
    @Test
    public void testLpmMatchPiMethod() {

        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesLpmBytes = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesLpmShort = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesLpmInt = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesLpmLong = Maps.newHashMap();

        Criterion matchPiBytes = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmBytes1, mask).build();
        PiCriterion piCriterionBytes =
                checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesLpmBytes.put(piIpv4HeaderFieldId, new PiLpmFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchLpmBytes1), mask));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionBytes.fieldMatches(), fieldMatchesLpmBytes.values()));
        assertThat(piCriterionBytes.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));


        Criterion matchPiShort = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmShort1, mask).build();
        PiCriterion piCriterionShort =
                checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesLpmShort.put(piIpv4HeaderFieldId, new PiLpmFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchLpmShort1), mask));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionShort.fieldMatches(), fieldMatchesLpmShort.values()));
        assertThat(piCriterionShort.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));


        Criterion matchPiInt = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmInt1, mask).build();
        PiCriterion piCriterionInt =
                checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesLpmInt.put(piIpv4HeaderFieldId, new PiLpmFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchLpmInt1), mask));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionInt.fieldMatches(), fieldMatchesLpmInt.values()));
        assertThat(piCriterionInt.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));


        Criterion matchPiLong = PiCriterion.builder().matchLpm(piIpv4HeaderFieldId, matchLpmLong1, mask).build();
        PiCriterion piCriterionLong =
                checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesLpmLong.put(piIpv4HeaderFieldId, new PiLpmFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchLpmLong1), mask));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionLong.fieldMatches(), fieldMatchesLpmLong.values()));
        assertThat(piCriterionLong.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));
    }

    /**
     * Test the TernaryMatchPi method.
     */
    @Test
    public void testTernaryMatchPiMethod() {
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesTernaryBytes = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesTernaryShort = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesTernaryInt = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesTernaryLong = Maps.newHashMap();

        Criterion matchPiBytes = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId, matchTernaryBytes1,
                matchTernaryMaskBytes)
                .build();
        PiCriterion piCriterionBytes =
                checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesTernaryBytes.put(piIpv4HeaderFieldId, new PiTernaryFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchTernaryBytes1),
                copyFrom(matchTernaryMaskBytes)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionBytes.fieldMatches(), fieldMatchesTernaryBytes.values()));
        assertThat(piCriterionBytes.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));

        Criterion matchPiShort = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId, matchTernaryShort1,
                matchTernaryMaskShort)
                .build();
        PiCriterion piCriterionShort =
                checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesTernaryShort.put(piIpv4HeaderFieldId, new PiTernaryFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchTernaryShort1),
                copyFrom(matchTernaryMaskShort)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionShort.fieldMatches(), fieldMatchesTernaryShort.values()));
        assertThat(piCriterionShort.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));

        Criterion matchPiInt = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId, matchTernaryInt1,
                matchTernaryMaskInt)
                .build();
        PiCriterion piCriterionInt =
                checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesTernaryInt.put(piIpv4HeaderFieldId, new PiTernaryFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchTernaryInt1),
                copyFrom(matchTernaryMaskInt)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionInt.fieldMatches(), fieldMatchesTernaryInt.values()));
        assertThat(piCriterionInt.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));

        Criterion matchPiLong = PiCriterion.builder().matchTernary(piIpv4HeaderFieldId, matchTernaryLong1,
                matchTernaryMaskLong)
                .build();
        PiCriterion piCriterionLong =
                checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesTernaryLong.put(piIpv4HeaderFieldId, new PiTernaryFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchTernaryLong1),
                copyFrom(matchTernaryMaskLong)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionLong.fieldMatches(), fieldMatchesTernaryLong.values()));
        assertThat(piCriterionLong.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));
    }

    /**
     * Test the ValidMatchPi method.
     */
    @Test
    public void testValidMatchPiMethod() {
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesValid = Maps.newHashMap();
        Criterion matchPiBytes = PiCriterion.builder().matchValid(piIpv4HeaderFieldId, true).build();
        PiCriterion piCriterionBytes = checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT,
                PiCriterion.class);
        fieldMatchesValid.put(piIpv4HeaderFieldId, new PiValidFieldMatch(piIpv4HeaderFieldId, true));

        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionBytes.fieldMatches(), fieldMatchesValid.values()));
        assertThat(piCriterionBytes.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));
    }

    /**
     * Test the RangeMatchPi method.
     */
    @Test
    public void testRangeMatchPiMethod() {
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesRangeBytes = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesRangeShort = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesRangeInt = Maps.newHashMap();
        Map<PiHeaderFieldId, PiFieldMatch> fieldMatchesRangeLong = Maps.newHashMap();

        Criterion matchPiBytes = PiCriterion.builder().matchRange(piIpv4HeaderFieldId, matchRangeBytes1,
                matchRangeHighBytes)
                .build();
        PiCriterion piCriterionBytes =
                checkAndConvert(matchPiBytes, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesRangeBytes.put(piIpv4HeaderFieldId, new PiRangeFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchRangeBytes1),
                copyFrom(matchRangeHighBytes)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionBytes.fieldMatches(), fieldMatchesRangeBytes.values()));
        assertThat(piCriterionBytes.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));

        Criterion matchPiShort = PiCriterion.builder().matchRange(piIpv4HeaderFieldId, matchRangeShort1,
                matchRangeHighShort)
                .build();
        PiCriterion piCriterionShort =
                checkAndConvert(matchPiShort, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesRangeShort.put(piIpv4HeaderFieldId, new PiRangeFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchRangeShort1),
                copyFrom(matchRangeHighShort)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionShort.fieldMatches(), fieldMatchesRangeShort.values()));
        assertThat(piCriterionShort.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));

        Criterion matchPiInt = PiCriterion.builder().matchRange(piIpv4HeaderFieldId, matchRangeInt1,
                matchRangeHighInt)
                .build();
        PiCriterion piCriterionInt =
                checkAndConvert(matchPiInt, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesRangeInt.put(piIpv4HeaderFieldId, new PiRangeFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchRangeInt1),
                copyFrom(matchRangeHighInt)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionInt.fieldMatches(), fieldMatchesRangeInt.values()));
        assertThat(piCriterionInt.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));

        Criterion matchPiLong = PiCriterion.builder().matchRange(piIpv4HeaderFieldId, matchRangeLong1,
                matchRangeHighLong)
                .build();
        PiCriterion piCriterionLong =
                checkAndConvert(matchPiLong, Criterion.Type.PROTOCOL_INDEPENDENT, PiCriterion.class);
        fieldMatchesRangeLong.put(piIpv4HeaderFieldId, new PiRangeFieldMatch(piIpv4HeaderFieldId,
                copyFrom(matchRangeLong1),
                copyFrom(matchRangeHighLong)));
        assertThat("Incorrect match param value",
                CollectionUtils.isEqualCollection(piCriterionLong.fieldMatches(), fieldMatchesRangeLong.values()));
        assertThat(piCriterionLong.type(), is(equalTo(Criterion.Type.PROTOCOL_INDEPENDENT)));
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
                .addEqualityGroup(matchPiExactShort1)
                .addEqualityGroup(matchPiExactShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiExactInt1)
                .addEqualityGroup(matchPiExactInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiExactLong1)
                .addEqualityGroup(matchPiExactLong2)
                .testEquals();
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiLpmCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiLpmByte1)
                .addEqualityGroup(matchPiLpmByte2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiLpmShort1)
                .addEqualityGroup(matchPiLpmShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiLpmInt1)
                .addEqualityGroup(matchPiLpmInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiLpmLong1)
                .addEqualityGroup(matchPiLpmLong2)
                .testEquals();
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiTernaryCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiTernaryByte1)
                .addEqualityGroup(matchPiTernaryByte2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiTernaryShort1)
                .addEqualityGroup(matchPiTernaryShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiTernaryInt1)
                .addEqualityGroup(matchPiTernaryInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiTernaryLong1)
                .addEqualityGroup(matchPiTernaryLong2)
                .testEquals();
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiValidCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiValid1)
                .addEqualityGroup(matchPiValid2)
                .testEquals();
    }

    /**
     * Test the equals() method of the PiCriterion class.
     */
    @Test
    public void testPiRangeCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchPiRangeByte1)
                .addEqualityGroup(matchPiRangeByte2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiRangeShort1)
                .addEqualityGroup(matchPiRangeShort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiRangeInt1)
                .addEqualityGroup(matchPiRangeInt2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchPiRangeLong1)
                .addEqualityGroup(matchPiRangeLong2)
                .testEquals();
    }
}
