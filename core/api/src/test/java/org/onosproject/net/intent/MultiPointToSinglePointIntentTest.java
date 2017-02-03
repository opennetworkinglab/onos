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

import org.junit.Test;

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

    /**
     * Create three intents with normal connect points.
     */
    @Test
    public void basics() {
        MultiPointToSinglePointIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", PS1, intent.ingressPoints());
        assertEquals("incorrect egress", P2, intent.egressPoint());

        intent = createAnother();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", PS2, intent.ingressPoints());
        assertEquals("incorrect egress", P1, intent.egressPoint());

        intent = createVlanMatch();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", VLANMATCH1, intent.selector());
        assertEquals("incorrect ingress", PS1, intent.ingressPoints());
        assertEquals("incorrect egress", P2, intent.egressPoint());

        intent = createWithResourceGroup();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", PS1, intent.ingressPoints());
        assertEquals("incorrect egress", P2, intent.egressPoint());
        assertEquals("incorrect resource group", RESOURCE_GROUP, intent.resourceGroup());

    }

    /**
     * Create two intents with filtered connect points.
     */
    @Test
    public void filteredIntent() {
        MultiPointToSinglePointIntent intent = createFilteredOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect filtered ingress", FPS1, intent.filteredIngressPoints());
        assertEquals("incorrect filtered egress", FP2, intent.filteredEgressPoint());

        intent = createAnotherFiltered();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect filtered ingress", FPS2, intent.filteredIngressPoints());
        assertEquals("incorrect filtered egress", FP1, intent.filteredEgressPoint());

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

    protected MultiPointToSinglePointIntent createVlanMatch() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(VLANMATCH1)
                .treatment(NOP)
                .ingressPoints(PS1)
                .egressPoint(P2)
                .build();
    }

    protected MultiPointToSinglePointIntent createWithResourceGroup() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoints(PS1)
                .egressPoint(P2)
                .resourceGroup(RESOURCE_GROUP)
                .build();
    }


    protected MultiPointToSinglePointIntent createFilteredOne() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .treatment(NOP)
                .filteredIngressPoints(FPS1)
                .filteredEgressPoint(FP2)
                .build();
    }

    protected MultiPointToSinglePointIntent createAnotherFiltered() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .treatment(NOP)
                .filteredIngressPoints(FPS2)
                .filteredEgressPoint(FP1)
                .build();
    }

    protected MultiPointToSinglePointIntent createWrongIntent() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(VLANMATCH1)
                .treatment(NOP)
                .filteredIngressPoints(FPS1)
                .filteredEgressPoint(FP2)
                .build();
    }

}
