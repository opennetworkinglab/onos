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

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.TestApplicationId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.resource.MockResourceService;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.util.List;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    private HostId hostOneId = HostId.hostId(HOST_ONE);
    private HostId hostTwoId = HostId.hostId(HOST_TWO);
    private HostService mockHostService;

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

        String[] hops = {HOST_ONE, "h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8", HOST_TWO};
        HostToHostIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent, null);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(2));
        Intent forwardResultIntent = result.get(0);
        assertThat(forwardResultIntent instanceof PathIntent, is(true));
        Intent reverseResultIntent = result.get(1);
        assertThat(reverseResultIntent instanceof PathIntent, is(true));

        if (forwardResultIntent instanceof PathIntent) {
            PathIntent forwardPathIntent = (PathIntent) forwardResultIntent;
            assertThat(forwardPathIntent.path().links(), hasSize(9));
            assertThat(forwardPathIntent.path().links(), linksHasPath(HOST_ONE, "h1"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("h1", "h2"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("h2", "h3"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("h3", "h4"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("h4", "h5"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("h5", "h6"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("h6", "h7"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("h7", "h8"));
            assertThat(forwardPathIntent.path().links(), linksHasPath("h8", HOST_TWO));
        }

        if (reverseResultIntent instanceof PathIntent) {
            PathIntent reversePathIntent = (PathIntent) reverseResultIntent;
            assertThat(reversePathIntent.path().links(), hasSize(9));
            assertThat(reversePathIntent.path().links(), linksHasPath("h1", HOST_ONE));
            assertThat(reversePathIntent.path().links(), linksHasPath("h2", "h1"));
            assertThat(reversePathIntent.path().links(), linksHasPath("h3", "h2"));
            assertThat(reversePathIntent.path().links(), linksHasPath("h4", "h3"));
            assertThat(reversePathIntent.path().links(), linksHasPath("h5", "h4"));
            assertThat(reversePathIntent.path().links(), linksHasPath("h6", "h5"));
            assertThat(reversePathIntent.path().links(), linksHasPath("h7", "h6"));
            assertThat(reversePathIntent.path().links(), linksHasPath("h8", "h7"));
            assertThat(reversePathIntent.path().links(), linksHasPath(HOST_TWO, "h8"));
        }
    }
}
