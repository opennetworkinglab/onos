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
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.MockResourceService;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.NetTestTools.hid;
import static org.onosproject.net.intent.LinksHaveEntryWithSourceDestinationPairMatcher.linksHasPath;

/**
 * Unit tests for the HostToHost intent compiler.
 */
public class HostToHostIntentCompilerTest extends AbstractIntentTest {
    private static final ApplicationId APPID = new TestApplicationId("foo");

    private static final String HOST_ONE_MAC = "00:00:00:00:00:01";
    private static final String HOST_TWO_MAC = "00:00:00:00:00:02";
    private static final String HOST_ONE_VLAN = "None";
    private static final String HOST_TWO_VLAN = "None";
    private static final String HOST_ONE = HOST_ONE_MAC + "/" + HOST_ONE_VLAN;
    private static final String HOST_TWO = HOST_TWO_MAC + "/" + HOST_TWO_VLAN;

    private static final String S1 = "s1";
    private static final String S2 = "s2";
    private static final String S3 = "s3";
    private static final String S4 = "s4";
    private static final String S5 = "s5";
    private static final String S6 = "s6";
    private static final String S7 = "s7";
    private static final String S8 = "s8";

    private static final DeviceId DID_S1 = DeviceId.deviceId("of:" + S1);
    private static final DeviceId DID_S2 = DeviceId.deviceId("of:" + S2);
    private static final DeviceId DID_S3 = DeviceId.deviceId("of:" + S3);
    private static final DeviceId DID_S8 = DeviceId.deviceId("of:" + S8);

    private static final PortNumber PORT_1 = PortNumber.portNumber(1);
    private static final PortNumber PORT_2 =  PortNumber.portNumber(2);

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    private HostId hostOneId = HostId.hostId(HOST_ONE);
    private HostId hostTwoId = HostId.hostId(HOST_TWO);

    private HostService mockHostService;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        Host hostOne = createMock(Host.class);
        expect(hostOne.mac()).andReturn(new MacAddress(HOST_ONE_MAC.getBytes())).anyTimes();
        expect(hostOne.vlan()).andReturn(VlanId.vlanId()).anyTimes();
        replay(hostOne);

        Host hostTwo = createMock(Host.class);
        expect(hostTwo.mac()).andReturn(new MacAddress(HOST_TWO_MAC.getBytes())).anyTimes();
        expect(hostTwo.vlan()).andReturn(VlanId.vlanId()).anyTimes();
        replay(hostTwo);

        mockHostService = createMock(HostService.class);
        expect(mockHostService.getHost(eq(hostOneId))).andReturn(hostOne).anyTimes();
        expect(mockHostService.getHost(eq(hostTwoId))).andReturn(hostTwo).anyTimes();
        replay(mockHostService);
    }

    /**
     * Creates a HostToHost intent based on two host Ids.
     *
     * @param oneIdString the string for host one id
     * @param twoIdString the string for host two id
     * @return HostToHostIntent for the two hosts
     */
    private HostToHostIntent makeIntent(String oneIdString,
                                        String twoIdString) {
        return makeIntent(oneIdString, twoIdString, Lists.newArrayList());
    }

    /**
     * Creates a HostToHost intent based on two host Ids and a list of
     * constraints.
     *
     * @param oneIdString the string for host one id
     * @param twoIdString the string for host two id
     * @param constraints the intent constraints
     * @return HostToHostIntent for the two hosts
     */
    private HostToHostIntent makeIntent(String oneIdString,
                                        String twoIdString,
                                        List<Constraint> constraints) {
        return HostToHostIntent.builder()
                .appId(APPID)
                .one(hid(oneIdString))
                .two(hid(twoIdString))
                .selector(selector)
                .treatment(treatment)
                .constraints(constraints)
                .build();
    }

    /**
     * Creates a compiler for HostToHost intents.
     *
     * @param hops string array describing the path hops to use when compiling
     * @return HostToHost intent compiler
     */
    private HostToHostIntentCompiler makeCompiler(String[] hops) {
        return makeCompiler(hops, null);
    }

    /**
     * Creates a compiler for HostToHost intents.
     *
     * @param hops string array describing the path hops to use when compiling
     * @param resourceService the resource service
     * @return HostToHost intent compiler
     */
    private HostToHostIntentCompiler makeCompiler(String[] hops,
                                                  ResourceService resourceService) {
        HostToHostIntentCompiler compiler =
                new HostToHostIntentCompiler();
        compiler.pathService = new IntentTestsMocks.MockPathService(hops);
        compiler.hostService = mockHostService;

        if (resourceService == null) {
            compiler.resourceService = new MockResourceService();
        } else {
            compiler.resourceService = resourceService;
        }

        return compiler;
    }


    /**
     * Tests a pair of hosts with 8 hops between them.
     */
    @Test
    public void testSingleLongPathCompilation() {

        HostToHostIntent intent = makeIntent(HOST_ONE,
                                             HOST_TWO);
        assertThat(intent, is(notNullValue()));

        String[] hops = {HOST_ONE, S1, S2, S3, S4, S5, S6, S7, S8, HOST_TWO};
        HostToHostIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(2));
        Intent forwardIntent = result.get(0);
        assertThat(forwardIntent instanceof LinkCollectionIntent, is(true));
        Intent reverseIntent = result.get(1);
        assertThat(reverseIntent instanceof LinkCollectionIntent, is(true));

        LinkCollectionIntent forwardLCIntent = (LinkCollectionIntent) forwardIntent;
        Set<Link> links = forwardLCIntent.links();
        assertThat(links, hasSize(7));
        Set<FilteredConnectPoint> ingressPoints = ImmutableSet.of(
                new FilteredConnectPoint(new ConnectPoint(DID_S1, PORT_1))
        );
        assertThat(forwardLCIntent.filteredIngressPoints(), is(ingressPoints));
        assertThat(links, linksHasPath(S1, S2));
        assertThat(links, linksHasPath(S2, S3));
        assertThat(links, linksHasPath(S3, S4));
        assertThat(links, linksHasPath(S4, S5));
        assertThat(links, linksHasPath(S5, S6));
        assertThat(links, linksHasPath(S6, S7));
        assertThat(links, linksHasPath(S7, S8));
        Set<FilteredConnectPoint> egressPoints = ImmutableSet.of(
                new FilteredConnectPoint(new ConnectPoint(DID_S8, PORT_2))
        );
        assertThat(forwardLCIntent.filteredEgressPoints(), is(egressPoints));

        LinkCollectionIntent reverseLCIntent = (LinkCollectionIntent) reverseIntent;
        links = reverseLCIntent.links();
        assertThat(reverseLCIntent.links(), hasSize(7));
        ingressPoints = ImmutableSet.of(new FilteredConnectPoint(new ConnectPoint(DID_S8, PORT_2)));
        assertThat(reverseLCIntent.filteredIngressPoints(), is(ingressPoints));
        assertThat(links, linksHasPath(S2, S1));
        assertThat(links, linksHasPath(S3, S2));
        assertThat(links, linksHasPath(S4, S3));
        assertThat(links, linksHasPath(S5, S4));
        assertThat(links, linksHasPath(S6, S5));
        assertThat(links, linksHasPath(S7, S6));
        assertThat(links, linksHasPath(S8, S7));
        egressPoints = ImmutableSet.of(new FilteredConnectPoint(new ConnectPoint(DID_S1, PORT_1)));
        assertThat(reverseLCIntent.filteredEgressPoints(), is(egressPoints));


        assertThat("key is inherited",
                   result.stream().map(Intent::key).collect(Collectors.toList()),
                   everyItem(is(intent.key())));
    }

    /**
     * Tests if bandwidth resources get allocated correctly.
     */
    @Test
    public void testBandwidthConstrainedIntentAllocation() {
        final double bpsTotal = 1000.0;
        final double bpsToReserve = 100.0;

        ContinuousResource resourceSw1P1 =
                Resources.continuous(DID_S1, PORT_1, Bandwidth.class)
                        .resource(bpsToReserve);
        ContinuousResource resourceSw1P2 =
                Resources.continuous(DID_S1, PORT_2, Bandwidth.class)
                        .resource(bpsToReserve);
        ContinuousResource resourceSw2P1 =
                Resources.continuous(DID_S2, PORT_1, Bandwidth.class)
                        .resource(bpsToReserve);
        ContinuousResource resourceSw2P2 =
                Resources.continuous(DID_S2, PORT_2, Bandwidth.class)
                        .resource(bpsToReserve);
        ContinuousResource resourceSw3P1 =
                Resources.continuous(DID_S3, PORT_1, Bandwidth.class)
                        .resource(bpsToReserve);
        ContinuousResource resourceSw3P2 =
                Resources.continuous(DID_S3, PORT_2, Bandwidth.class)
                        .resource(bpsToReserve);

        String[] hops = {HOST_ONE, S1, S2, S3, HOST_TWO};

        final ResourceService resourceService =
                MockResourceService.makeCustomBandwidthResourceService(bpsTotal);
        final List<Constraint> constraints =
                Collections.singletonList(new BandwidthConstraint(Bandwidth.bps(bpsToReserve)));

        final HostToHostIntent intent = makeIntent(HOST_ONE, HOST_TWO, constraints);

        HostToHostIntentCompiler compiler = makeCompiler(hops, resourceService);

        compiler.compile(intent, null);

        Key intentKey = intent.key();

        ResourceAllocation rAOne = new ResourceAllocation(resourceSw1P1, intentKey);
        ResourceAllocation rATwo = new ResourceAllocation(resourceSw1P2, intentKey);
        ResourceAllocation rAThree = new ResourceAllocation(resourceSw2P1, intentKey);
        ResourceAllocation rAFour = new ResourceAllocation(resourceSw2P2, intentKey);
        ResourceAllocation rAFive = new ResourceAllocation(resourceSw3P1, intentKey);
        ResourceAllocation rASix = new ResourceAllocation(resourceSw3P2, intentKey);

        Set<ResourceAllocation> expectedresourceAllocations =
                ImmutableSet.of(rAOne, rATwo, rAThree, rAFour, rAFive, rASix);

        Set<ResourceAllocation> resourceAllocations =
                ImmutableSet.copyOf(resourceService.getResourceAllocations(intentKey));

        assertThat(resourceAllocations, hasSize(6));
        assertEquals(expectedresourceAllocations, resourceAllocations);
    }
}
