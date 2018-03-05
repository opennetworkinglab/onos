/*
 * Copyright 2015-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.onosproject.net.intent.ConnectivityIntentTest.FP2;
import static org.onosproject.net.intent.ConnectivityIntentTest.FPS1;

/**
 * Suite of tests of the multi-to-single point intent's codec for rest api.
 */

public class MultiPointToSinglePointIntentCodecTest extends AbstractIntentTest {

    public static final ApplicationId APPID = new TestApplicationId("foo");
    public static final Key KEY = Key.of(1L, APPID);
    public static final TrafficSelector MATCH = DefaultTrafficSelector.emptySelector();
    public static final TrafficTreatment NOP = DefaultTrafficTreatment.emptyTreatment();
    public static final ConnectPoint P1 = new ConnectPoint(DeviceId.deviceId("111"), PortNumber.portNumber(0x1));
    public static final ConnectPoint P2 = new ConnectPoint(DeviceId.deviceId("222"), PortNumber.portNumber(0x2));
    public static final ConnectPoint P3 = new ConnectPoint(DeviceId.deviceId("333"), PortNumber.portNumber(0x3));

    public static final Set<ConnectPoint> PS1 = new HashSet<>(Arrays.asList(new ConnectPoint[]{P1, P3}));
    public static final Set<ConnectPoint> PS2 = new HashSet<>(Arrays.asList(new ConnectPoint[]{P2, P3}));

    private final MockCodecContext context = new MockCodecContext();
    final CoreService mockCoreService = createMock(CoreService.class);

    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";
    private static final String CP_POINTS = "connectPoints";

    @Before
    public void setUpIntentService() {
        final IntentService mockIntentService = new IntentServiceAdapter();
        context.registerService(IntentService.class, mockIntentService);
        context.registerService(CoreService.class, mockCoreService);
        expect(mockCoreService.getAppId(APPID.name()))
                .andReturn(APPID);
        replay(mockCoreService);
    }

    /**
     * Tests the encoding of a multi point to single point intent using Intent Codec.
     */
    @Test
    public void encodeMultiPointToSinglePointIntent() {

        final MultiPointToSinglePointIntent intent = MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .filteredIngressPoints(FPS1)
                .filteredEgressPoint(FP2)
                .build();

        assertThat(intent, notNullValue());
        assertThat(intent, instanceOf(MultiPointToSinglePointIntent.class));

        final JsonCodec<MultiPointToSinglePointIntent> intentCodec =
                context.codec(MultiPointToSinglePointIntent.class);
        assertThat(intentCodec, notNullValue());

        final ObjectNode result = intentCodec.encode(intent, context);

        assertThat(result.get("type").textValue(), is("MultiPointToSinglePointIntent"));
        assertThat(result.get("id").textValue(), is("0x0"));
        assertThat(result.get("appId").textValue(), is("foo"));
        assertThat(result.get("priority").asInt(), is(100));

        boolean found1 = false;
        boolean found3 = false;
        for (int i = 0; i < FPS1.size(); i++) {
            String portString = result.get("ingressPoint").get(i).get("port").textValue();
            if (portString.equals("1")) {
                found1 = true;
            } else if (portString.equals("3")) {
                found3 = true;
            }
        }
        assertThat("Port 1 was not found", found1, is(true));
        assertThat("Port 3 was not found", found3, is(true));

        assertThat(result.get("egressPoint").get("port").textValue(), is("2"));
        assertThat(result.get("egressPoint").get("device").textValue(), is("222"));
    }

    /**
     * Tests the multi point to single point intent decoding with JSON codec.
     *
     * @throws IOException if JSON processing fails
     */
    @Test
    public void decodeMultiPointToSinglePointIntent() throws IOException {

        final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode json = nodeFactory.objectNode();
        json.put("type", "MultiPointToSinglePointIntent");
        json.put("id", "0x0");
        json.put("appId", "foo");
        json.put("priority", 100);
        ArrayNode ingress = nodeFactory.arrayNode();
        ObjectNode ingressPoint = nodeFactory.objectNode();
        ingressPoint.put("port", "3");
        ingressPoint.put("device", "333");
        ingress.add(ingressPoint);
        ObjectNode ingressPoint2 = nodeFactory.objectNode();
        ingressPoint2.put("port", "1");
        ingressPoint2.put("device", "111");
        ingress.add(ingressPoint2);
        json.set("ingressPoint", ingress);
        ObjectNode egressPoint = nodeFactory.objectNode();
        egressPoint.put("port", "2");
        egressPoint.put("device", "222");
        json.set("egressPoint", egressPoint);
        assertThat(json, notNullValue());

        JsonCodec<MultiPointToSinglePointIntent> intentCodec = context.codec(MultiPointToSinglePointIntent.class);
        assertThat(intentCodec, notNullValue());

        final MultiPointToSinglePointIntent intent = intentCodec.decode(json, context);

        assertThat(intent.toString(), notNullValue());
        assertThat(intent, instanceOf(MultiPointToSinglePointIntent.class));
        assertThat(intent.priority(), is(100));
        assertThat(intent.ingressPoints().toString(), is("[333/3, 111/1]"));
        assertThat(intent.egressPoint().toString(), is("222/2"));

    }

}
