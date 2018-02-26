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
package org.onosproject.soam.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmCreateBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmType;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DmCreateCodecTest {
    ObjectMapper mapper;
    CfmCodecContext context;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
    }

    @Test
    public void testDecodeObjectNodeCodecContext1()
            throws JsonProcessingException, IOException {
        String moStr = "{\"dm\": {}}";

        InputStream input = new ByteArrayInputStream(
                moStr.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);

        try {
            context.codec(DelayMeasurementCreate.class)
                .decode((ObjectNode) cfg, context);
            fail("Expecting an exception");
        } catch (IllegalArgumentException e) {
            assertEquals("remoteMepId is required", e.getMessage());
        }
    }

    @Test
    public void testDecodeObjectNodeCodecContext2()
            throws JsonProcessingException, IOException {
        String moStr = "{\"dm\": {" +
            "\"version\":\"Y17312008\"," +
            "\"dmType\":\"DMDMM\"," +
            "\"remoteMepId\":12," +
            "\"priority\":\"PRIO6\"," +
            "\"measurementsEnabled\" :" +
            "[\"FRAME_DELAY_RANGE_BACKWARD_AVERAGE\", " +
            "\"INTER_FRAME_DELAY_VARIATION_FORWARD_AVERAGE\"]" +
            "}}";

        InputStream input = new ByteArrayInputStream(
                moStr.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);

        DelayMeasurementCreate dmCreate = context
                .codec(DelayMeasurementCreate.class)
                .decode((ObjectNode) cfg, context);

        assertEquals(Version.Y17312008, dmCreate.version());
        assertEquals(DmType.DMDMM, dmCreate.dmCfgType());
        assertEquals(12, dmCreate.remoteMepId().id().shortValue());
    }

    @Test
    public void testEncodeDelayMeasurementCreateCodecContext()
            throws SoamConfigException {
        DmCreateBuilder builder = DefaultDelayMeasurementCreate
                .builder(DmType.DM1DMRX, Version.Y17312011,
                        MepId.valueOf((short) 16), Priority.PRIO5);
        builder.addToMeasurementsEnabled(
                MeasurementOption.FRAME_DELAY_BACKWARD_MAX);
        builder.addToMeasurementsEnabled(
                MeasurementOption.FRAME_DELAY_TWO_WAY_MAX);
        builder.addToMeasurementsEnabled(
                MeasurementOption.INTER_FRAME_DELAY_VARIATION_BACKWARD_BINS);
        builder = (DmCreateBuilder) builder.messagePeriod(Duration.ofMillis(100));
        builder = (DmCreateBuilder) builder.frameSize((short) 1200);

        ObjectNode node = mapper.createObjectNode();
        node.set("dm", context.codec(DelayMeasurementCreate.class)
                .encode(builder.build(), context));

        assertEquals(DmType.DM1DMRX.name(), node.get("dm").get("dmCfgType").asText());
        assertEquals(Version.Y17312011.name(), node.get("dm").get("version").asText());
        assertEquals(16, node.get("dm").get("remoteMepId").asInt());
        assertEquals(Priority.PRIO5.name(), node.get("dm").get("priority").asText());
        assertEquals(100, node.get("dm").get("messagePeriodMs").asInt());
        assertEquals(1200, node.get("dm").get("frameSize").asInt());

        assertEquals(3, ((ArrayNode) node.get("dm").get("measurementsEnabled")).size());
    }

}
