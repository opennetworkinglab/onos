/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cfm.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MepLbCreateCodecTest {
    ObjectMapper mapper;
    CfmCodecContext context;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
    }

    @Test
    public void testDecodeMepLbCreateMepId() throws JsonProcessingException, IOException {
        String loopbackString = "{\"loopback\": {    \"remoteMepId\": 20," +
                "\"numberMessages\": 10,    \"vlanDropEligible\": true," +
                "\"vlanPriority\": 6,    \"dataTlvHex\": \"0A:BB:CC\" }}";

        InputStream input = new ByteArrayInputStream(
                loopbackString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MepLbCreate mepLbCreate = context
                .codec(MepLbCreate.class).decode((ObjectNode) cfg, context);

        assertNull(mepLbCreate.remoteMepAddress());
        assertEquals(20, mepLbCreate.remoteMepId().id().shortValue());
        assertEquals(10, mepLbCreate.numberMessages().intValue());
        assertEquals(6, mepLbCreate.vlanPriority().ordinal());
        assertEquals(true, mepLbCreate.vlanDropEligible());
        assertEquals("0A:BB:CC".toLowerCase(), mepLbCreate.dataTlvHex());
    }

    @Test
    public void testDecodeMepLbCreateMepMac() throws JsonProcessingException, IOException {
        String loopbackString = "{\"loopback\": {    " +
                "\"remoteMepMac\": \"AA:BB:CC:DD:EE:FF\" }}";
        InputStream input = new ByteArrayInputStream(
                loopbackString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MepLbCreate mepLbCreate = context
                .codec(MepLbCreate.class).decode((ObjectNode) cfg, context);

        assertNull(mepLbCreate.remoteMepId());
        assertEquals("AA:BB:CC:DD:EE:FF", mepLbCreate.remoteMepAddress().toString());
        assertNull(mepLbCreate.dataTlvHex());
    }
}
