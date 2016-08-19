/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.controller.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.ospf.controller.OspfProcess;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfJsonParsingUtilTest.
 */
public class OspfConfigUtilTest {
    private ObjectMapper mapper;
    private JsonNode jsonNode;
    private List<OspfProcess> ospfProcessList = new ArrayList<>();
    private String jsonString = "{\n" +
            "\t\"processes\": {\n" +
            "\t\t\"areas\": [{\n" +
            "\t\t\t\"interface\": [{\n" +
            "\t\t\t\t\"interfaceIndex\": \"2\",\n" +
            "\n" +
            "\t\t\t\t\"helloIntervalTime\": \"10\",\n" +
            "\n" +
            "\t\t\t\t\"routerDeadIntervalTime\": \"40\",\n" +
            "\n" +
            "\t\t\t\t\"interfaceType\": \"2\",\n" +
            "\n" +
            "\t\t\t\t\"reTransmitInterval\": \"5\"\n" +
            "\t\t\t}],\n" +
            "\t\t\t\"areaId\": \"5.5.5.5\",\n" +
            "\n" +
            "\t\t\t\"routerId\": \"7.7.7.7\",\n" +
            "\n" +
            "\t\t\t\"isOpaqueEnable\": \"false\",\n" +
            "\n" +
            "\t\t\t\"externalRoutingCapability\": \"true\"\n" +
            "\t\t}]\n" +
            "\t}\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        jsonNode = mapper.readTree(jsonString);
        mapper = new ObjectMapper();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    @Ignore
    // Disabling because it seems to have an external dependency that can cause
    // it to fail in some environments.
    public void testProcesses() throws Exception {
        jsonNode.path("areas");
        ospfProcessList = OspfConfigUtil.processes(jsonNode);
        assertThat(ospfProcessList, is(notNullValue()));
    }
}
