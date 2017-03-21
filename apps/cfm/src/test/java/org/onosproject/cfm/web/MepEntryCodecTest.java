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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.MepDirection;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry.MepEntryBuilder;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test that the MepEntryCodec can successfully parse Json in to a Mep.
 */
public class MepEntryCodecTest {
    ObjectMapper mapper;
    CfmCodecContext context;
    MepEntry mepEntry1;

    @Before
    public void setUp() throws Exception, CfmConfigException {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
        MepEntryBuilder builder = DefaultMepEntry.builder(
                MepId.valueOf((short) 22),
                DeviceId.deviceId("netconf:1234:830"),
                PortNumber.portNumber(2),
                MepDirection.UP_MEP,
                MdIdCharStr.asMdId("md-1"),
                MaIdCharStr.asMaId("ma-1-1"))
            .macAddress(MacAddress.valueOf("aa:bb:cc:dd:ee:ff"));
        builder = (MepEntryBuilder) builder
                .administrativeState(true)
                .cciEnabled(true)
                .ccmLtmPriority(Priority.PRIO1);
        mepEntry1 = builder.buildEntry();

    }

    @Test
    public void testEncodeMepEntryCodecContext() {
        ObjectNode node = mapper.createObjectNode();
        node.set("mep", context.codec(MepEntry.class).encode(mepEntry1, context));

        assertEquals(22, node.get("mep").get("mepId").asInt());
        assertEquals("aa:bb:cc:dd:ee:ff".toUpperCase(),
                node.get("mep").get("macAddress").asText());
        assertTrue(node.get("mep").get("administrative-state").asBoolean());
        assertTrue(node.get("mep").get("cci-enabled").asBoolean());
        assertEquals(Priority.PRIO1.ordinal(),
                node.get("mep").get("ccm-ltm-priority").asInt());
    }

    @Test
    public void testEncodeIterableOfMepEntryCodecContext() throws CfmConfigException {
        MepEntry mepEntry2 = DefaultMepEntry.builder(
                MepId.valueOf((short) 33),
                DeviceId.deviceId("netconf:4321:830"),
                PortNumber.portNumber(1),
                MepDirection.DOWN_MEP,
                MdIdCharStr.asMdId("md-2"),
                MaIdCharStr.asMaId("ma-2-2"))
            .buildEntry();

        ArrayList<MepEntry> meps = new ArrayList<>();
        meps.add(mepEntry1);
        meps.add(mepEntry2);

        ObjectNode node = mapper.createObjectNode();
        node.set("mep", context.codec(MepEntry.class)
                .encode(meps, context));

        Iterator<JsonNode> an = node.get("mep").elements();
        while (an.hasNext()) {
            JsonNode jn = an.next();
            assertEquals("md-", jn.get("mdName").asText().substring(0, 3));
        }
    }

}
