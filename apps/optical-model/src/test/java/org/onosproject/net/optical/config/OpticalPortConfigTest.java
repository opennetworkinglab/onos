/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.optical.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.onosproject.net.optical.config.OpticalPortConfig.NAME;
import static org.onosproject.net.optical.config.OpticalPortConfig.PORT;
import static org.onosproject.net.optical.config.OpticalPortConfig.STATIC_LAMBDA;
import static org.onosproject.net.optical.config.OpticalPortConfig.STATIC_PORT;
import static org.onosproject.net.optical.config.OpticalPortConfig.TYPE;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

public class OpticalPortConfigTest {
    private static final String FIELD = "ports";
    private static final String KEY = "opc-test";

    private static final DeviceId DID = DeviceId.deviceId(KEY);
    private static final PortNumber PN = PortNumber.portNumber(100);
    private static final ConnectPoint CPT = new ConnectPoint(DID, PN);
    private static final String DEMOTREE = "{" +
            "\"ports\": [" +
            // config entity 0
                "{" +
                    "\"name\": \"1-10-E1_WPORT\"," +
                    "\"type\": \"OMS\"" +
                "}," +
            // config entity 1
                "{" +
                    "\"type\": \"OCH\"," +
                    "\"speed\": 0," +
                    "\"port\": 10" +
                "}," +
            // config entity 2
                "{" +
                    "\"name\": \"1-1-E1_LPORT\"," +
                    "\"type\": \"OCH\"," +
                    "\"annotations\": {" +
                        "\"staticLambda\": 1," +
                        "\"staticPort\": \"1-22-E1_WPORT\"" +
                    "}" +
                "}" +
            "]" +
            "}";

    private final ConfigApplyDelegate delegate = new MockCfgDelegate();
    private final ObjectMapper mapper = new ObjectMapper();

    // one OPC per port in DEMOTREE
    private List<OpticalPortConfig> opcl = Lists.newArrayList();
    // JsonNodes representing each port.
    private List<JsonNode> testNodes = Lists.newArrayList();

    @Before
    public void setUp() {
        try {
            JsonNode tree = new ObjectMapper().readTree(DEMOTREE);
            Iterator<JsonNode> pitr = tree.get(FIELD).elements();
            while (pitr.hasNext()) {
                // initialize a config entity, add to lists
                JsonNode jn = pitr.next();
                OpticalPortConfig opc = new OpticalPortConfig();
                ObjectNode node = JsonNodeFactory.instance.objectNode();
                opc.init(CPT, KEY, node, mapper, delegate);

                testNodes.add(jn);
                opcl.add(opc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBaseAttrs() {
        // configs 0 and 1 - port with and without alphanumeric names
        OpticalPortConfig op0 = opcl.get(0);
        OpticalPortConfig op1 = opcl.get(1);
        // config 2 - no name
        OpticalPortConfig op2 = opcl.get(2);
        JsonNode jn0 = testNodes.get(0);
        JsonNode jn1 = testNodes.get(1);

        op0.portType(Port.Type.valueOf(jn0.path(TYPE).asText()))
                .portName(jn0.path(NAME).asText());
        op1.portType(Port.Type.valueOf(jn1.path(TYPE).asText()))
                .portNumberName(jn1.path(PORT).asLong());

        assertEquals(Port.Type.OMS, op0.type());
        assertEquals(jn0.path(NAME).asText(), op0.name());
        assertEquals(jn1.path(PORT).asText(), op1.numberName());
        assertEquals("", op1.name());
        assertEquals("", op2.name());
    }

    @Test
    public void testAdditionalAttrs() {
        // config 1 has no annotations, 2 has predefined ones
        OpticalPortConfig op1 = opcl.get(1);
        OpticalPortConfig op2 = opcl.get(2);
        JsonNode jn2 = testNodes.get(2);
        Long sl = 1L;

        // see config entity 2 in DEMOTREE
        op2.staticLambda(jn2.path("annotations").path(STATIC_LAMBDA).asLong());
        op2.staticPort(jn2.path("annotations").path(STATIC_PORT).asText());

        assertEquals(sl, op2.staticLambda().get());
        assertFalse(op1.staticLambda().isPresent());
        assertEquals("1-22-E1_WPORT", op2.staticPort());
        assertEquals("", op1.staticPort());

        op2.staticLambda(null);
        assertFalse(op2.staticLambda().isPresent());
    }

    private class MockCfgDelegate implements ConfigApplyDelegate {

        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }

    }
}
