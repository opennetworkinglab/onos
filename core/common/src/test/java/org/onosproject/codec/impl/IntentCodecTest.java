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
package org.onosproject.codec.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.util.Bandwidth;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.HostId;
import org.onosproject.net.Lambda;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.AnnotationConstraint;
import org.onosproject.net.intent.constraint.AsymmetricPathConstraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.intent.constraint.ObstacleConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.codec.impl.IntentJsonMatcher.matchesIntent;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.hid;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;

/**
 * Unit tests for the host to host intent class codec.
 */
public class IntentCodecTest extends AbstractIntentTest {

    private final HostId id1 = hid("12:34:56:78:91:ab/1");
    private final HostId id2 = hid("12:34:56:78:92:ab/1");
    private final ApplicationId appId = new DefaultApplicationId(3, "test");
    final TrafficSelector emptySelector =
            DefaultTrafficSelector.emptySelector();
    final TrafficTreatment emptyTreatment =
            DefaultTrafficTreatment.emptyTreatment();
    private final MockCodecContext context = new MockCodecContext();
    final CoreService mockCoreService = createMock(CoreService.class);

    @Before
    public void setUpIntentService() {
        final IntentService mockIntentService = new IntentServiceAdapter();
        context.registerService(IntentService.class, mockIntentService);
        context.registerService(CoreService.class, mockCoreService);
        expect(mockCoreService.getAppId(appId.name()))
                .andReturn(appId);
        replay(mockCoreService);
    }

    /**
     * Tests the encoding of a host to host intent.
     */
    @Test
    public void hostToHostIntent() {
        final HostToHostIntent intent =
                HostToHostIntent.builder()
                        .appId(appId)
                        .one(id1)
                        .two(id2)
                        .build();

        final JsonCodec<HostToHostIntent> intentCodec =
                context.codec(HostToHostIntent.class);
        assertThat(intentCodec, notNullValue());

        final ObjectNode intentJson = intentCodec.encode(intent, context);
        assertThat(intentJson, matchesIntent(intent));
    }

    /**
     * Tests the encoding of a point to point intent.
     */
    @Test
    public void pointToPointIntent() {
        ConnectPoint ingress = NetTestTools.connectPoint("ingress", 1);
        ConnectPoint egress = NetTestTools.connectPoint("egress", 2);

        final PointToPointIntent intent =
                PointToPointIntent.builder()
                        .appId(appId)
                        .selector(emptySelector)
                        .treatment(emptyTreatment)
                        .ingressPoint(ingress)
                        .egressPoint(egress).build();

        final JsonCodec<PointToPointIntent> intentCodec =
                context.codec(PointToPointIntent.class);
        assertThat(intentCodec, notNullValue());

        final ObjectNode intentJson = intentCodec.encode(intent, context);
        assertThat(intentJson, matchesIntent(intent));
    }

    /**
     * Tests the encoding of an intent with treatment, selector and constraints
     * specified.
     */
    @Test
    public void intentWithTreatmentSelectorAndConstraints() {
        ConnectPoint ingress = NetTestTools.connectPoint("ingress", 1);
        ConnectPoint egress = NetTestTools.connectPoint("egress", 2);
        DeviceId did1 = did("device1");
        DeviceId did2 = did("device2");
        DeviceId did3 = did("device3");
        Lambda ochSignal = Lambda.ochSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, 4, 8);
        final TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchIPProtocol((byte) 3)
                .matchMplsLabel(MplsLabel.mplsLabel(4))
                .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                .add(Criteria.matchLambda(ochSignal))
                .matchEthDst(MacAddress.BROADCAST)
                .matchIPDst(IpPrefix.valueOf("1.2.3.4/24"))
                .build();
        final TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setMpls(MplsLabel.mplsLabel(44))
                .setOutput(PortNumber.CONTROLLER)
                .setEthDst(MacAddress.BROADCAST)
                .build();

        final List<Constraint> constraints =
                ImmutableList.of(
                        new BandwidthConstraint(Bandwidth.bps(1.0)),
                        new AnnotationConstraint("key", 33.0),
                        new AsymmetricPathConstraint(),
                        new LatencyConstraint(Duration.ofSeconds(2)),
                        new ObstacleConstraint(did1, did2),
                        new WaypointConstraint(did3));

        final PointToPointIntent intent =
                PointToPointIntent.builder()
                        .appId(appId)
                        .selector(selector)
                        .treatment(treatment)
                        .ingressPoint(ingress)
                        .egressPoint(egress)
                        .constraints(constraints)
                        .build();


        final JsonCodec<PointToPointIntent> intentCodec =
                context.codec(PointToPointIntent.class);
        assertThat(intentCodec, notNullValue());

        final ObjectNode intentJson = intentCodec.encode(intent, context);
        assertThat(intentJson, matchesIntent(intent));

    }

    /**
     * Reads in a rule from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded flow rule
     * @throws IOException if processing the resource fails
     */
    private Intent getIntent(String resourceName, JsonCodec intentCodec) throws IOException {
        InputStream jsonStream = IntentCodecTest.class
                .getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        Intent intent = (Intent) intentCodec.decode((ObjectNode) json, context);
        assertThat(intent, notNullValue());
        return intent;
    }

    /**
     * Tests the point to point intent JSON codec.
     *
     * @throws IOException if JSON processing fails
     */
    @Test
    public void decodePointToPointIntent() throws IOException {
        JsonCodec<Intent> intentCodec = context.codec(Intent.class);
        assertThat(intentCodec, notNullValue());

        Intent intent = getIntent("PointToPointIntent.json", intentCodec);
        assertThat(intent, notNullValue());
        assertThat(intent, instanceOf(PointToPointIntent.class));

        PointToPointIntent pointIntent = (PointToPointIntent) intent;
        assertThat(pointIntent.priority(), is(55));
        assertThat(pointIntent.ingressPoint().deviceId(), is(did("0000000000000001")));
        assertThat(pointIntent.ingressPoint().port(), is(PortNumber.portNumber(1)));
        assertThat(pointIntent.egressPoint().deviceId(), is(did("0000000000000007")));
        assertThat(pointIntent.egressPoint().port(), is(PortNumber.portNumber(2)));

        assertThat(pointIntent.constraints(), hasSize(1));

        assertThat(pointIntent.selector(), notNullValue());
        assertThat(pointIntent.selector().criteria(), hasSize(1));
        Criterion criterion1 = pointIntent.selector().criteria().iterator().next();
        assertThat(criterion1, instanceOf(EthCriterion.class));
        EthCriterion ethCriterion = (EthCriterion) criterion1;
        assertThat(ethCriterion.mac().toString(), is("11:22:33:44:55:66"));
        assertThat(ethCriterion.type().name(), is("ETH_DST"));

        assertThat(pointIntent.treatment(), notNullValue());
        assertThat(pointIntent.treatment().allInstructions(), hasSize(1));
        Instruction instruction1 = pointIntent.treatment().allInstructions().iterator().next();
        assertThat(instruction1, instanceOf(ModEtherInstruction.class));
        ModEtherInstruction ethInstruction = (ModEtherInstruction) instruction1;
        assertThat(ethInstruction.mac().toString(), is("22:33:44:55:66:77"));
        assertThat(ethInstruction.type().toString(), is("L2MODIFICATION"));
        assertThat(ethInstruction.subtype().toString(), is("ETH_SRC"));
    }

    /**
     * Tests the host to host intent JSON codec.
     *
     * @throws IOException
     */
    @Test
    public void decodeHostToHostIntent() throws IOException {
        JsonCodec<Intent> intentCodec = context.codec(Intent.class);
        assertThat(intentCodec, notNullValue());

        Intent intent = getIntent("HostToHostIntent.json", intentCodec);
        assertThat(intent, notNullValue());
        assertThat(intent, instanceOf(HostToHostIntent.class));

        HostToHostIntent hostIntent = (HostToHostIntent) intent;
        assertThat(hostIntent.priority(), is(7));
        assertThat(hostIntent.constraints(), hasSize(1));
    }
}
