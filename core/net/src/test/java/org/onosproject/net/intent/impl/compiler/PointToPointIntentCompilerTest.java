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
import org.onlab.util.Bandwidth;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LambdaConstraint;
import org.onosproject.net.intent.impl.PathNotFoundException;
import org.onosproject.net.resource.link.LinkResourceService;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
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
                .ingressPoint(connectPoint(ingressIdString, 1))
                .egressPoint(connectPoint(egressIdString, 1))
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
                .ingressPoint(connectPoint(ingressIdString, 1))
                .egressPoint(connectPoint(egressIdString, 1))
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
    private PointToPointIntentCompiler makeCompiler(String[] hops, LinkResourceService resourceService) {
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

        List<Intent> result = compiler.compile(intent, null, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent forwardResultIntent = result.get(0);
        assertThat(forwardResultIntent instanceof PathIntent, is(true));

        if (forwardResultIntent instanceof PathIntent) {
            PathIntent forwardPathIntent = (PathIntent) forwardResultIntent;
            // 7 links for the hops, plus one default lnk on ingress and egress
            assertThat(forwardPathIntent.path().links(), hasSize(hops.length + 1));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d1", "d2"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d2", "d3"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d3", "d4"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d4", "d5"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d5", "d6"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d6", "d7"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("d7", "d8"));
        }
    }

    /**
     * Tests a pair of devices in an 8 hop path, forward direction.
     */
    @Test
    public void testReversePathCompilation() {

        PointToPointIntent intent = makeIntent("d8", "d1");

        String[] hops = {"d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8"};
        PointToPointIntentCompiler compiler = makeCompiler(hops);

        List<Intent> result = compiler.compile(intent, null, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent reverseResultIntent = result.get(0);
        assertThat(reverseResultIntent instanceof PathIntent, is(true));

        if (reverseResultIntent instanceof PathIntent) {
            PathIntent reversePathIntent = (PathIntent) reverseResultIntent;
            assertThat(reversePathIntent.path().links(), hasSize(hops.length + 1));
            assertThat(reversePathIntent.path().links(), linksHasPath("d2", "d1"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d3", "d2"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d4", "d3"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d5", "d4"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d6", "d5"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d7", "d6"));
            assertThat(reversePathIntent.path().links(), linksHasPath("d8", "d7"));
        }
    }

    /**
     * Tests compilation of the intent which designates two different ports on the same switch.
     */
    @Test
    public void testSameSwitchDifferentPortsIntentCompilation() {
        ConnectPoint src = new ConnectPoint(deviceId("1"), portNumber(1));
        ConnectPoint dst = new ConnectPoint(deviceId("1"), portNumber(2));
        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(src)
                .egressPoint(dst)
                .build();

        String[] hops = {"1"};
        PointToPointIntentCompiler sut = makeCompiler(hops);

        List<Intent> compiled = sut.compile(intent, null, null);

        assertThat(compiled, hasSize(1));
        assertThat(compiled.get(0), is(instanceOf(PathIntent.class)));
        Path path = ((PathIntent) compiled.get(0)).path();

        assertThat(path.links(), hasSize(2));
        Link firstLink = path.links().get(0);
        assertThat(firstLink, is(createEdgeLink(src, true)));
        Link secondLink = path.links().get(1);
        assertThat(secondLink, is(createEdgeLink(dst, false)));
    }

    /**
     * Tests that requests with sufficient available bandwidth succeed.
     */
    @Test
    public void testBandwidthConstrainedIntentSuccess() {

        final LinkResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(1000.0);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(100.0)));

        final PointToPointIntent intent = makeIntent("s1", "s3", constraints);

        String[] hops = {"s1", "s2", "s3"};
        final PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

        final List<Intent> compiledIntents = compiler.compile(intent, null, null);

        assertThat(compiledIntents, Matchers.notNullValue());
        assertThat(compiledIntents, hasSize(1));
    }

    /**
     * Tests that requests with insufficient available bandwidth fail.
     */
    @Test
    public void testBandwidthConstrainedIntentFailure() {

        final LinkResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(10.0);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(100.0)));

        try {
            final PointToPointIntent intent = makeIntent("s1", "s3", constraints);

            String[] hops = {"s1", "s2", "s3"};
            final PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

            compiler.compile(intent, null, null);

            fail("Point to Point compilation with insufficient bandwidth does "
                    + "not throw exception.");
        } catch (PathNotFoundException noPath) {
            assertThat(noPath.getMessage(), containsString("No path"));
        }
    }

    /**
     * Tests that requests for available lambdas are successful.
     */
    @Test
    public void testLambdaConstrainedIntentSuccess() {

        final List<Constraint> constraints =
                Collections.singletonList(new LambdaConstraint(new IndexedLambda(1)));
        final LinkResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeLambdaResourceService(1);

        final PointToPointIntent intent = makeIntent("s1", "s3", constraints);

        String[] hops = {"s1", "s2", "s3"};
        final PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

        final List<Intent> compiledIntents =
                compiler.compile(intent, null, null);

        assertThat(compiledIntents, Matchers.notNullValue());
        assertThat(compiledIntents, hasSize(1));
    }

    /**
     * Tests that requests for lambdas when there are no available lambdas
     * fail.
     */
    @Test
    public void testLambdaConstrainedIntentFailure() {

        final List<Constraint> constraints =
                Collections.singletonList(new LambdaConstraint(new IndexedLambda(1)));
        final LinkResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(10.0);
        try {
            final PointToPointIntent intent = makeIntent("s1", "s3", constraints);

            String[] hops = {"s1", "s2", "s3"};
            final PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

            compiler.compile(intent, null, null);

            fail("Point to Point compilation with no available lambda does "
                    + "not throw exception.");
        } catch (PathNotFoundException noPath) {
            assertThat(noPath.getMessage(), containsString("No path"));
        }
    }

}
