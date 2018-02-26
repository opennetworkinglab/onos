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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultRemoteMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.InterfaceStatusTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.PortStatusTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.RemoteMepState;
import org.onosproject.incubator.net.l2monitoring.cfm.SenderIdTlv.SenderIdTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RemoteMepEntryCodecTest {
    ObjectMapper mapper;
    CfmCodecContext context;
    RemoteMepEntry remoteMep1;

    @Before
    public void setUp() throws Exception, CfmConfigException {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
        remoteMep1 = DefaultRemoteMepEntry
                .builder(MepId.valueOf((short) 10), RemoteMepState.RMEP_OK)
                .failedOrOkTime(Duration.ofMillis(546546546L))
                .interfaceStatusTlvType(InterfaceStatusTlvType.IS_LOWERLAYERDOWN)
                .macAddress(MacAddress.IPV4_MULTICAST)
                .portStatusTlvType(PortStatusTlvType.PS_NO_STATUS_TLV)
                .rdi(true)
                .senderIdTlvType(SenderIdTlvType.SI_NETWORK_ADDRESS)
                .build();
    }

    @Test
    public void testEncodeRemoteMepEntryCodecContext() {
        ObjectNode node = mapper.createObjectNode();
        node.set("remoteMep", context.codec(RemoteMepEntry.class)
                .encode(remoteMep1, context));

        assertEquals(10, node.get("remoteMep").get("remoteMepId").asInt());
    }

    @Test
    public void testEncodeIterableOfRemoteMepEntryCodecContext()
            throws CfmConfigException {
        RemoteMepEntry remoteMep2 = DefaultRemoteMepEntry
                .builder(MepId.valueOf((short) 20), RemoteMepState.RMEP_IDLE)
                .build();

        ArrayList<RemoteMepEntry> remoteMeps = new ArrayList<>();
        remoteMeps.add(remoteMep1);
        remoteMeps.add(remoteMep2);

        ObjectNode node = mapper.createObjectNode();
        node.set("remoteMep", context.codec(RemoteMepEntry.class)
                .encode(remoteMeps, context));

        Iterator<JsonNode> an = node.get("remoteMep").elements();
        while (an.hasNext()) {
            JsonNode jn = an.next();
            assertEquals("RMEP_", jn.get("remoteMepState").asText().substring(0, 5));
        }
    }
}
