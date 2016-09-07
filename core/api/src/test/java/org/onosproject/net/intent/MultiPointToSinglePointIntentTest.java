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
 * Suite of tests of the multi-to-single point intent descriptor.
 */
public class MultiPointToSinglePointIntentTest extends ConnectivityIntentTest {

    /**
     * Checks that the MultiPointToSinglePointIntent class is immutable.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutable(MultiPointToSinglePointIntent.class);
    }

    @Test
    public void basics() {
        MultiPointToSinglePointIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", PS1, intent.ingressPoints());
        assertEquals("incorrect egress", P2, intent.egressPoint());
    }

    @Rule
    public ExpectedException wrongMultiple = ExpectedException.none();

    @Test
    public void multipleSelectors() {

        MultiPointToSinglePointIntent intent = createFirstMultiple();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", PS1, intent.ingressPoints());
        assertEquals("incorrect egress", P2, intent.egressPoint());
        assertEquals("incorrect selectors", MATCHES, intent.ingressSelectors());

        intent = createSecondMultiple();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", VLANMATCH1, intent.selector());
        assertEquals("incorrect ingress", PS1, intent.ingressPoints());
        assertEquals("incorrect egress", P2, intent.egressPoint());
        assertEquals("incorrect selectors", MATCHES, intent.ingressSelectors());

        intent = createThirdMultiple();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", PS1, intent.ingressPoints());
        assertEquals("incorrect egress", P2, intent.egressPoint());
        assertEquals("incorrect selectors", VLANMATCHES, intent.ingressSelectors());

        wrongMultiple.expect(IllegalArgumentException.class);
        wrongMultiple.expectMessage("Selector and Multiple Selectors are both set");
        intent = createWrongMultiple();
    }


    @Override
    protected MultiPointToSinglePointIntent createOne() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoints(PS1)
                .egressPoint(P2)
                .build();
    }

    @Override
    protected MultiPointToSinglePointIntent createAnother() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoints(PS2)
                .egressPoint(P1)
                .build();
    }

    protected MultiPointToSinglePointIntent createFirstMultiple() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoints(PS1)
                .egressPoint(P2)
                .selectors(MATCHES)
                .build();
    }

    protected MultiPointToSinglePointIntent createSecondMultiple() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(VLANMATCH1)
                .treatment(NOP)
                .ingressPoints(PS1)
                .egressPoint(P2)
                .selectors(MATCHES)
                .build();
    }

    protected MultiPointToSinglePointIntent createThirdMultiple() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoints(PS1)
                .egressPoint(P2)
                .selectors(VLANMATCHES)
                .build();
    }

    protected MultiPointToSinglePointIntent createWrongMultiple() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(VLANMATCH1)
                .treatment(NOP)
                .ingressPoints(PS1)
                .egressPoint(P2)
                .selectors(VLANMATCHES)
                .build();
    }

}
