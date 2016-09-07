/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.intent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Suite of tests of the single-to-multi point intent descriptor.
 */
public class SinglePointToMultiPointIntentTest extends ConnectivityIntentTest {

    /**
     * Checks that the SinglePointToMultiPointIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(SinglePointToMultiPointIntent.class);
    }

    @Test
    public void basics() {
        SinglePointToMultiPointIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", PS2, intent.egressPoints());
    }

    @Rule
    public ExpectedException wrongMultiple = ExpectedException.none();

    @Test
    public void multipleTreatments() {

        SinglePointToMultiPointIntent intent = createFirstMultiple();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", PS2, intent.egressPoints());
        assertEquals("incorrect treatment", NOP, intent.treatment());
        assertEquals("incorrect treatments", TREATMENTS, intent.egressTreatments());

        intent = createSecondMultiple();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", PS2, intent.egressPoints());
        assertEquals("incorrect treatment", VLANACTION1, intent.treatment());
        assertEquals("incorrect selectors", TREATMENTS, intent.egressTreatments());

        intent = createThirdMultiple();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", PS2, intent.egressPoints());
        assertEquals("incorrect treatment", NOP, intent.treatment());
        assertEquals("incorrect selectors", VLANACTIONS, intent.egressTreatments());

        wrongMultiple.expect(IllegalArgumentException.class);
        wrongMultiple.expectMessage("Treatment and Multiple Treatments are both set");
        intent = createWrongMultiple();
    }

    @Override
    protected SinglePointToMultiPointIntent createOne() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P1)
                .egressPoints(PS2)
                .build();
    }

    @Override
    protected SinglePointToMultiPointIntent createAnother() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P2)
                .egressPoints(PS1)
                .build();
    }


    protected SinglePointToMultiPointIntent createFirstMultiple() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P1)
                .egressPoints(PS2)
                .treatments(TREATMENTS)
                .build();
    }

    protected SinglePointToMultiPointIntent createSecondMultiple() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(VLANACTION1)
                .ingressPoint(P1)
                .egressPoints(PS2)
                .treatments(TREATMENTS)
                .build();
    }

    protected SinglePointToMultiPointIntent createThirdMultiple() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P1)
                .egressPoints(PS2)
                .treatments(VLANACTIONS)
                .build();
    }

    protected SinglePointToMultiPointIntent createWrongMultiple() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(VLANACTION1)
                .ingressPoint(P1)
                .egressPoints(PS2)
                .treatments(VLANACTIONS)
                .build();
    }

}
