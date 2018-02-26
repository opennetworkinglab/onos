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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmType;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry.DmEntryBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry.SessionStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DmEntryCodecTest {
    ObjectMapper mapper;
    CfmCodecContext context;
    DelayMeasurementEntry dmEntry1;

    @Before
    public void setUp() throws Exception, SoamConfigException {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
        DmEntryBuilder builder = DefaultDelayMeasurementEntry
                .builder(SoamId.valueOf(12), DmType.DM1DMTX,
                        Version.Y17312008, MepId.valueOf((short) 10), Priority.PRIO4);
        builder = builder.sessionStatus(SessionStatus.NOT_ACTIVE);
        builder = builder.frameDelayTwoWay(Duration.ofNanos(101 * 1000));
        builder = builder.frameDelayForward(Duration.ofNanos(102 * 1000));
        builder = builder.frameDelayBackward(Duration.ofNanos(103 * 1000));
        builder = builder.interFrameDelayVariationTwoWay(Duration.ofNanos(104 * 1000));
        builder = builder.interFrameDelayVariationForward(Duration.ofNanos(105 * 1000));
        builder = builder.interFrameDelayVariationBackward(Duration.ofNanos(106 * 1000));
        builder.addToMeasurementsEnabled(MeasurementOption.FRAME_DELAY_BACKWARD_MAX);
        builder.addToMeasurementsEnabled(MeasurementOption.FRAME_DELAY_TWO_WAY_MAX);
        builder.addToMeasurementsEnabled(MeasurementOption.INTER_FRAME_DELAY_VARIATION_BACKWARD_BINS);

        dmEntry1 = builder.build();
    }

    @Test
    public void testEncodeDelayMeasurementEntryCodecContext()
            throws JsonProcessingException, IOException {
        ObjectNode node = mapper.createObjectNode();
        node.set("dm", context.codec(DelayMeasurementEntry.class)
                .encode(dmEntry1, context));

        assertEquals(12, node.get("dm").get("dmId").asInt());
        assertEquals(DmType.DM1DMTX.name(), node.get("dm").get("dmCfgType").asText());
        assertEquals(Version.Y17312008.name(), node.get("dm").get("version").asText());
        assertEquals(10, node.get("dm").get("remoteMepId").asInt());
        assertEquals(3, ((ArrayNode) node.get("dm").get("measurementsEnabled")).size());

        assertEquals(SessionStatus.NOT_ACTIVE.name(),
                node.get("dm").get("sessionStatus").asText());
        assertEquals("PT0.000101S",
                node.get("dm").get("frameDelayTwoWay").asText());
        assertEquals("PT0.000102S",
                node.get("dm").get("frameDelayForward").asText());
        assertEquals("PT0.000103S",
                node.get("dm").get("frameDelayBackward").asText());
        assertEquals("PT0.000104S",
                node.get("dm").get("interFrameDelayVariationTwoWay").asText());
        assertEquals("PT0.000105S",
                node.get("dm").get("interFrameDelayVariationForward").asText());
        assertEquals("PT0.000106S",
                node.get("dm").get("interFrameDelayVariationBackward").asText());

    }

    @Test
    public void testEncodeIterableOfDelayMeasurementEntryCodecContext()
            throws SoamConfigException {
        DmEntryBuilder builder2 = DefaultDelayMeasurementEntry
                .builder(SoamId.valueOf(14), DmType.DM1DMRX,
                        Version.Y17312011, MepId.valueOf((short) 16), Priority.PRIO5);
        builder2.addToMeasurementsEnabled(MeasurementOption.FRAME_DELAY_BACKWARD_MIN);
        builder2.addToMeasurementsEnabled(MeasurementOption.FRAME_DELAY_TWO_WAY_MIN);
        builder2.addToMeasurementsEnabled(MeasurementOption.INTER_FRAME_DELAY_VARIATION_BACKWARD_MIN);

        Collection<DelayMeasurementEntry> dmEntries = new ArrayList<>();
        dmEntries.add(dmEntry1);
        dmEntries.add(builder2.build());
        ObjectNode node = mapper.createObjectNode();
        node.set("dm", context.codec(DelayMeasurementEntry.class)
                .encode(dmEntries, context));

        assertEquals(2, ((ArrayNode) node.get("dm")).size());
    }

}
