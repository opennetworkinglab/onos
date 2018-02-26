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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MepLtCreateCodecTest {
    ObjectMapper mapper;
    CfmCodecContext context;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
    }

    @Test
    public void testDecodeMepLtCreateMepId() throws JsonProcessingException, IOException {
        String linktraceString = "{\"linktrace\": {    " +
                "\"remoteMepId\": 20," +
                "\"defaultTtl\": 21," +
                "\"transmitLtmFlags\": \"use-fdb-only\"}}";

        InputStream input = new ByteArrayInputStream(
                linktraceString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MepLtCreate mepLtCreate = context
                .codec(MepLtCreate.class).decode((ObjectNode) cfg, context);

        assertNull(mepLtCreate.remoteMepAddress());
        assertEquals(20, mepLtCreate.remoteMepId().id().shortValue());
        assertEquals(21, mepLtCreate.defaultTtl().intValue());
        assertEquals(BitSet.valueOf(new byte[]{1}), mepLtCreate.transmitLtmFlags());
    }

    @Test
    public void testDecodeMepLtCreateInvalidTransmitLtmFlags()
            throws JsonProcessingException, IOException {
        String linktraceString = "{\"linktrace\": {    " +
                "\"remoteMepId\": 20," +
                "\"transmitLtmFlags\": \"1\"}}";

        InputStream input = new ByteArrayInputStream(
                linktraceString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        try {
            context.codec(MepLtCreate.class).decode((ObjectNode) cfg, context);
        } catch (IllegalArgumentException e) {
            assertEquals("Expecting value 'use-fdb-only' or '' " +
                    "for transmitLtmFlags", e.getMessage());
        }
    }

    @Test
    public void testEncodeMepLtCreate() {
        MepId mepId1 = MepId.valueOf((short) 1);
        MepLtCreate mepLtCreate1 = DefaultMepLtCreate
                .builder(mepId1)
                .defaultTtl((short) 20)
                .transmitLtmFlags(BitSet.valueOf(new byte[]{1}))
                .build();

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("linktrace", context.codec(MepLtCreate.class).encode(mepLtCreate1, context));

        assertEquals("{\"linktrace\":{" +
                "\"remoteMepId\":1," +
                "\"defaultTtl\":20," +
                "\"transmitLtmFlags\":\"use-fdb-only\"}}", node.toString());
    }
}
