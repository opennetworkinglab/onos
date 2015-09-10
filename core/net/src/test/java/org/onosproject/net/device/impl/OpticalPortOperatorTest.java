/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.device.impl;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.basics.OpticalPortConfig;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OduCltPort;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.OduCltPortDescription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import static org.junit.Assert.assertEquals;

public class OpticalPortOperatorTest {
    private static final DeviceId DID = DeviceId.deviceId("op-test");
    private static final String TPNAME = "test-port-100";
    private static final String SPNAME = "out-port-200";
    private static final String CFGNAME = "cfg-name";

    private static final PortNumber NAMED = PortNumber.portNumber(100, TPNAME);
    private static final PortNumber UNNAMED = PortNumber.portNumber(101);
    private static final ConnectPoint NCP = new ConnectPoint(DID, UNNAMED);

    private static final SparseAnnotations SA = DefaultAnnotations.builder()
                                                    .set(AnnotationKeys.STATIC_PORT, SPNAME)
                                                    .build();

    private static final OduCltPortDescription N_DESC = new OduCltPortDescription(
            NAMED, true, OduCltPort.SignalType.CLT_100GBE, SA);
    private static final OduCltPortDescription FAULTY = new OduCltPortDescription(
            null, true, OduCltPort.SignalType.CLT_100GBE);

    private final ConfigApplyDelegate delegate = new MockCfgDelegate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final OpticalPortConfig N_OPC = new OpticalPortConfig();
    private static final OpticalPortConfig UNN_OPC = new OpticalPortConfig();

    @Before
    public void setUp() {
        N_OPC.init(NCP, TPNAME, JsonNodeFactory.instance.objectNode(), mapper, delegate);
        UNN_OPC.init(NCP, TPNAME, JsonNodeFactory.instance.objectNode(), mapper, delegate);

        N_OPC.portName(CFGNAME).portNumberName(101L).portType(Port.Type.ODUCLT).staticLambda(300L);
        UNN_OPC.portType(Port.Type.ODUCLT);
    }

    @Test(expected = RuntimeException.class)
    public void testDescOps() {
        // port-null desc + opc with port number name
        OduCltPortDescription res = (OduCltPortDescription) OpticalPortOperator.combine(N_OPC, FAULTY);
        assertEquals(CFGNAME, res.portNumber().name());
        // full desc + opc with name
        assertEquals(TPNAME, N_DESC.portNumber().name());
        res = (OduCltPortDescription) OpticalPortOperator.combine(N_OPC, N_DESC);
        long sl = Long.valueOf(res.annotations().value(AnnotationKeys.STATIC_LAMBDA));
        assertEquals(CFGNAME, res.portNumber().name());
        assertEquals(300L, sl);
        // port-null desc + opc without port number name - throws RE
        res = (OduCltPortDescription) OpticalPortOperator.combine(UNN_OPC, FAULTY);
    }

    private class MockCfgDelegate implements ConfigApplyDelegate {

        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }

    }
}
