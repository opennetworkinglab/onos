/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import org.hamcrest.Matchers;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.topology.PathServiceAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.NetTestTools.createPath;
import static org.onosproject.net.intent.LinksHaveEntryWithSourceDestinationPairMatcher.linksHasPath;

/**
 * Unit tests for the MultiPointToSinglePoint intent compiler.
 */
public class MultiPointToSinglePointIntentCompilerTest extends AbstractIntentTest {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    /**
     * Mock path service for creating paths within the test.
     */
    private static class MockPathService extends PathServiceAdapter {

        final String[] pathHops;

        /**
         * Constructor that provides a set of hops to mock.
         *
         * @param pathHops path hops to mock
         */
        MockPathService(String[] pathHops) {
            this.pathHops = pathHops;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst) {
            Set<Path> result = new HashSet<>();

            String[] allHops = new String[pathHops.length + 1];
            allHops[0] = src.toString();
            if (pathHops.length != 0) {
                System.arraycopy(pathHops, 0, allHops, 1, pathHops.length);
            }
            result.add(createPath(allHops));

            return result;
        }
    }

    /**
     * Mocks the device service so that a device appears available in the test.
     */
    private static class MockDeviceService extends DeviceServiceAdapter {
        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return true;
        }
    }

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
     * Creates a compiler for MultiPointToSinglePoint intents.
     *
     * @param hops hops to use while computing paths for this intent
     * @return MultiPointToSinglePoint intent
     */
    private MultiPointToSinglePointIntentCompiler makeCompiler(String[] hops) {
        MultiPointToSinglePointIntentCompiler compiler =
                new MultiPointToSinglePointIntentCompiler();
        compiler.pathService = new MockPathService(hops);
        compiler.deviceService = new MockDeviceService();
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

        String[] hops = {"h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8",
                egress};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null, null);
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

        final String[] hops = {"inner1", "inner2", egress};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null, null);
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

        final String[] hops = {"n1", egress};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null, null);
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
    }

    /**
     * Tests ingress and egress on the same device.
     */
    @Test
    public void testSameDeviceCompilation() {
        String[] ingress = {"i1", "i2"};
        String egress = "i1";

        MultiPointToSinglePointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {"i1", "i2"};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null, null);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(LinkCollectionIntent.class));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(ingress.length));

            assertThat(linkIntent.links(), linksHasPath("i2", "i1"));
        }
    }
}
