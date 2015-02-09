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
package org.onosproject.net.intent.impl;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.onosproject.net.flow.FlowRuleBatchOperation;
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
import org.onosproject.net.resource.Bandwidth;
import org.onosproject.net.resource.Lambda;
import org.onosproject.net.resource.LinkResourceService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Unit tests for calculating paths for intents with constraints.
 */

public class PathConstraintCalculationTest extends AbstractIntentTest {

    /**
     * Creates a point to point intent compiler for a three switch linear
     * topology.
     *
     * @param resourceService service to use for resource allocation requests
     * @return point to point compiler
     */
    private PointToPointIntentCompiler makeCompiler(LinkResourceService resourceService) {
        final String[] hops = {"s1", "s2", "s3"};
        final PointToPointIntentCompiler compiler = new PointToPointIntentCompiler();
        compiler.resourceService = resourceService;
        compiler.pathService = new IntentTestsMocks.MockPathService(hops);
        return compiler;
    }

    /**
     * Creates an intent with a given constraint and compiles it. The compiler
     * will throw PathNotFoundException if the allocations cannot be satisfied.
     *
     * @param constraint constraint to apply to the created intent
     * @param resourceService service to use for resource allocation requests
     * @return List of compiled intents
     */
    private List<Intent> compileIntent(Constraint constraint,
                                       LinkResourceService resourceService) {
        final List<Constraint> constraints = new LinkedList<>();
        constraints.add(constraint);
        final TrafficSelector selector = new IntentTestsMocks.MockSelector();
        final TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

        final PointToPointIntent intent =
                new PointToPointIntent(APP_ID,
                                       selector,
                                       treatment,
                                       connectPoint("s1", 1),
                                       connectPoint("s3", 1),
                                       constraints);
        final PointToPointIntentCompiler compiler = makeCompiler(resourceService);

        return compiler.compile(intent, null, null);
    }

    /**
     * Installs a compiled path intent and returns the flow rules it generates.
     *
     * @param compiledIntents list of compiled intents
     * @param resourceService service to use for resource allocation requests
     * @return
     */
    private List<FlowRuleBatchOperation> installIntents(List<Intent> compiledIntents,
                                                        LinkResourceService resourceService) {
        final PathIntent path = (PathIntent) compiledIntents.get(0);

        final PathIntentInstaller installer = new PathIntentInstaller();
        installer.resourceService = resourceService;
        installer.appId = APP_ID;
        return installer.install(path);
    }

    /**
     * Tests that requests with sufficient available bandwidth succeed.
     */
    @Test
    public void testBandwidthConstrainedIntentSuccess() {

        final LinkResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(1000.0);
        final Constraint constraint = new BandwidthConstraint(Bandwidth.bps(100.0));

        final List<Intent> compiledIntents = compileIntent(constraint, resourceService);
        assertThat(compiledIntents, notNullValue());
        assertThat(compiledIntents, hasSize(1));
    }

    /**
     * Tests that requests with insufficient available bandwidth fail.
     */
    @Test
    public void testBandwidthConstrainedIntentFailure() {

        final LinkResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(10.0);
        final Constraint constraint = new BandwidthConstraint(Bandwidth.bps(100.0));

        try {
            compileIntent(constraint, resourceService);
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

        final Constraint constraint = new LambdaConstraint(Lambda.valueOf(1));
        final LinkResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeLambdaResourceService(1);

        final List<Intent> compiledIntents =
                compileIntent(constraint, resourceService);
        assertThat(compiledIntents, notNullValue());
        assertThat(compiledIntents, hasSize(1));
    }

    /**
     * Tests that requests for lambdas when there are no available lambdas
     * fail.
     */
    @Test
    public void testLambdaConstrainedIntentFailure() {

        final Constraint constraint = new LambdaConstraint(Lambda.valueOf(1));
        final LinkResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(10.0);
        try {
            compileIntent(constraint, resourceService);
            fail("Point to Point compilation with no available lambda does "
                    + "not throw exception.");
        } catch (PathNotFoundException noPath) {
            assertThat(noPath.getMessage(), containsString("No path"));
        }
    }

    /**
     * Tests that installation of bandwidth constrained path intents are
     * successful.
     */
    @Test
    public void testInstallBandwidthConstrainedIntentSuccess() {

        final IntentTestsMocks.MockResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(1000.0);
        final Constraint constraint = new BandwidthConstraint(Bandwidth.bps(100.0));

        final List<Intent> compiledIntents = compileIntent(constraint, resourceService);
        assertThat(compiledIntents, notNullValue());
        assertThat(compiledIntents, hasSize(1));

        final List<FlowRuleBatchOperation> flowOperations =
                installIntents(compiledIntents, resourceService);

        assertThat(flowOperations, notNullValue());
        assertThat(flowOperations, hasSize(1));
    }

    /**
     * Tests that installation of bandwidth constrained path intents fail
     * if there are no available resources.
     */
    @Test
    public void testInstallBandwidthConstrainedIntentFailure() {

        final IntentTestsMocks.MockResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeBandwidthResourceService(1000.0);
        final Constraint constraint = new BandwidthConstraint(Bandwidth.bps(100.0));

        final List<Intent> compiledIntents = compileIntent(constraint, resourceService);
        assertThat(compiledIntents, notNullValue());
        assertThat(compiledIntents, hasSize(1));

        // Make it look like the available bandwidth was consumed
        resourceService.setAvailableBandwidth(1.0);

        try {
            installIntents(compiledIntents, resourceService);
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

        final IntentTestsMocks.MockResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeLambdaResourceService(1);
        final Constraint constraint = new LambdaConstraint(Lambda.valueOf(1));

        final List<Intent> compiledIntents = compileIntent(constraint, resourceService);
        assertThat(compiledIntents, notNullValue());
        assertThat(compiledIntents, hasSize(1));

        final List<FlowRuleBatchOperation> flowOperations =
                installIntents(compiledIntents, resourceService);

        assertThat(flowOperations, notNullValue());
        assertThat(flowOperations, hasSize(1));
    }

    /**
     * Tests that installation of lambda constrained path intents fail
     * if there are no available resources.
     */
    @Test
    public void testInstallLambdaConstrainedIntentFailure() {

        final IntentTestsMocks.MockResourceService resourceService =
                IntentTestsMocks.MockResourceService.makeLambdaResourceService(1);
        final Constraint constraint = new LambdaConstraint(Lambda.valueOf(1));

        final List<Intent> compiledIntents = compileIntent(constraint, resourceService);
        assertThat(compiledIntents, notNullValue());
        assertThat(compiledIntents, hasSize(1));

        // Make it look like the available lambda was consumed
        resourceService.setAvailableLambda(0);

        try {
            installIntents(compiledIntents, resourceService);
            fail("Lambda request with no available lambda did not fail.");
        } catch (IntentTestsMocks.MockedAllocationFailure failure) {
            assertThat(failure,
                       instanceOf(IntentTestsMocks.MockedAllocationFailure.class));
        }
    }

}
