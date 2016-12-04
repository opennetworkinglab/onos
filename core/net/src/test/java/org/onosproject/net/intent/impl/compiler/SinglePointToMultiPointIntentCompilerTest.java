/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.net.intent.SinglePointToMultiPointIntent;

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
 * Unit tests for SinglePointToMultiPointIntentCompiler.
 */
public class SinglePointToMultiPointIntentCompilerTest extends AbstractIntentTest {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();



    /**
     * Creates a SinglePointToMultiPoint intent for an ingress point
     * and a group of egress points.
     *
     * @param ingressId device id of the ingress point
     * @param egressIds array of egress device ids
     * @return MultiPointToSinglePoint intent
     */
    private SinglePointToMultiPointIntent makeIntent(String ingressId, String[] egressIds) {
        ConnectPoint ingressPoint = connectPoint(ingressId, 2);
        Set<ConnectPoint> egressPoints = new HashSet<>();


        for (String egressId : egressIds) {
            egressPoints.add(connectPoint(egressId, 1));
        }

        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(ingressPoint)
                .egressPoints(egressPoints)
                .build();
    }

    /**
     * Generate SinglePointToMultiPointIntent with filtered connection point.
     *
     * @param ingress filtered ingress point
     * @param egress filtered egress point
     * @return
     */
    private SinglePointToMultiPointIntent makeFilteredConnectPointIntent(FilteredConnectPoint ingress,
                                                                         Set<FilteredConnectPoint> egress,
                                                                         TrafficSelector trafficSelector) {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .treatment(treatment)
                .selector(trafficSelector)
                .filteredIngressPoint(ingress)
                .filteredEgressPoints(egress)
                .build();
    }

    /**
     * Creates a compiler for SinglePointToMultiPoint intents.
     *
     * @param hops hops to use while computing paths for this intent
     * @return SinglePointToMultiPoint intent
     */
    private SinglePointToMultiPointIntentCompiler makeCompiler(String[] hops) {
        SinglePointToMultiPointIntentCompiler compiler =
                new SinglePointToMultiPointIntentCompiler();

        compiler.pathService = new IntentTestsMocks.Mp2MpMockPathService(hops);
        return compiler;
    }

    /**
     * Tests a single ingress point with 8 hops to its egress point.
     */
    @Test
    public void testSingleLongPathCompilation() {

        String ingress = "ingress";
        String[] egress = {"egress"};

        SinglePointToMultiPointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        String[] hops = {"h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8"};
        SinglePointToMultiPointIntentCompiler compiler = makeCompiler(hops);
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
     * Tests a simple topology where two egress points share some path segments
     * and some path segments are not shared.
     */
    @Test
    public void testTwoIngressCompilation() {
        String ingress = "ingress";
        String[] egress = {"egress1", "egress2"};

        SinglePointToMultiPointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {"inner1", "inner2"};
        SinglePointToMultiPointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent instanceof LinkCollectionIntent, is(true));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(4));
            assertThat(linkIntent.links(), linksHasPath("ingress", "inner1"));
            assertThat(linkIntent.links(), linksHasPath("inner1", "inner2"));
            assertThat(linkIntent.links(), linksHasPath("inner2", "egress1"));
            assertThat(linkIntent.links(), linksHasPath("inner2", "egress2"));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));
    }

    /**
     * Tests a large number of ingress points that share a common path to the
     * egress point.
     */
    @Test
    public void testMultiIngressCompilation() {
        String ingress = "i";
        String[] egress = {"e1", "e2", "e3", "e4", "e5",
                "e6", "e7", "e8", "e9", "e10"};

        SinglePointToMultiPointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {"n1"};
        SinglePointToMultiPointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent instanceof LinkCollectionIntent, is(true));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(egress.length + 1));
            for (String egressToCheck : egress) {
                assertThat(linkIntent.links(), linksHasPath("n1", egressToCheck));
            }
            assertThat(linkIntent.links(), linksHasPath(ingress, "n1"));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));
    }

    /**
     * Tests ingress and egress on the same device.
     */
    @Test
    public void testSameDeviceCompilation() {
        String ingress = "i1";
        String[] egress = {"i2", "i3"};

        SinglePointToMultiPointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {};
        SinglePointToMultiPointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(LinkCollectionIntent.class));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(egress.length));

            assertThat(linkIntent.links(), linksHasPath("i1", "i2"));
            assertThat(linkIntent.links(), linksHasPath("i1", "i3"));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));
    }

    /**
     * Tests filtered ingress and egress.
     */
    @Test
    public void testFilteredConnectPointIntent() {

        FilteredConnectPoint ingress = new FilteredConnectPoint(connectPoint("of1", 1));

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(connectPoint("of3", 1),
                                         DefaultTrafficSelector.builder().matchVlanId(VlanId.vlanId("100")).build()),
                new FilteredConnectPoint(connectPoint("of4", 1),
                                         DefaultTrafficSelector.builder().matchVlanId(VlanId.vlanId("200")).build())
        );


        SinglePointToMultiPointIntent intent = makeFilteredConnectPointIntent(ingress, egress, selector);
        String[] hops = {"of2"};

        SinglePointToMultiPointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));

        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(LinkCollectionIntent.class));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(3));

            assertThat(linkIntent.links(), linksHasPath("of1", "of2"));
            assertThat(linkIntent.links(), linksHasPath("of2", "of3"));
            assertThat(linkIntent.links(), linksHasPath("of2", "of4"));

            Set<FilteredConnectPoint> ingressPoints = linkIntent.filteredIngressPoints();
            assertThat("Link collection ingress points do not match base intent",
                       ingressPoints.size() == 1 && ingressPoints.contains(intent.filteredIngressPoint()));

            assertThat("Link collection egress points do not match base intent",
                       linkIntent.filteredEgressPoints().equals(intent.filteredEgressPoints()));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));

    }

    /**
     * Tests selector, filtered ingress and egress.
     */
    @Test
    public void testNonTrivialSelectorsIntent() {

        FilteredConnectPoint ingress = new FilteredConnectPoint(connectPoint("of1", 1));

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(connectPoint("of3", 1),
                                         DefaultTrafficSelector.builder().matchVlanId(VlanId.vlanId("100")).build()),
                new FilteredConnectPoint(connectPoint("of4", 1),
                                         DefaultTrafficSelector.builder().matchVlanId(VlanId.vlanId("200")).build())
        );

        TrafficSelector ipPrefixSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.100.0/24"))
                .build();

        SinglePointToMultiPointIntent intent = makeFilteredConnectPointIntent(ingress, egress, ipPrefixSelector);
        String[] hops = {"of2"};

        SinglePointToMultiPointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));

        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(LinkCollectionIntent.class));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(3));

            assertThat(linkIntent.links(), linksHasPath("of1", "of2"));
            assertThat(linkIntent.links(), linksHasPath("of2", "of3"));
            assertThat(linkIntent.links(), linksHasPath("of2", "of4"));

            Set<FilteredConnectPoint> ingressPoints = linkIntent.filteredIngressPoints();
            assertThat("Link collection ingress points do not match base intent",
                       ingressPoints.size() == 1 && ingressPoints.contains(intent.filteredIngressPoint()));

            assertThat("Link collection egress points do not match base intent",
                       linkIntent.filteredEgressPoints().equals(intent.filteredEgressPoints()));
            assertThat(linkIntent.selector(), is(ipPrefixSelector));
        }
        assertThat("key is inherited", resultIntent.key(), is(intent.key()));

    }
}
