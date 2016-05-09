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

package org.onosproject.isis.controller.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for ControllerTest.
 */
public class ControllerTest {

    private Controller controller;
    private ObjectMapper mapper;
    private JsonNode jsonNode;
    private JsonNode jsonNode1;
    private String jsonString = "{" +
            "                \"processes\": [{" +
            "                                \"processId\": \"4.4.4.4\"," +
            "                                \"interface\": [{" +
            "                                                \"interfaceIndex\": \"2\"," +
            "                                                \"macAddress\": \"08:00:27:b7:ab:bf\"," +
            "                                                \"interfaceIp\": \"192.168.56.101\"," +
            "                                                \"networkMask\": \"255.255.255.224\"," +
            "                                                \"intermediateSystemName\": \"ROUTERONE\"," +
            "                                                \"systemId\": \"2929.2929.2929\"," +
            "                                                \"lanId\": \"0000.0000.0000.00\"," +
            "                                                \"idLength\": \"0\"," +
            "                                                \"maxAreaAddresses\": \"3\"," +
            "                                                \"reservedPacketCircuitType\": \"1\"," +
            "                                                \"circuitId\": \"10\"," +
            "                                                \"networkType\": \"2\"," +
            "                                                \"areaAddress\": \"490000\"," +
            "                                                \"areaLength\": \"3\"," +
            "                                                \"lspId\": \"1313131313130000\"," +
            "                                                \"holdingTime\": \"50\"," +
            "                                                \"helloInterval\": \"10\"," +
            "                                                \"priority\": \"0\"" +
            "                                }]" +
            "                }]" +
            "}";
    private String jsonString1 = "{" +
            "                \"processes\": {" +
            "                                \"interface\": [{" +
            "                                                \"interfaceIndex\": \"2\"," +
            "                                                \"interfaceIp\": \"100.100.100.100\"," +
            "                                                \"macAddress\": \"08:00:27:b7:ab:bf\"," +
            "                                                \"networkMask\": \"255.255.255.224\"," +
            "                                                \"intermediateSystemName\": \"ROUTERONE\"," +
            "                                                \"systemId\": \"2929.2929.2929\"," +
            "                                                \"lanId\": \"0000.0000.0000.00\"," +
            "                                                \"idLength\": \"0\"," +
            "                                                \"maxAreaAddresses\": \"3\"," +
            "                                                \"reservedPacketCircuitType\": \"1\"," +
            "                                                \"circuitId\": \"10\"," +
            "                                                \"networkType\": \"2\"," +
            "                                                \"areaAddress\": \"490000\"," +
            "                                                \"areaLength\": \"3\"," +
            "                                                \"lspId\": \"1313131313130000\"," +
            "                                                \"holdingTime\": \"50\"," +
            "                                                \"helloInterval\": \"10\"," +
            "                                                \"priority\": \"0\"" +
            "                                }]" +
            "                }" +
            "}";

    @Before
    public void setUp() throws Exception {
        controller = new Controller();
        mapper = new ObjectMapper();
        jsonNode = mapper.readTree(jsonString);
        jsonNode1 = mapper.readTree(jsonString1);
    }

    @After
    public void tearDown() throws Exception {
        controller = null;
    }

    /**
     * Tests isisDeactivate() method.
     */
    @Test(expected = Exception.class)
    public void testIsisDeactivate() throws Exception {
        controller.isisDeactivate();
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests getAllConfiguredProcesses() method.
     */
    @Test
    public void testGetAllConfiguredProcesses() throws Exception {
        controller.getAllConfiguredProcesses();
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests updateConfig() method.
     */
    @Test
    public void testUpdateConfig() throws Exception {
        jsonNode.path("interface");
        controller.updateConfig(jsonNode);
        assertThat(controller, is(notNullValue()));

        controller.updateConfig(jsonNode1);
        assertThat(controller, is(notNullValue()));
    }

}