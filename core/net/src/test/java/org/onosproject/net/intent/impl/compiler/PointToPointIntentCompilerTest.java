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
import org.onlab.util.Bandwidth;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.impl.PathNotFoundException;
import org.onosproject.net.resource.ResourceService;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.intent.LinksHaveEntryWithSourceDestinationPairMatcher.linksHasPath;

/**
 * Unit tests for the HostToHost intent compiler.
 */
public class PointToPointIntentCompilerTest extends AbstractIntentTest {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    /**
     * Creates a PointToPoint intent based on ingress and egress device Ids.
     *
     * @param ingressIdString string for id of ingress device
     * @param egressIdString  string for id of egress device
     * @return PointToPointIntent for the two devices
     */
    private PointToPointIntent makeIntent(String ingressIdString,
                                          String egressIdString) {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(selector)
                .treatment(treatment)
                .filteredIngressPoint(new FilteredConnectPoint(connectPoint(ingressIdString, 1)))
                .filteredEgressPoint(new FilteredConnectPoint(connectPoint(egressIdString, 1)))
                .build();
    }

    /**
     * Creates a PointToPoint intent based on ingress and egress deviceIds and constraints.
     *
     * @param ingressIdString string for id of ingress device
     * @param egressIdString  string for id of egress device
     * @param constraints     constraints
     * @return PointToPointIntent for the two device with constraints
     */
    private PointToPointIntent makeIntent(String ingressIdString,
                                          String egressIdString, List<Constraint> constraints) {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(selector)
                .treatment(treatment)
                .filteredIngressPoint(new FilteredConnectPoint(connectPoint(ingressIdString, 1)))
                .filteredEgressPoint(new FilteredConnectPoint(connectPoint(egressIdString, 1)))
                .constraints(constraints)
                .build();
    }

    /**
     * Creates a compiler for HostToHost intents.
     *
     * @param hops string array describing the path hops to use when compiling
     * @return HostToHost intent compiler
     */
    private PointToPointIntentCompiler makeCompiler(String[] hops) {
        PointToPointIntentCompiler compiler = new PointToPointIntentCompiler();
        compiler.pathService = new IntentTestsMocks.MockPathService(hops);
        return compiler;
    }

    /**
     * Creates a point to point intent compiler for a three switch linear
     * topology.
     *
     * @param resourceService service to use for resource allocation requests
     * @return point to point compiler
     */
    private PointToPointIntentCompiler makeCompiler(String[] hops, ResourceService resourceService) {
        final PointToPointIntentCompiler compiler = new PointToPointIntentCompiler();
        compiler.resourceService = resourceService;
        compiler.pathService = new IntentTestsMocks.MockPathService(hops);
        return compiler;
    }

    /**
     * Tests a pair of devices in an 8 hop path, forward direction.
     */
    @Test
    public void testForwardPathCompilation() {

        PointToPointIntent intent = makeIntent("d1", "d8");

        String[] hops = {"d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8"};
        PointToPointIntentCompiler compiler = makeCompiler(hops);

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent forwardResultIntent = result.get(0);
        assertThat(forwardResultIntent instanceof LinkCollectionIntent, is(true));

        if (forwardResultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent forwardIntent = (LinkCollectionIntent) forwardResultIntent;
            FilteredConnectPoint ingressPoint = new FilteredConnectPoint(connectPoint("d1", 1));
            FilteredConnectPoint egressPoint = new FilteredConnectPoint(connectPoint("d8", 1));
            // 7 links for the hops, plus one default lnk on ingress and egress
            assertThat(forwardIntent.links(), hasSize(hops.length - 1));
            assertThat(forwardIntent.links(), linksHasPath("d1", "d2"));
            assertThat(forwardIntent.links(), linksHasPath("d2", "d3"));
            assertThat(forwardIntent.links(), linksHasPath("d3", "d4"));
            assertThat(forwardIntent.links(), linksHasPath("d4", "d5"));
            assertThat(forwardIntent.links(), linksHasPath("d5", "d6"));
            assertThat(forwardIntent.links(), linksHasPath("d6", "d7"));
            assertThat(forwardIntent.links(), linksHasPath("d7", "d8"));
            assertThat(forwardIntent.filteredIngressPoints(), is(ImmutableSet.of(ingressPoint)));
            assertThat(forwardIntent.filteredEgressPoints(), is(ImmutableSet.of(egressPoint)));
        }
        assertThat("key is inherited", forwardResultIntent.key(), is(intent.key()));
    }

    /**
     * Tests a pair of devices in an 8 hop path, forward direction.
     */
    @Test
    public void testReversePathCompilation() {

        PointToPointIntent intent = makeIntent("d8", "d1");

        String[] hops = {"d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8"};
        PointToPointIntentCompiler compiler = makeCompiler(hops);

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent reverseResultIntent = result.get(0);
        assertThat(reverseResultIntent instanceof LinkCollectionIntent, is(true));

        if (reverseResultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent reverseLinkCollectionIntent = (LinkCollectionIntent) reverseResultIntent;
            FilteredConnectPoint egressPoint = new FilteredConnectPoint(connectPoint("d1", 1));
            FilteredConnectPoint ingressPoint = new FilteredConnectPoint(connectPoint("d8", 1));
            assertThat(reverseLinkCollectionIntent.links(), hasSize(hops.length - 1));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath("d2", "d1"));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath("d3", "d2"));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath("d4", "d3"));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath("d5", "d4"));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath("d6", "d5"));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath("d7", "d6"));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath("d8", "d7"));
            assertThat(reverseLinkCollectionIntent.filteredIngressPoints(), is(ImmutableSet.of(ingressPoint)));
            assertThat(reverseLinkCollectionIntent.filteredEgressPoints(), is(ImmutableSet.of(egressPoint)));
        }
        assertThat("key is inherited", reverseResultIntent.key(), is(intent.key()));
    }

    /**
     * Tests compilation of the intent which designates two different ports on the same switch.
     */
    @Test
    public void testSameSwitchDifferentPortsIntentCompilation() {
        FilteredConnectPoint src = new FilteredConnectPoint(new ConnectPoint(deviceId("1"), portNumber(1)));
        FilteredConnectPoint dst = new FilteredConnectPoint(new ConnectPoint(deviceId("1"), portNumber(2)));
        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .filteredIngressPoint(src)
                .filteredEgressPoint(dst)
                .build();

        String[] hops = {"1"};
        PointToPointIntentCompiler sut = makeCompiler(hops);

        List<Intent> compiled = sut.compile(intent, null);

        assertThat("key is inherited",
                   compiled.stream().map(Intent::key).collect(Collectors.toList()),
                   everyItem(is(intent.key())));

        assertThat(compiled, hasSize(1));
        assertThat(compiled.get(0), is(instanceOf(LinkCollectionIntent.class)));
        LinkCollectionIntent linkCollectionIntent = (LinkCollectionIntent) compiled.get(0);
        Set<Link> links = linkCollectionIntent.links();

        assertThat(links, hasSize(0));
        assertThat(linkCollectionIntent.filteredIngressPoints(), is(ImmutableSet.of(src)));
        assertThat(linkCollectionIntent.filteredEgressPoints(), is(ImmutableSet.of(dst)));
    }

    /**
     * Tests that requests with sufficient available bandwidth succeed.
     */
    @Test
    public void testBandwidthConstrainedIntentSuccess() {

        final ResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(1000.0);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(100.0)));

        final PointToPointIntent intent = makeIntent("s1", "s3", constraints);

        String[] hops = {"s1", "s2", "s3"};
        final PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

        final List<Intent> compiledIntents = compiler.compile(intent, null);

        assertThat(compiledIntents, Matchers.notNullValue());
        assertThat(compiledIntents, hasSize(1));

        assertThat("key is inherited",
                   compiledIntents.stream().map(Intent::key).collect(Collectors.toList()),
                   everyItem(is(intent.key())));

    }

    /**
     * Tests that requests with insufficient available bandwidth fail.
     */
    @Test
    public void testBandwidthConstrainedIntentFailure() {

        final ResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(10.0);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(100.0)));

        try {
            final PointToPointIntent intent = makeIntent("s1", "s3", constraints);

            String[] hops = {"s1", "s2", "s3"};
            final PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

            compiler.compile(intent, null);

            fail("Point to Point compilation with insufficient bandwidth does "
                    + "not throw exception.");
        } catch (PathNotFoundException noPath) {
            assertThat(noPath.getMessage(), containsString("No path"));
        }
    }
}
