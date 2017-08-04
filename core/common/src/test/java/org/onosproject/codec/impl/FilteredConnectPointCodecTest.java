/*
 * Copyright 2016-present Open Networking Foundation
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link FilteredConnectPointCodec}.
 */
public class FilteredConnectPointCodecTest {

    private static final VlanId VID_56 = VlanId.vlanId((short) 56);
    private static final TrafficSelector VLAN_SELECTOR
        = DefaultTrafficSelector.builder().matchVlanId(VID_56).build();
    private static final PortNumber PN_42 = PortNumber.portNumber(42);
    private static final DeviceId DID = DeviceId.deviceId("test:device");
    private static final ConnectPoint CP = new ConnectPoint(DID, PN_42);

    private static final String JSON
        = "{"
            + "\"connectPoint\":{\"port\":\"42\",\"device\":\"test:device\"},"
            + "\"trafficSelector\":{\"criteria\":[{\"type\":\"VLAN_VID\",\"vlanId\":56}]}"
        + "}";

    private FilteredConnectPointCodec sut;
    private CodecContext context;

    @Before
    public void setUp() {
        context = new MockCodecContext();
        JsonCodec<FilteredConnectPoint> codec = context.codec(FilteredConnectPoint.class);
        assertThat(codec, instanceOf(FilteredConnectPointCodec.class));
        sut = (FilteredConnectPointCodec) codec;
    }


    @Test
    public void testNoInformationLoss() {
        FilteredConnectPoint original = new FilteredConnectPoint(CP, VLAN_SELECTOR);
        ObjectNode json = sut.encode(original, context);
        assertNotNull(json);

        FilteredConnectPoint decoded = sut.decode(json, context);
        assertThat(decoded, is(equalTo(original)));
    }

    @Test
    public void testJsonFormat() throws JsonProcessingException, IOException {
        FilteredConnectPoint original = new FilteredConnectPoint(CP, VLAN_SELECTOR);

        // Jackson configuration for ease of Numeric node comparison
        // - treat integral number node as long node
        context.mapper().enable(DeserializationFeature.USE_LONG_FOR_INTS);
        context.mapper().setNodeFactory(new JsonNodeFactory(false) {
            @Override
            public NumericNode numberNode(int v) {
                return super.numberNode((long) v);
            }
            @Override
            public NumericNode numberNode(short v) {
                return super.numberNode((long) v);
            }
        });

        ObjectNode json = sut.encode(original, context);
        JsonNode expected = context.mapper().readTree(JSON);

        assertEquals(expected, json);
    }

    @Test
    public void testEmptySelector() {
        FilteredConnectPoint original = new FilteredConnectPoint(CP);
        ObjectNode json = sut.encode(original, context);
        assertNotNull(json);

        FilteredConnectPoint decoded = sut.decode(json, context);
        assertThat(decoded, is(equalTo(original)));
    }

}
