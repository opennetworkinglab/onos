/*
 * Copyright 2014-present Open Networking Foundation
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
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
        assertEquals("incorrect ingress", P1, intent.filteredIngressPoint().connectPoint());
        assertEquals("incorrect egress", P2, intent.filteredEgressPoint().connectPoint());

        intent = createWithResourceGroup();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.filteredIngressPoint().connectPoint());
        assertEquals("incorrect egress", P2, intent.filteredEgressPoint().connectPoint());
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

    @Test
    public void suggestedPath() {
        List<Link> suggestedPath = new LinkedList<>();
        suggestedPath.add(new IntentTestsMocks.FakeLink(FP1.connectPoint(), FP2.connectPoint()));

        PointToPointIntent intent = createWithSuggestedPath(suggestedPath);
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", FP1, intent.filteredIngressPoint());
        assertEquals("incorrect egress", FP2, intent.filteredEgressPoint());
        assertEquals("incorrect suggested path", suggestedPath, intent.suggestedPath());

    }

    @Test
    public void failSuggestedPath() {
        List<Link> suggestedPath = new LinkedList<>();
        try {
            suggestedPath.add(new IntentTestsMocks.FakeLink(FP3.connectPoint(), FP2.connectPoint()));

            createWithSuggestedPath(suggestedPath);
            fail("Point to Point intent building with incompatible suggested path "
                         + "not throw exception.");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage(), containsString("Suggested path not compatible"));
        }
    }

    @Override
    protected PointToPointIntent createOne() {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .filteredIngressPoint(new FilteredConnectPoint(P1))
                .filteredEgressPoint(new FilteredConnectPoint(P2))
                .build();
    }

    protected PointToPointIntent createWithResourceGroup() {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .filteredIngressPoint(new FilteredConnectPoint(P1))
                .filteredEgressPoint(new FilteredConnectPoint(P2))
                .resourceGroup(RESOURCE_GROUP)
                .build();
    }

    @Override
    protected PointToPointIntent createAnother() {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .filteredIngressPoint(new FilteredConnectPoint(P2))
                .filteredEgressPoint(new FilteredConnectPoint(P1))
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

    protected PointToPointIntent createWithSuggestedPath(List<Link> suggestedPath) {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .filteredIngressPoint(FP1)
                .filteredEgressPoint(FP2)
                .suggestedPath(suggestedPath)
                .build();
    }
}
