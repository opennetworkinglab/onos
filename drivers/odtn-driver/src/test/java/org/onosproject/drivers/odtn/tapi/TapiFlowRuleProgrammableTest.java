/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.tapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.provider.ProviderId;

import java.io.IOException;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

/**
 * Tests for the TAPI Flow Rule Programmable.
 */
public class TapiFlowRuleProgrammableTest {

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("rest:127.0.0.1:8080");
    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "test_app_id");

    private static final ProviderId PID = new ProviderId("rest", "foo");
    private static final DefaultAnnotations DEVICE_ANNOTATIONS =
            DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, "foo").build();
    private static final Device DEV =
            new DefaultDevice(PID, DEVICE_ID, Device.Type.OLS, "", "", "", "", null, DEVICE_ANNOTATIONS);

    private static final DefaultAnnotations PORT_ANNOTATIONS = DefaultAnnotations.builder()
            .set(TapiDeviceHelper.UUID, "76be95de-5769-4e5d-b65e-62cb6c39cf6b").build();

    private static final DefaultAnnotations PORT_ANNOTATIONS_2 = DefaultAnnotations.builder()
            .set(TapiDeviceHelper.UUID, "0923962e-b83f-4702-9b16-a1a0db0dc1f9").build();

    private static final PortNumber ONE = PortNumber.portNumber(1);
    private static final PortNumber TWO = PortNumber.portNumber(2);

    private static final Port PORT_IN = new DefaultPort(DEV, ONE, true, PORT_ANNOTATIONS);
    private static final Port PORT_OUT = new DefaultPort(DEV, TWO, true, PORT_ANNOTATIONS_2);


    private static final Lambda LAMBDA = new OchSignal(GridType.DWDM, ChannelSpacing.CHL_0GHZ, 1, 1);

    private static final TrafficSelector SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(ONE)
            .add(Criteria.matchLambda(LAMBDA))
            .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
            .build();

    private static final TrafficTreatment TREATMENT = DefaultTrafficTreatment.builder()
            .add(Instructions.modL0Lambda(LAMBDA))
            .setOutput(TWO)
            .build();

    private static final FlowRule FLOW_RULE = DefaultFlowRule.builder()
            .forDevice(DEVICE_ID)
            .withSelector(SELECTOR)
            .withTreatment(TREATMENT)
            .withPriority(1)
            .fromApp(APP_ID)
            .makePermanent()
            .build();

    private static final String CONNECTION_UUID = "30c3e74c-0e3c-40b3-9e26-221c56e995c2";
    private static final String END_POINT_1_UUID = "76be95de-5769-4e5d-b65e-62cb6c39cf6b";
    private static final String END_POINT_2_UUID = "0923962e-b83f-4702-9b16-a1a0db0dc1f9";

    private static final String CONNECTIVITY_REQUEST = "{\"tapi-connectivity:connectivity-service\":" +
            "[{\"uuid\":\"" + CONNECTION_UUID + "\",\"service-layer\":\"PHOTONIC_MEDIA\",\"service-type\"" +
            ":\"POINT_TO_POINT_CONNECTIVITY\",\"end-point\":[{\"local-id\":\"" + 1 + "\"," +
            "\"layer-protocol-name\":" + "\"PHOTONIC_MEDIA\",\"layer-protocol-qualifier\":" +
            "\"tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC\"," + "\"service-interface-point\":" +
            "{\"service-interface-point-uuid\":\"" + END_POINT_1_UUID + "\"}}," +
            "{\"local-id\":\"" + 2 + "\",\"layer-protocol-name\":\"PHOTONIC_MEDIA\"," +
            "\"layer-protocol-qualifier\":" + "\"tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC\"," +
            "\"service-interface-point\":{" + "\"service-interface-point-uuid\":\"" + END_POINT_2_UUID + "\"}}]}]}";

    private static final String GET_CONNECTIVITY_REQUEST_REPLY = "{\"tapi-connectivity:connectivity-context\": " +
            "{\"connectivity-service\" : [{\"uuid\" : \"" + CONNECTION_UUID + "\"}]}}}";

    private static final Set<String> CONNECTION_UUIDS = ImmutableSet.of(CONNECTION_UUID);

    private TapiFlowRuleProgrammable tapiFrp;

    @Before
    public void setUp() throws Exception {
        tapiFrp = new TapiFlowRuleProgrammable();
        DriverHandler mockHandler = new InternalDriverHandler();
        tapiFrp.setHandler(mockHandler);
    }

    @Test
    public void createConnRequest() {
        String output = tapiFrp.createConnectivityRequest(CONNECTION_UUID, FLOW_RULE).toString();
        System.out.println(output);
        assertEquals("Json to create network connectivity is wrong", CONNECTIVITY_REQUEST, output);
    }

    @Test
    public void parseConnReply() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonReply = mapper.readTree(GET_CONNECTIVITY_REQUEST_REPLY);
        Set<String> output = TapiDeviceHelper.parseTapiGetConnectivityRequest(jsonReply);
        assertEquals("Wrong Tapi UUIDS", CONNECTION_UUIDS, output);
    }

    private class InternalDriverHandler implements DriverHandler {

        @Override
        public Driver driver() {
            return null;
        }

        @Override
        public DriverData data() {
            return null;
        }

        @Override
        public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
            return null;
        }

        @Override
        public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
            return false;
        }

        @Override
        public <T> T get(Class<T> serviceClass) {
            if (serviceClass.equals(DeviceService.class)) {
                return (T) new InternalDeviceService();
            }
            return null;
        }
    }

    private class InternalDeviceService extends DeviceServiceAdapter {
        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            if (portNumber.equals(ONE)) {
                return PORT_IN;
            } else {
                return PORT_OUT;
            }
        }
    }
}