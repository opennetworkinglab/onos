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

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.PortDescription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.optical.device.OduCltPortHelper.oduCltPortDescription;

public class OpticalPortOperatorTest {
    private static final DeviceId DID = DeviceId.deviceId("op-test");
    private static final long PORT_NUMBER = 100;
    private static final String CFG_KEY = "optical";

    private static final String CFG_PORT_NAME = "cfg-name";
    private static final long CFG_STATIC_LAMBDA = 300L;

    private static final String DESC_PORT_NAME = "test-port-100";
    private static final PortNumber NAMED = PortNumber.portNumber(PORT_NUMBER, DESC_PORT_NAME);
    private static final PortNumber UNNAMED = PortNumber.portNumber(PORT_NUMBER);

    private static final String DESC_STATIC_PORT = "out-port-200";
    private static final SparseAnnotations SA = DefaultAnnotations.builder()
                                                    .set(AnnotationKeys.STATIC_PORT, DESC_STATIC_PORT)
                                                    .build();

    private static final PortDescription N_DESC = oduCltPortDescription(
            NAMED, true, CltSignalType.CLT_100GBE, SA);
    private static final PortDescription U_DESC = oduCltPortDescription(
            UNNAMED, true, CltSignalType.CLT_100GBE, SA);

    private final ConfigApplyDelegate delegate = new MockCfgDelegate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final ConnectPoint CP = new ConnectPoint(DID, UNNAMED);

    private OpticalPortConfig opc;

    private OpticalPortOperator oper;

    @Before
    public void setUp() {
        opc = new OpticalPortConfig();
        opc.init(CP, CFG_KEY, JsonNodeFactory.instance.objectNode(), mapper, delegate);

        oper = new OpticalPortOperator();
        oper.bindService(new MockNetworkConfigService());
    }

    @Test
    public void testConfigPortName() {
        opc.portType(Port.Type.ODUCLT)
            .portNumberName(PORT_NUMBER)
            .portName(CFG_PORT_NAME);

        PortDescription res;
        // full desc + opc with name
        res = oper.combine(CP, N_DESC);
        assertEquals("Configured port name expected",
                     CFG_PORT_NAME, res.portNumber().name());
        assertEquals(DESC_STATIC_PORT, res.annotations().value(AnnotationKeys.STATIC_PORT));

        res = oper.combine(CP, U_DESC);
        assertEquals("Configured port name expected",
                     CFG_PORT_NAME, res.portNumber().name());
        assertEquals(DESC_STATIC_PORT, res.annotations().value(AnnotationKeys.STATIC_PORT));
    }

    @Test
    public void testConfigAddStaticLambda() {
        opc.portType(Port.Type.ODUCLT)
            .portNumberName(PORT_NUMBER)
            .staticLambda(CFG_STATIC_LAMBDA);

        PortDescription res;
        res = oper.combine(CP, N_DESC);
        assertEquals("Original port name expected",
                     DESC_PORT_NAME, res.portNumber().name());
        assertEquals(DESC_STATIC_PORT, res.annotations().value(AnnotationKeys.STATIC_PORT));
        long sl = Long.valueOf(res.annotations().value(AnnotationKeys.STATIC_LAMBDA));
        assertEquals(CFG_STATIC_LAMBDA, sl);
    }

    @Test
    public void testEmptyConfig() {
        opc.portType(Port.Type.ODUCLT)
            .portNumberName(PORT_NUMBER);

        PortDescription res;
        res = oper.combine(CP, N_DESC);
        assertEquals("Configured port name expected",
                     DESC_PORT_NAME, res.portNumber().name());
        assertEquals(DESC_STATIC_PORT, res.annotations().value(AnnotationKeys.STATIC_PORT));
    }


    private class MockNetworkConfigService
            extends NetworkConfigServiceAdapter {

        @Override
        public <S, C extends Config<S>> C getConfig(S subject,
                                                    Class<C> configClass) {
            if (configClass == OpticalPortConfig.class) {
                return (C) opc;
            }
            return null;
        }
    }


    private class MockCfgDelegate implements ConfigApplyDelegate {

        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }

    }
}
