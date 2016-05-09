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
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.controller.topology.IsisRouterListener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test case for DefaultIsisController.
 */
public class DefaultIsisControllerTest {
    private ObjectMapper mapper;
    private JsonNode jsonNode;
    private DefaultIsisController defaultIsisController;
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

    private IsisRouterListener isisRouterListener;

    @Before
    public void setUp() throws Exception {
        defaultIsisController = new DefaultIsisController();
        mapper = new ObjectMapper();
        jsonNode = mapper.readTree(jsonString);
        isisRouterListener = EasyMock.createNiceMock(IsisRouterListener.class);
    }

    @After
    public void tearDown() throws Exception {
        defaultIsisController = null;
    }

    /**
     * Tests activate() method.
     */
    @Test
    public void testActivate() throws Exception {
        defaultIsisController.activate();
        assertThat(defaultIsisController, is(notNullValue()));
    }

    /**
     * Tests deactivate() method.
     */
    @Test(expected = Exception.class)
    public void testDeactivate() throws Exception {
        defaultIsisController.activate();
        defaultIsisController.deactivate();
        assertThat(defaultIsisController, is(notNullValue()));
    }

    /**
     * Tests allConfiguredProcesses() method.
     */
    @Test
    public void testAllConfiguredProcesses() throws Exception {
        defaultIsisController.allConfiguredProcesses();
        assertThat(defaultIsisController, is(notNullValue()));
    }

    /**
     * Tests updateConfig() method.
     */
    @Test
    public void testUpdateConfig() throws Exception {
        defaultIsisController.updateConfig(jsonNode);
        assertThat(defaultIsisController, is(notNullValue()));
    }

    /**
     * Tests addRouterListener() method.
     */
    @Test
    public void testaddRouterListener() throws Exception {
        defaultIsisController.addRouterListener(isisRouterListener);
        assertThat(defaultIsisController, is(notNullValue()));
    }

    /**
     * Tests removeRouterListener() method.
     */
    @Test
    public void testremoveRouterListener() throws Exception {
        defaultIsisController.removeRouterListener(isisRouterListener);
        assertThat(defaultIsisController, is(notNullValue()));
    }
}
