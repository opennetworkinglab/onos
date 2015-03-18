/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent.impl.installer;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Link;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LambdaConstraint;
import org.onosproject.net.resource.Bandwidth;
import org.onosproject.net.resource.Lambda;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.PID;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.intent.IntentTestsMocks.MockResourceService.makeBandwidthResourceService;
import static org.onosproject.net.intent.IntentTestsMocks.MockResourceService.makeLambdaResourceService;

/**
 * Unit tests for calculating paths for intents with constraints.
 */

public class PathConstraintCalculationTest extends AbstractIntentTest {

    private final IntentTestsMocks.MockSelector selector = new IntentTestsMocks.MockSelector();
    private final IntentTestsMocks.MockTreatment treatment = new IntentTestsMocks.MockTreatment();
    private final ConnectPoint d1p1 = connectPoint("s1", 0);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);
    private final ConnectPoint d3p0 = connectPoint("s3", 10);
    private final ConnectPoint d1p0 = connectPoint("s1", 10);

    private PathIntentInstaller sut;

    @Before
    public void setUpIntentInstaller() {
        sut = new PathIntentInstaller();
        sut.appId = APP_ID;
    }

    private PathIntent createPathIntent(List<Link> links, List<Constraint> constraints) {
        int hops = links.size() - 1;
        return PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .path(new DefaultPath(PID, links, hops))
                .constraints(constraints)
                .priority(333)
                .build();
    }

    /**
     * Tests that installation of bandwidth constrained path intents are
     * successful.
     */
    @Test
    public void testInstallBandwidthConstrainedIntentSuccess() {

        final Constraint constraint = new BandwidthConstraint(Bandwidth.bps(100.0));

        List<Link> links = Arrays.asList(
                createEdgeLink(d1p0, true),
                new DefaultLink(PID, d1p1, d2p0, DIRECT),
                new DefaultLink(PID, d2p1, d3p1, DIRECT),
                createEdgeLink(d3p0, false)
        );
        PathIntent installable = createPathIntent(links, Arrays.asList(constraint));

        sut.resourceService = makeBandwidthResourceService(1000.0);

        final List<Collection<FlowRuleOperation>> flowOperations = sut.install(installable);

        assertThat(flowOperations, notNullValue());
        assertThat(flowOperations, hasSize(1));
    }

    /**
     * Tests that installation of bandwidth constrained path intents fail
     * if there are no available resources.
     */
    @Test
    public void testInstallBandwidthConstrainedIntentFailure() {

        final Constraint constraint = new BandwidthConstraint(Bandwidth.bps(100.0));

        List<Link> links = Arrays.asList(
                createEdgeLink(d1p0, true),
                new DefaultLink(PID, d1p1, d2p0, DIRECT),
                new DefaultLink(PID, d2p1, d3p1, DIRECT),
                createEdgeLink(d3p0, false)
        );
        PathIntent installable = createPathIntent(links, Arrays.asList(constraint));

        // Make it look like the available bandwidth was consumed
        final IntentTestsMocks.MockResourceService resourceService = makeBandwidthResourceService(1000.0);
        resourceService.setAvailableBandwidth(1.0);
        sut.resourceService = resourceService;

        try {
            sut.install(installable);
            fail("Bandwidth request with no available bandwidth did not fail.");
        } catch (IntentTestsMocks.MockedAllocationFailure failure) {
            assertThat(failure,
                       instanceOf(IntentTestsMocks.MockedAllocationFailure.class));
        }
    }

    /**
     * Tests that installation of lambda constrained path intents are
     * successful.
     */
    @Test
    public void testInstallLambdaConstrainedIntentSuccess() {

        final Constraint constraint = new LambdaConstraint(Lambda.valueOf(1));

        List<Link> links = Arrays.asList(
                createEdgeLink(d1p0, true),
                new DefaultLink(PID, d1p1, d2p0, DIRECT),
                new DefaultLink(PID, d2p1, d3p1, DIRECT),
                createEdgeLink(d3p0, false)
        );
        PathIntent installable = createPathIntent(links, Arrays.asList(constraint));

        sut.resourceService = makeLambdaResourceService(1);

        final List<Collection<FlowRuleOperation>> flowOperations = sut.install(installable);

        assertThat(flowOperations, notNullValue());
        assertThat(flowOperations, hasSize(1));
    }

    /**
     * Tests that installation of lambda constrained path intents fail
     * if there are no available resources.
     */
    @Test
    public void testInstallLambdaConstrainedIntentFailure() {

        final Constraint constraint = new LambdaConstraint(Lambda.valueOf(1));

        List<Link> links = Arrays.asList(
                createEdgeLink(d1p0, true),
                new DefaultLink(PID, d1p1, d2p0, DIRECT),
                new DefaultLink(PID, d2p1, d3p1, DIRECT),
                createEdgeLink(d3p0, false)
        );
        PathIntent installable = createPathIntent(links, Arrays.asList(constraint));

        // Make it look like the available lambda was consumed
        final IntentTestsMocks.MockResourceService resourceService = makeLambdaResourceService(1);
        resourceService.setAvailableLambda(0);
        sut.resourceService = resourceService;

        try {
            sut.install(installable);
            fail("Lambda request with no available lambda did not fail.");
        } catch (IntentTestsMocks.MockedAllocationFailure failure) {
            assertThat(failure,
                       instanceOf(IntentTestsMocks.MockedAllocationFailure.class));
        }
    }

}
