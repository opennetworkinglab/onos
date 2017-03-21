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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class DmMeasurementOptionCodecTest {
    ObjectMapper mapper;
    CfmCodecContext context;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
    }

    @Test
    public void testEncodeIterableOfMeasurementOptionCodecContext() {
        List<MeasurementOption> moList = new ArrayList<>();
        moList.add(MeasurementOption.FRAME_DELAY_BACKWARD_MAX);
        moList.add(MeasurementOption.FRAME_DELAY_FORWARD_BINS);

        ArrayNode an =
                context.codec(MeasurementOption.class).encode(moList, context);

        assertEquals(MeasurementOption.FRAME_DELAY_BACKWARD_MAX.toString(),
                an.get(0).asText());
        assertEquals(MeasurementOption.FRAME_DELAY_FORWARD_BINS.toString(),
                an.get(1).asText());
    }

    @Test
    public void testDecodeArrayNodeCodecContext()
            throws JsonProcessingException, IOException {
        String moStr = "{\"measurementsEnabled\": " +
                "[\"FRAME_DELAY_RANGE_BACKWARD_AVERAGE\", " +
                "\"INTER_FRAME_DELAY_VARIATION_FORWARD_AVERAGE\"]}";
        InputStream input = new ByteArrayInputStream(
                moStr.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        Iterable<MeasurementOption> moIter = context
                .codec(MeasurementOption.class)
                .decode((ArrayNode) cfg.get("measurementsEnabled"), context);

        Iterator<MeasurementOption> source = moIter.iterator();
        List<MeasurementOption> moList = new ArrayList<>();
        source.forEachRemaining(moList::add);

        assertEquals(MeasurementOption.FRAME_DELAY_RANGE_BACKWARD_AVERAGE.toString(),
                moList.get(0).name());
        assertEquals(MeasurementOption.INTER_FRAME_DELAY_VARIATION_FORWARD_AVERAGE.toString(),
                moList.get(1).name());
    }

}
