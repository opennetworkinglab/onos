/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.intent.LinksHaveEntryWithSourceDestinationPairMatcher.linksHasPath;

/**
 * Unit tests for the MultiPointToSinglePoint intent compiler.
 */
public class MultiPointToSinglePointIntentCompilerTest extends AbstractIntentTest {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    /**
     * Creates a MultiPointToSinglePoint intent for a group of ingress points
     * and an egress point.
     *
     * @param ingressIds array of ingress device ids
     * @param egressId   device id of the egress point
     * @return MultiPointToSinglePoint intent
     */
    private MultiPointToSinglePointIntent makeIntent(String[] ingressIds, String egressId) {
        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ConnectPoint egressPoint = connectPoint(egressId, 2);

        for (String ingressId : ingressIds) {
            ingressPoints.add(connectPoint(ingressId, 1));
        }

        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(selector)
                .treatment(treatment)
                .ingressPoints(ingressPoints)
                .egressPoint(egressPoint)
                .build();
    }

    /**
     * Generate MultiPointToSinglePointIntent with filtered connection point.
     *
     * @param ingress filtered ingress points
     * @param egress filtered egress point
     * @return
     */
    private MultiPointToSinglePointIntent makeFilteredConnectPointIntent(Set<FilteredConnectPoint> ingress,
                                                                         FilteredConnectPoint egress,
                                                                         TrafficSelector trafficSelector) {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .treatment(treatment)
                .selector(trafficSelector)
                .filteredIngressPoints(ingress)
                .filteredEgressPoint(egress)
                .build();
    }

    /**
     * Creates a compiler for MultiPointToSinglePoint intents.
     *
     * @param hops hops to use while computing paths for this intent
     * @return MultiPointToSinglePoint intent
     */
    private MultiPointToSinglePointIntentCompiler makeCompiler(String[] hops) {
        MultiPointToSinglePointIntentCompiler compiler =
                new MultiPointToSinglePointIntentCompiler();
        compiler.pathService = new IntentTestsMocks.Mp2MpMockPathService(hops);
        compiler.deviceService = new IntentTestsMocks.MockDeviceService();
        return compiler;
    }

    /**
     * Tests a single ingress point with 8 hops to its egress point.
     */
    @Test
    public void testSingleLongPathCompilation() {

        String[] ingress = {"ingress"};
        String egress = "egress";

        MultiPointToSinglePointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        String[] hops = {"h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8"};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent instanceof LinkCollectionIntent, is(true));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(9));
            assertThat(linkIntent.links(), linksHasPath("ingress", "h1"));
            assertThat(linkIntent.links(), linksHasPath("h1", "h2"));
            assertThat(linkIntent.links(), linksHasPath("h2", "h3"));
            assertThat(linkIntent.links(), linksHasPath("h4", "h5"));
            assertThat(linkIntent.links(), linksHasPath("h5", "h6"));
            assertThat(linkIntent.links(), linksHasPath("h7", "h8"));
            assertThat(linkIntent.links(), linksHasPath("h8", "egress"));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));
    }

    /**
     * Tests a simple topology where two ingress points share some path segments
     * and some path segments are not shared.
     */
    @Test
    public void testTwoIngressCompilation() {
        String[] ingress = {"ingress1", "ingress2"};
        String egress = "egress";

        MultiPointToSinglePointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {"inner1", "inner2"};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent instanceof LinkCollectionIntent, is(true));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(4));
            assertThat(linkIntent.links(), linksHasPath("ingress1", "inner1"));
            assertThat(linkIntent.links(), linksHasPath("ingress2", "inner1"));
            assertThat(linkIntent.links(), linksHasPath("inner1", "inner2"));
            assertThat(linkIntent.links(), linksHasPath("inner2", "egress"));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));
    }

    /**
     * Tests a large number of ingress points that share a common path to the
     * egress point.
     */
    @Test
    public void testMultiIngressCompilation() {
        String[] ingress = {"i1", "i2", "i3", "i4", "i5",
                "i6", "i7", "i8", "i9", "i10"};
        String egress = "e";

        MultiPointToSinglePointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {"n1"};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent instanceof LinkCollectionIntent, is(true));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(ingress.length + 1));
            for (String ingressToCheck : ingress) {
                assertThat(linkIntent.links(),
                           linksHasPath(ingressToCheck,
                                        "n1"));
            }
            assertThat(linkIntent.links(), linksHasPath("n1", egress));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));
    }

    /**
     * Tests ingress and egress on the same device.
     */
    @Test
    public void testSameDeviceCompilation() {
        String[] ingress = {"i1", "i2"};
        String egress = "i3";

        MultiPointToSinglePointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(LinkCollectionIntent.class));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(ingress.length));

            assertThat(linkIntent.links(), linksHasPath("i1", "i3"));
            assertThat(linkIntent.links(), linksHasPath("i2", "i3"));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));
    }

    /**
     * Tests filtered ingress and egress.
     */
    @Test
    public void testFilteredConnectPointIntent() {

        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(connectPoint("of1", 1),
                                         DefaultTrafficSelector.builder().matchVlanId(VlanId.vlanId("100")).build()),
                new FilteredConnectPoint(connectPoint("of2", 1),
                                         DefaultTrafficSelector.builder().matchVlanId(VlanId.vlanId("200")).build())
        );

        FilteredConnectPoint egress = new FilteredConnectPoint(connectPoint("of4", 1));

        MultiPointToSinglePointIntent intent = makeFilteredConnectPointIntent(ingress, egress, selector);
        String[] hops = {"of3"};

        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));

        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(LinkCollectionIntent.class));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(3));
            assertThat(linkIntent.links(), linksHasPath("of1", "of3"));
            assertThat(linkIntent.links(), linksHasPath("of2", "of3"));
            assertThat(linkIntent.links(), linksHasPath("of3", "of4"));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));

    }

    /**
     * Tests selector, filtered ingress and egress.
     */
    @Test
    public void testNonTrivialSelectorsIntent() {

        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(connectPoint("of1", 1),
                                         DefaultTrafficSelector.builder().matchVlanId(VlanId.vlanId("100")).build()),
                new FilteredConnectPoint(connectPoint("of2", 1),
                                         DefaultTrafficSelector.builder().matchVlanId(VlanId.vlanId("200")).build())
        );

        TrafficSelector ipPrefixSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.100.0/24"))
                .build();

        FilteredConnectPoint egress = new FilteredConnectPoint(connectPoint("of4", 1));

        MultiPointToSinglePointIntent intent = makeFilteredConnectPointIntent(ingress, egress, ipPrefixSelector);
        String[] hops = {"of3"};

        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));

        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(LinkCollectionIntent.class));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(3));
            assertThat(linkIntent.links(), linksHasPath("of1", "of3"));
            assertThat(linkIntent.links(), linksHasPath("of2", "of3"));
            assertThat(linkIntent.links(), linksHasPath("of3", "of4"));
            assertThat(linkIntent.selector(), is(ipPrefixSelector));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));

    }


}
