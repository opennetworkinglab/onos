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
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;

/**
 * Suite of tests of the point-to-point intent descriptor.
 */
public class PointToPointIntentTest extends ConnectivityIntentTest {

    /**
     * Checks that the PointToPointIntent class is immutable.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutableBaseClass(PointToPointIntent.class);
    }

    @Test
    public void basics() {
        PointToPointIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", P2, intent.egressPoint());

        intent = createWithResourceGroup();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", P2, intent.egressPoint());
        assertEquals("incorrect resource group", RESOURCE_GROUP, intent.resourceGroup());
    }

    @Test
    public void filtered() {
        PointToPointIntent intent = createOneFiltered();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", FP1, intent.filteredIngressPoint());
        assertEquals("incorrect egress", FP2, intent.filteredEgressPoint());
    }

    @Override
    protected PointToPointIntent createOne() {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P1)
                .egressPoint(P2)
                .build();
    }

    protected PointToPointIntent createWithResourceGroup() {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P1)
                .egressPoint(P2)
                .resourceGroup(RESOURCE_GROUP)
                .build();
    }

    @Override
    protected PointToPointIntent createAnother() {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P2)
                .egressPoint(P1)
                .build();
    }

    protected PointToPointIntent createOneFiltered() {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .filteredIngressPoint(FP1)
                .filteredEgressPoint(FP2)
                .build();
    }
}
