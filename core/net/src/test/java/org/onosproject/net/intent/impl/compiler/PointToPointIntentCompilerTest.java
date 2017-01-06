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
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.impl.PathNotFoundException;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.MockResourceService;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.onosproject.net.intent.LinksHaveEntryWithSourceDestinationPairMatcher.linksHasPath;

/**
 * Unit tests for the HostToHost intent compiler.
 */
public class PointToPointIntentCompilerTest extends AbstractIntentTest {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private static final String S1 = "s1";
    private static final String S2 = "s2";
    private static final String S3 = "s3";
    private static final String S4 = "s4";
    private static final String S5 = "s5";
    private static final String S6 = "s6";
    private static final String S7 = "s7";
    private static final String S8 = "s8";

    private static final DeviceId DID_1 = DeviceId.deviceId("of:" + S1);
    private static final DeviceId DID_2 = DeviceId.deviceId("of:" + S2);
    private static final DeviceId DID_3 = DeviceId.deviceId("of:" + S3);
    private static final DeviceId DID_8 = DeviceId.deviceId("of:" + S8);

    private static final PortNumber PORT_1 = PortNumber.portNumber(1);
    private static final PortNumber PORT_2 =  PortNumber.portNumber(2);

    private  static final double BPS_TO_RESERVE = 100.0;

    private static final ContinuousResource RESOURCE_SW1_P1 =
            Resources.continuous(DID_1, PORT_1, Bandwidth.class)
                     .resource(BPS_TO_RESERVE);
    private static final ContinuousResource RESOURCE_SW1_P2 =
            Resources.continuous(DID_1, PORT_2, Bandwidth.class)
                    .resource(BPS_TO_RESERVE);
    private static final ContinuousResource RESOURCE_SW2_P1 =
            Resources.continuous(DID_2, PORT_1, Bandwidth.class)
                    .resource(BPS_TO_RESERVE);
    private static final ContinuousResource RESOURCE_SW2_P2 =
            Resources.continuous(DID_2, PORT_2, Bandwidth.class)
                    .resource(BPS_TO_RESERVE);
    private static final ContinuousResource RESOURCE_SW3_P1 =
            Resources.continuous(DID_3, PORT_1, Bandwidth.class)
                    .resource(BPS_TO_RESERVE);
    private static final ContinuousResource RESOURCE_SW3_P2 =
            Resources.continuous(DID_3, PORT_2, Bandwidth.class)
                    .resource(BPS_TO_RESERVE);

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    /**
     * Creates a PointToPoint intent based on ingress and egress device Ids.
     *
     * @param ingress the ingress connect point
     * @param egress the egress connect point
     * @return PointToPointIntent for the two devices
     */
    private PointToPointIntent makeIntent(ConnectPoint ingress, ConnectPoint egress) {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(selector)
                .treatment(treatment)
                .filteredIngressPoint(new FilteredConnectPoint(ingress))
                .filteredEgressPoint(new FilteredConnectPoint(egress))
                .build();
    }

    /**
     * Creates a PointToPoint intent based on ingress and egress deviceIds and
     * constraints.
     *
     * @param ingress         the ingress connect point
     * @param egress          the egress connect point
     * @param constraints     constraints
     * @return the PointToPointIntent connecting the two connect points with
     * constraints
     */
    private PointToPointIntent makeIntent(ConnectPoint ingress,
                                          ConnectPoint egress,
                                          List<Constraint> constraints) {
        return PointToPointIntent.builder()
                .appId(APPID)
                .selector(selector)
                .treatment(treatment)
                .filteredIngressPoint(new FilteredConnectPoint(ingress))
                .filteredEgressPoint(new FilteredConnectPoint(egress))
                .constraints(constraints)
                .build();
    }

    /**
     * Creates a PointToPoint intent based on ingress and egress deviceIds,
     * constraints and a resource group.
     *
     * @param ingress         the ingress connect point
     * @param egress          the egress connect point
     * @param constraints     constraints
     * @param resourceGroup   the resource group
     * @return the PointToPointIntent connecting the two connect points with
     * constraints
     */
    private PointToPointIntent makeIntent(ConnectPoint ingress,
                                          ConnectPoint egress,
                                          List<Constraint> constraints,
                                          ResourceGroup resourceGroup) {
        return PointToPointIntent.builder()
                .appId(APPID)
                .resourceGroup(resourceGroup)
                .selector(selector)
                .treatment(treatment)
                .filteredIngressPoint(new FilteredConnectPoint(ingress))
                .filteredEgressPoint(new FilteredConnectPoint(egress))
                .constraints(constraints)
                .build();
    }

    /**
     * Creates a PointToPoint intent based on an intent key, ingress and egress
     * deviceIds, constraints and a resource group.
     *
     * @param key             the intent key
     * @param ingress         the ingress connect point
     * @param egress          the egress connect point
     * @param constraints     constraints
     * @param resourceGroup   the resource group
     * @return the PointToPointIntent connecting the two connect points with
     * constraints
     */
    private PointToPointIntent makeIntent(Key key,
                                          ConnectPoint ingress,
                                          ConnectPoint egress,
                                          List<Constraint> constraints,
                                          ResourceGroup resourceGroup) {
        return PointToPointIntent.builder()
                .appId(APPID)
                .key(key)
                .resourceGroup(resourceGroup)
                .selector(selector)
                .treatment(treatment)
                .filteredIngressPoint(new FilteredConnectPoint(ingress))
                .filteredEgressPoint(new FilteredConnectPoint(egress))
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
        return makeCompiler(hops, null);
    }

    /**
     * Creates a point to point intent compiler for a three switch linear
     * topology.
     *
     * @param resourceService service to use for resource allocation requests
     * @return point to point compiler
     */
    private PointToPointIntentCompiler makeCompiler(String[] hops,
                                                    ResourceService resourceService) {
        final PointToPointIntentCompiler compiler = new PointToPointIntentCompiler();
        compiler.pathService = new IntentTestsMocks.MockPathService(hops);

        if (resourceService == null) {
            compiler.resourceService = new MockResourceService();
        } else {
            compiler.resourceService = resourceService;
        }

        return compiler;
    }

    /**
     * Tests a pair of devices in an 8 hop path, forward direction.
     */
    @Test
    public void testForwardPathCompilation() {

        PointToPointIntent intent = makeIntent(new ConnectPoint(DID_1, PORT_1),
                                               new ConnectPoint(DID_8, PORT_1));

        String[] hops = {S1, S2, S3, S4, S5, S6, S7, S8};
        PointToPointIntentCompiler compiler = makeCompiler(hops);

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent forwardResultIntent = result.get(0);
        assertThat(forwardResultIntent instanceof LinkCollectionIntent, is(true));

        if (forwardResultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent forwardIntent = (LinkCollectionIntent) forwardResultIntent;
            FilteredConnectPoint ingressPoint = new FilteredConnectPoint(new ConnectPoint(DID_1, PORT_1));
            FilteredConnectPoint egressPoint = new FilteredConnectPoint(new ConnectPoint(DID_8, PORT_1));
            // 7 links for the hops, plus one default lnk on ingress and egress
            assertThat(forwardIntent.links(), hasSize(hops.length - 1));
            assertThat(forwardIntent.links(), linksHasPath(S1, S2));
            assertThat(forwardIntent.links(), linksHasPath(S2, S3));
            assertThat(forwardIntent.links(), linksHasPath(S3, S4));
            assertThat(forwardIntent.links(), linksHasPath(S4, S5));
            assertThat(forwardIntent.links(), linksHasPath(S5, S6));
            assertThat(forwardIntent.links(), linksHasPath(S6, S7));
            assertThat(forwardIntent.links(), linksHasPath(S7, S8));
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

        PointToPointIntent intent = makeIntent(new ConnectPoint(DID_8, PORT_1),
                                               new ConnectPoint(DID_1, PORT_1));

        String[] hops = {S1, S2, S3, S4, S5, S6, S7, S8};
        PointToPointIntentCompiler compiler = makeCompiler(hops);

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent reverseResultIntent = result.get(0);
        assertThat(reverseResultIntent instanceof LinkCollectionIntent, is(true));

        if (reverseResultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent reverseLinkCollectionIntent = (LinkCollectionIntent) reverseResultIntent;
            FilteredConnectPoint egressPoint = new FilteredConnectPoint(new ConnectPoint(DID_1, PORT_1));
            FilteredConnectPoint ingressPoint = new FilteredConnectPoint(new ConnectPoint(DID_8, PORT_1));
            assertThat(reverseLinkCollectionIntent.links(), hasSize(hops.length - 1));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath(S2, S1));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath(S3, S2));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath(S4, S3));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath(S5, S4));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath(S6, S5));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath(S7, S6));
            assertThat(reverseLinkCollectionIntent.links(), linksHasPath(S8, S7));
            assertThat(reverseLinkCollectionIntent.filteredIngressPoints(), is(ImmutableSet.of(ingressPoint)));
            assertThat(reverseLinkCollectionIntent.filteredEgressPoints(), is(ImmutableSet.of(egressPoint)));
        }
        assertThat("key is inherited", reverseResultIntent.key(), is(intent.key()));
    }

    /**
     * Tests the compilation of an intent which designates two different ports
     * on the same switch.
     */
    @Test
    public void testSameSwitchDifferentPortsIntentCompilation() {
        FilteredConnectPoint src =
                new FilteredConnectPoint(new ConnectPoint(DID_1, PORT_1));
        FilteredConnectPoint dst =
                new FilteredConnectPoint(new ConnectPoint(DID_1, PORT_2));

        PointToPointIntent intent = makeIntent(new ConnectPoint(DID_1, PORT_1),
                                               new ConnectPoint(DID_1, PORT_2));

        String[] hops = {S1};
        PointToPointIntentCompiler compiler = makeCompiler(hops);

        List<Intent> compiled = compiler.compile(intent, null);

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
        final double bpsTotal = 1000.0;
        final double bpsToReserve = 100.0;

        final ResourceService resourceService =
               MockResourceService.makeCustomBandwidthResourceService(bpsTotal);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(bpsToReserve)));

        final PointToPointIntent intent = makeIntent(new ConnectPoint(DID_1, PORT_1),
                                                     new ConnectPoint(DID_3, PORT_2),
                                                     constraints);

        String[] hops = {S1, S2, S3};
        final PointToPointIntentCompiler compiler = makeCompiler(hops,
                                                                 resourceService);

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
        final double bpsTotal = 10.0;

        final ResourceService resourceService =
                MockResourceService.makeCustomBandwidthResourceService(bpsTotal);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(BPS_TO_RESERVE)));

        try {
            final PointToPointIntent intent = makeIntent(new ConnectPoint(DID_1, PORT_1),
                                                         new ConnectPoint(DID_3, PORT_2),
                                                         constraints);

            String[] hops = {S1, S2, S3};
            final PointToPointIntentCompiler compiler = makeCompiler(hops,
                                                                     resourceService);

            compiler.compile(intent, null);

            fail("Point to Point compilation with insufficient bandwidth does "
                    + "not throw exception.");
        } catch (PathNotFoundException noPath) {
            assertThat(noPath.getMessage(), containsString("No path"));
        }
    }

    /**
     * Tests if bandwidth resources get allocated correctly. An intent with a
     * key only is submitted.
     */
    @Test
    public void testBandwidthConstrainedIntentAllocation() {
        final double bpsTotal = 1000.0;

        String[] hops = {S1, S2, S3};

        final ResourceService resourceService =
                MockResourceService.makeCustomBandwidthResourceService(bpsTotal);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(BPS_TO_RESERVE)));

        final PointToPointIntent intent = makeIntent(new ConnectPoint(DID_1, PORT_1),
                                                     new ConnectPoint(DID_3, PORT_2),
                                                     constraints);

        PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

        compiler.compile(intent, null);

        Key intentKey = intent.key();

        ResourceAllocation rAOne = new ResourceAllocation(RESOURCE_SW1_P1, intentKey);
        ResourceAllocation rATwo = new ResourceAllocation(RESOURCE_SW1_P2, intentKey);
        ResourceAllocation rAThree = new ResourceAllocation(RESOURCE_SW2_P1, intentKey);
        ResourceAllocation rAFour = new ResourceAllocation(RESOURCE_SW2_P2, intentKey);
        ResourceAllocation rAFive = new ResourceAllocation(RESOURCE_SW3_P1, intentKey);
        ResourceAllocation rASix = new ResourceAllocation(RESOURCE_SW3_P2, intentKey);

        Set<ResourceAllocation> expectedresourceAllocations =
                ImmutableSet.of(rAOne, rATwo, rAThree, rAFour, rAFive, rASix);

        Set<ResourceAllocation> resourceAllocations =
                ImmutableSet.copyOf(resourceService.getResourceAllocations(intentKey));

        assertThat(resourceAllocations, hasSize(6));
        assertEquals(expectedresourceAllocations, resourceAllocations);
    }

    /**
     * Tests if bandwidth resources get allocated correctly using the resource
     * group. An intent with a resource group is submitted.
     */
    @Test
    public void testRGBandwidthConstrainedIntentAllocation() {
        final double bpsTotal = 1000.0;

        ResourceGroup resourceGroup = ResourceGroup.of(100);

        String[] hops = {S1, S2, S3};

        final ResourceService resourceService =
                MockResourceService.makeCustomBandwidthResourceService(bpsTotal);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(BPS_TO_RESERVE)));

        final PointToPointIntent intent = makeIntent(new ConnectPoint(DID_1, PORT_1),
                                                     new ConnectPoint(DID_3, PORT_2),
                                                     constraints,
                                                     resourceGroup);

        PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

        compiler.compile(intent, null);

        ResourceAllocation rAOne = new ResourceAllocation(RESOURCE_SW1_P1, resourceGroup);
        ResourceAllocation rATwo = new ResourceAllocation(RESOURCE_SW1_P2, resourceGroup);
        ResourceAllocation rAThree = new ResourceAllocation(RESOURCE_SW2_P1, resourceGroup);
        ResourceAllocation rAFour = new ResourceAllocation(RESOURCE_SW2_P2, resourceGroup);
        ResourceAllocation rAFive = new ResourceAllocation(RESOURCE_SW3_P1, resourceGroup);
        ResourceAllocation rASix = new ResourceAllocation(RESOURCE_SW3_P2, resourceGroup);

        Set<ResourceAllocation> expectedresourceAllocations =
                ImmutableSet.of(rAOne, rATwo, rAThree, rAFour, rAFive, rASix);

        Set<ResourceAllocation> resourceAllocations =
                ImmutableSet.copyOf(resourceService.getResourceAllocations(resourceGroup));

        assertThat(resourceAllocations, hasSize(6));
        assertEquals(expectedresourceAllocations, resourceAllocations);
    }

    /**
     * Tests that bandwidth resources don't get allocated twice if the intent
     * is submitted twice.
     */
    @Test
    public void testTwoBandwidthConstrainedIntentAllocation() {
        final double bpsTotal = 1000.0;

        String[] hops = {S1, S2, S3};

        final ResourceService resourceService =
                MockResourceService.makeCustomBandwidthResourceService(bpsTotal);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(BPS_TO_RESERVE)));

        final PointToPointIntent intent = makeIntent(new ConnectPoint(DID_1, PORT_1),
                                                     new ConnectPoint(DID_3, PORT_2),
                                                     constraints);

        PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

        compiler.compile(intent, null);

        // Resubmit the same intent
        compiler.compile(intent, null);

        Key intentKey = intent.key();

        ResourceAllocation rAOne = new ResourceAllocation(RESOURCE_SW1_P1, intentKey);
        ResourceAllocation rATwo = new ResourceAllocation(RESOURCE_SW1_P2, intentKey);
        ResourceAllocation rAThree = new ResourceAllocation(RESOURCE_SW2_P1, intentKey);
        ResourceAllocation rAFour = new ResourceAllocation(RESOURCE_SW2_P2, intentKey);
        ResourceAllocation rAFive = new ResourceAllocation(RESOURCE_SW3_P1, intentKey);
        ResourceAllocation rASix = new ResourceAllocation(RESOURCE_SW3_P2, intentKey);

        Set<ResourceAllocation> expectedresourceAllocations =
                ImmutableSet.of(rAOne, rATwo, rAThree, rAFour, rAFive, rASix);

        Set<ResourceAllocation> resourceAllocations =
                ImmutableSet.copyOf(resourceService.getResourceAllocations(intentKey));

        assertThat(resourceAllocations, hasSize(6));
        assertEquals(expectedresourceAllocations, resourceAllocations);
    }

    /**
     * Tests if bandwidth resources get allocated correctly using groups.
     * An intent asks to allocate bandwidth using the intent key as a reference.
     * Then, the intent is submitted with the same key and a group set.
     * Previous allocations should be released and new resources should be
     * allocated using the group.
     */
    @Test
    public void testKeyRGBandwidthConstrainedIntentAllocation() {
        final double bpsTotal = 1000.0;

        String[] hops = {S1, S2, S3};

        final ResourceService resourceService =
                MockResourceService.makeCustomBandwidthResourceService(bpsTotal);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(BPS_TO_RESERVE)));

        final PointToPointIntent intent = makeIntent(new ConnectPoint(DID_1, PORT_1),
                                                     new ConnectPoint(DID_3, PORT_2),
                                                     constraints);

        PointToPointIntentCompiler compiler = makeCompiler(hops, resourceService);

        compiler.compile(intent, null);

        Key intentKey = intent.key();

        ResourceGroup resourceGroup = ResourceGroup.of(100);

        final PointToPointIntent newIntent = makeIntent(intentKey,
                                                        new ConnectPoint(DID_1, PORT_1),
                                                        new ConnectPoint(DID_3, PORT_2),
                                                        constraints,
                                                        resourceGroup);

        compiler.compile(newIntent, null);

        ResourceAllocation rAOne = new ResourceAllocation(RESOURCE_SW1_P1, resourceGroup);
        ResourceAllocation rATwo = new ResourceAllocation(RESOURCE_SW1_P2, resourceGroup);
        ResourceAllocation rAThree = new ResourceAllocation(RESOURCE_SW2_P1, resourceGroup);
        ResourceAllocation rAFour = new ResourceAllocation(RESOURCE_SW2_P2, resourceGroup);
        ResourceAllocation rAFive = new ResourceAllocation(RESOURCE_SW3_P1, resourceGroup);
        ResourceAllocation rASix = new ResourceAllocation(RESOURCE_SW3_P2, resourceGroup);

        Set<ResourceAllocation> expectedresourceAllocations =
                ImmutableSet.of(rAOne, rATwo, rAThree, rAFour, rAFive, rASix);

        Set<ResourceAllocation> resourceAllocations =
                ImmutableSet.copyOf(resourceService.getResourceAllocations(resourceGroup));

        assertThat(resourceAllocations, hasSize(6));
        assertEquals(expectedresourceAllocations, resourceAllocations);
    }
}
