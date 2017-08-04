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

package org.onosproject.drivers.cisco.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Tests Cisco NX-API request message generator for Cisco NXOS devices.
 */
public class NxApiRequestTest {
    public static final String REQ_FILE1 = "/testNxApiRequest1.json";
    public static final String REQ_FILE2 = "/testNxApiRequest2.json";

    @Test
    public void oneRequestTest() throws IOException {
        InputStream streamOrig = getClass().getResourceAsStream(REQ_FILE1);
        ObjectMapper om = new ObjectMapper();
        JsonNode node1 = om.readTree(streamOrig);

        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("show interface");

        JsonNode node2 = om.readTree(NxApiRequest.generate(cmds, NxApiRequest.CommandType.CLI));

        assertEquals(node1.toString(), node2.toString());
    }

    @Test
    public void manyRequestsTest() throws IOException {
        InputStream streamOrig = getClass().getResourceAsStream(REQ_FILE2);
        ObjectMapper om = new ObjectMapper();
        JsonNode node1 = om.readTree(streamOrig);

        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("show interface");
        cmds.add("show ver");

        JsonNode node2 = om.readTree(NxApiRequest.generate(cmds, NxApiRequest.CommandType.CLI));

        assertEquals(node1.toString(), node2.toString());
    }
}
