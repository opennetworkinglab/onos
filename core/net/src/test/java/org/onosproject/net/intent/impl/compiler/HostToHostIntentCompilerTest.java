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
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.resource.MockResourceService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.NetTestTools.hid;
import static org.onosproject.net.intent.LinksHaveEntryWithSourceDestinationPairMatcher.linksHasPath;

/**
 * Unit tests for the HostToHost intent compiler.
 */
public class HostToHostIntentCompilerTest extends AbstractIntentTest {
    private static final String HOST_ONE_MAC = "00:00:00:00:00:01";
    private static final String HOST_TWO_MAC = "00:00:00:00:00:02";
    private static final String HOST_ONE_VLAN = "None";
    private static final String HOST_TWO_VLAN = "None";
    private static final String HOST_ONE = HOST_ONE_MAC + "/" + HOST_ONE_VLAN;
    private static final String HOST_TWO = HOST_TWO_MAC + "/" + HOST_TWO_VLAN;

    private static final int PORT_1 = 1;

    private static final String HOP_1 = "h1";
    private static final String HOP_2 = "h2";
    private static final String HOP_3 = "h3";
    private static final String HOP_4 = "h4";
    private static final String HOP_5 = "h5";
    private static final String HOP_6 = "h6";
    private static final String HOP_7 = "h7";
    private static final String HOP_8 = "h8";

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    private HostId hostOneId = HostId.hostId(HOST_ONE);
    private HostId hostTwoId = HostId.hostId(HOST_TWO);
    private HostService mockHostService;

    @Override
    @Before
    public void setUp() throws Exception {
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
     * @param oneIdString string for host one id
     * @param twoIdString string for host two id
     * @return HostToHostIntent for the two hosts
     */
    private HostToHostIntent makeIntent(String oneIdString, String twoIdString) {
        return HostToHostIntent.builder()
                .appId(APPID)
                .one(hid(oneIdString))
                .two(hid(twoIdString))
                .selector(selector)
                .treatment(treatment)
                .build();
    }

    /**
     * Creates a compiler for HostToHost intents.
     *
     * @param hops string array describing the path hops to use when compiling
     * @return HostToHost intent compiler
     */
    private HostToHostIntentCompiler makeCompiler(String[] hops) {
        HostToHostIntentCompiler compiler =
                new HostToHostIntentCompiler();
        compiler.pathService = new IntentTestsMocks.MockPathService(hops);
        compiler.hostService = mockHostService;
        compiler.resourceService = new MockResourceService();
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

        String[] hops = {HOST_ONE, HOP_1, HOP_2, HOP_3, HOP_4, HOP_5, HOP_6, HOP_7, HOP_8, HOST_TWO};
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
                new FilteredConnectPoint(connectPoint(HOP_1, PORT_1))
        );
        assertThat(forwardLCIntent.filteredIngressPoints(), is(ingressPoints));
        assertThat(links, linksHasPath(HOP_1, HOP_2));
        assertThat(links, linksHasPath(HOP_2, HOP_3));
        assertThat(links, linksHasPath(HOP_3, HOP_4));
        assertThat(links, linksHasPath(HOP_4, HOP_5));
        assertThat(links, linksHasPath(HOP_5, HOP_6));
        assertThat(links, linksHasPath(HOP_6, HOP_7));
        assertThat(links, linksHasPath(HOP_7, HOP_8));
        Set<FilteredConnectPoint> egressPoints = ImmutableSet.of(
                new FilteredConnectPoint(connectPoint(HOP_8, PORT_1))
        );
        assertThat(forwardLCIntent.filteredEgressPoints(), is(egressPoints));

        LinkCollectionIntent reverseLCIntent = (LinkCollectionIntent) reverseIntent;
        links = reverseLCIntent.links();
        assertThat(reverseLCIntent.links(), hasSize(7));
        ingressPoints = ImmutableSet.of(new FilteredConnectPoint(connectPoint(HOP_8, PORT_1)));
        assertThat(reverseLCIntent.filteredIngressPoints(), is(ingressPoints));
        assertThat(links, linksHasPath(HOP_2, HOP_1));
        assertThat(links, linksHasPath(HOP_3, HOP_2));
        assertThat(links, linksHasPath(HOP_4, HOP_3));
        assertThat(links, linksHasPath(HOP_5, HOP_4));
        assertThat(links, linksHasPath(HOP_6, HOP_5));
        assertThat(links, linksHasPath(HOP_7, HOP_6));
        assertThat(links, linksHasPath(HOP_8, HOP_7));
        egressPoints = ImmutableSet.of(new FilteredConnectPoint(connectPoint(HOP_1, PORT_1)));
        assertThat(reverseLCIntent.filteredEgressPoints(), is(egressPoints));


        assertThat("key is inherited",
                   result.stream().map(Intent::key).collect(Collectors.toList()),
                   everyItem(is(intent.key())));
    }
}
