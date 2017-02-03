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

        intent = createAnother();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P2, intent.ingressPoint());
        assertEquals("incorrect egress", PS1, intent.egressPoints());

        intent = createWithResourceGroup();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P2, intent.ingressPoint());
        assertEquals("incorrect egress", PS1, intent.egressPoints());
        assertEquals("incorrect resource group", RESOURCE_GROUP, intent.resourceGroup());
    }

    @Test
    public void filteredIntent() {
        SinglePointToMultiPointIntent intent = createFilteredOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect filtered ingress", FP2, intent.filteredIngressPoint());
        assertEquals("incorrect filtered egress", FPS1, intent.filteredEgressPoints());

        intent = createAnotherFiltered();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect filtered ingress", FP1, intent.filteredIngressPoint());
        assertEquals("incorrect filtered egress", FPS2, intent.filteredEgressPoints());
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

    protected SinglePointToMultiPointIntent createWithResourceGroup() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P2)
                .egressPoints(PS1)
                .resourceGroup(RESOURCE_GROUP)
                .build();
    }

    protected SinglePointToMultiPointIntent createFilteredOne() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .treatment(NOP)
                .filteredEgressPoints(FPS1)
                .filteredIngressPoint(FP2)
                .build();
    }

    protected SinglePointToMultiPointIntent createAnotherFiltered() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .treatment(NOP)
                .filteredEgressPoints(FPS2)
                .filteredIngressPoint(FP1)
                .build();
    }

    protected SinglePointToMultiPointIntent createWrongIntent() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .treatment(NOP)
                .selector(VLANMATCH1)
                .filteredEgressPoints(FPS2)
                .filteredIngressPoint(FP1)
                .build();
    }

}
