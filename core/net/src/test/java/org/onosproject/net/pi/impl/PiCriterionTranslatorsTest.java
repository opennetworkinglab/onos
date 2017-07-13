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

package org.onosproject.net.pi.impl;

import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.net.pi.impl.CriterionTranslatorHelper.translateCriterion;
import static org.onosproject.net.pi.model.PiMatchType.*;

/**
 * Tests for CriterionTranslators.
 */
public class PiCriterionTranslatorsTest {

    private Random random = new Random();
    private final PiHeaderFieldId fieldId = PiHeaderFieldId.of("foo", "bar");

    @Test
    public void testEthCriterion() throws Exception {
        MacAddress value1 = MacAddress.valueOf(random.nextLong());
        MacAddress value2 = MacAddress.valueOf(random.nextLong());
        MacAddress mask = MacAddress.valueOf(random.nextLong());
        int bitWidth = value1.toBytes().length * 8;

        EthCriterion criterion = (EthCriterion) Criteria.matchEthDst(value1);
        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        EthCriterion maskedCriterion = (EthCriterion) Criteria.matchEthDstMasked(value2, mask);
        PiTernaryFieldMatch ternaryMatch = (PiTernaryFieldMatch) translateCriterion(maskedCriterion, fieldId, TERNARY,
                                                                                    bitWidth);

        assertThat(exactMatch.value().asArray(), is(criterion.mac().toBytes()));
        assertThat(ternaryMatch.value().asArray(), is(maskedCriterion.mac().toBytes()));
        assertThat(ternaryMatch.mask().asArray(), is(maskedCriterion.mask().toBytes()));
    }

    @Test
    public void testEthTypeCriterion() throws Exception {
        EthType ethType = new EthType(random.nextInt());
        int bitWidth = 16;

        EthTypeCriterion criterion = (EthTypeCriterion) Criteria.matchEthType(ethType);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is(criterion.ethType().toShort()));
    }

    @Test
    public void testIpCriterion() throws Exception {
        IpPrefix prefix1 = IpPrefix.valueOf(random.nextInt(), random.nextInt(32));
        int bitWidth = prefix1.address().toOctets().length * 8;

        IPCriterion criterion = (IPCriterion) Criteria.matchIPDst(prefix1);

        PiLpmFieldMatch lpmMatch = (PiLpmFieldMatch) translateCriterion(criterion, fieldId, LPM, bitWidth);

        assertThat(lpmMatch.value().asArray(), is(criterion.ip().address().toOctets()));
        assertThat(lpmMatch.prefixLength(), is(criterion.ip().prefixLength()));
    }


}
