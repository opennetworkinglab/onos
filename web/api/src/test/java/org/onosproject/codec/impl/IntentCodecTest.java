/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.List;

import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LambdaConstraint;
import org.onosproject.net.resource.Bandwidth;
import org.onosproject.net.resource.Lambda;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import static org.onosproject.codec.impl.IntentJsonMatcher.matchesIntent;
import static org.onosproject.net.NetTestTools.hid;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for the host to host intent class codec.
 */
public class IntentCodecTest extends AbstractIntentTest {

    private final HostId id1 = hid("12:34:56:78:91:ab/1");
    private final HostId id2 = hid("12:34:56:78:92:ab/1");
    private final ApplicationId appId =
            new DefaultApplicationId((short) 3, "test");
    final TrafficSelector emptySelector =
            DefaultTrafficSelector.builder().build();
    final TrafficTreatment emptyTreatment =
            DefaultTrafficTreatment.builder().build();
    private final CodecContext context = new MockCodecContext();

    /**
     * Tests the encoding of a host to host intent.
     */
    @Test
    public void hostToHostIntent() {
        final HostToHostIntent intent =
                new HostToHostIntent(appId, id1, id2);

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
                new PointToPointIntent(appId, emptySelector,
                        emptyTreatment, ingress, egress);

        final CodecContext context = new MockCodecContext();
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
        final TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchIPProtocol((byte) 3)
                .matchMplsLabel(4)
                .matchOpticalSignalType((short) 5)
                .matchLambda((short) 6)
                .matchEthDst(MacAddress.BROADCAST)
                .matchIPDst(IpPrefix.valueOf("1.2.3.4/24"))
                .build();
        final TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setLambda((short) 33)
                .setMpls(44)
                .setOutput(PortNumber.CONTROLLER)
                .setEthDst(MacAddress.BROADCAST)
                .build();
        final Constraint constraint1 = new BandwidthConstraint(Bandwidth.valueOf(1.0));
        final Constraint constraint2 = new LambdaConstraint(Lambda.valueOf(3));
        final List<Constraint> constraints = ImmutableList.of(constraint1, constraint2);

        final PointToPointIntent intent =
                new PointToPointIntent(appId, selector, treatment,
                                       ingress, egress, constraints);

        final CodecContext context = new MockCodecContext();
        final JsonCodec<PointToPointIntent> intentCodec =
                context.codec(PointToPointIntent.class);
        assertThat(intentCodec, notNullValue());

        final ObjectNode intentJson = intentCodec.encode(intent, context);
        assertThat(intentJson, matchesIntent(intent));

    }
}
