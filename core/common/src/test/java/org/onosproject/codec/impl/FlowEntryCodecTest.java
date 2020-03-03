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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.io.IOException;
import java.io.InputStream;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.codec.impl.JsonCodecUtils.assertJsonEncodable;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit test for FlowEntryCodec.
 */
public class FlowEntryCodecTest {

    private static final FlowEntry.FlowEntryState STATE;
    private static final long LIFE;
    private static final FlowEntry.FlowLiveType LIVE_TYPE;
    private static final long PACKETS;
    private static final long BYTES;
    private static final TrafficSelector SELECTOR;
    private static final TrafficTreatment TREATMENT;
    private static final FlowRule FLOW_RULE;
    private static final FlowEntry FLOW_ENTRY;

    private CoreService mockCoreService = createMock(CoreService.class);
    private MockCodecContext context = new MockCodecContext();
    private JsonCodec<FlowEntry> flowEntryCodec = context.codec(FlowEntry.class);

    private static final String JSON_FILE = "simple-flow-entry.json";

    static {
        // make sure these members have same values with the corresponding JSON fields
        STATE = FlowEntry.FlowEntryState.valueOf("ADDED");
        LIFE = 1000;
        LIVE_TYPE = FlowEntry.FlowLiveType.valueOf("UNKNOWN");
        PACKETS = 123;
        BYTES = 456;

        SELECTOR = DefaultTrafficSelector.builder()
                .matchEthType((short) (Integer.decode("0x800") & 0xFFFF))
                .build();
        TREATMENT = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.fromString("CONTROLLER"))
                .build();
        FLOW_RULE = DefaultFlowEntry.builder()
                .withCookie(1)
                // isPermanent & timeout
                .makeTemporary(1)
                .forDevice(DeviceId.deviceId("of:0000000000000001"))
                .forTable(Integer.valueOf("1"))
                .withPriority(Integer.valueOf("1"))
                .withSelector(SELECTOR)
                .withTreatment(TREATMENT)
                .build();
        FLOW_ENTRY = new DefaultFlowEntry(FLOW_RULE, STATE, LIFE, LIVE_TYPE, PACKETS, BYTES);
    }

    @Before
    public void setUp() {
        expect(mockCoreService.registerApplication(FlowRuleCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        expect(mockCoreService.registerApplication(APP_ID.name()))
                .andReturn(APP_ID).anyTimes();
        expect(mockCoreService.getAppId(anyShort())).andReturn(APP_ID).anyTimes();
        replay(mockCoreService);

        context.registerService(CoreService.class, mockCoreService);
    }

    @Test
    public void testCodec() {
        assertNotNull(flowEntryCodec);
        assertJsonEncodable(context, flowEntryCodec, FLOW_ENTRY);
    }

    @Test
    public void testDecode() throws IOException {
        InputStream jsonStream = FlowEntryCodec.class.getResourceAsStream(JSON_FILE);
        JsonNode jsonString = context.mapper().readTree(jsonStream);

        FlowEntry expected = flowEntryCodec.decode((ObjectNode) jsonString, context);
        assertEquals(expected, FLOW_ENTRY);
    }

    @Test
    public void testEncode() throws IOException {
        InputStream jsonStream = FlowEntryCodec.class.getResourceAsStream(JSON_FILE);
        ObjectNode jsonString = (ObjectNode) context.mapper().readTree(jsonStream);

        ObjectNode expected = flowEntryCodec.encode(FLOW_ENTRY, context);
        // only set by the internal FlowRule encoder, so should not appear in the JSON string
        expected.remove("id");
        expected.remove("appId");
        expected.remove("tableName");

        // only set by the FlowEntry encoder but not used for the decoder
        // so should not appear in the JSON, or a decoding error occurs
        expected.remove(FlowEntryCodec.GROUP_ID);
        expected.remove(FlowEntryCodec.LAST_SEEN);

        // assert equality of those values separately. see below
        assertEquals(expected.get(FlowEntryCodec.LIFE).asLong(), jsonString.get(FlowEntryCodec.LIFE).asLong());
        assertEquals(expected.get(FlowEntryCodec.PACKETS).asLong(), jsonString.get(FlowEntryCodec.PACKETS).asLong());
        assertEquals(expected.get(FlowEntryCodec.BYTES).asLong(), jsonString.get(FlowEntryCodec.BYTES).asLong());

        // if those numeric values are included in expected as a result of the encoding,
        // AssertionError occurs even though both expected and jsonString are semantically identical
        expected.remove(FlowEntryCodec.LIFE);
        expected.remove(FlowEntryCodec.PACKETS);
        expected.remove(FlowEntryCodec.BYTES);
        jsonString.remove(FlowEntryCodec.LIFE);
        jsonString.remove(FlowEntryCodec.PACKETS);
        jsonString.remove(FlowEntryCodec.BYTES);

        assertEquals(expected, jsonString);
    }

}
