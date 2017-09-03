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

package org.onosproject.drivers.fujitsu;

import org.apache.commons.io.IOUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.behaviour.ControllerInfo;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.netconf.DatastoreId;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.nio.charset.StandardCharsets;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;


/**
 * Unit tests for methods of FujitsuVoltControllerConfig.
 */
public class FujitsuVoltControllerConfigTest {

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListenerTest();

    private static final String TEST_VOLT_OFCONFIG = "volt-ofconfig";
    private static final String TEST_OFCONFIG_ID = "ofconfig-id";
    private static final String TEST_END_LICENSE_HEADER = "-->";

    private static final String[] GET_CONTROLLERS = {
            "tcp:172.10.10.45:6633",
            "tcp:100.0.0.22:5555",
    };
    private static final String[] SET_CONTROLLERS = {
            "tcp:172.10.10.55:2222",
            "tcp:172.20.33.11:6633",
    };
    private static final String GET_CONTROLLERS_RSP_FILE = "/getcontrollers.xml";
    private static final String SET_CONTROLLERS_REQ_FILE = "/setcontrollers.xml";

    private Integer currentKey;
    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltControllerConfig voltConfig;

    @Before
    public void setUp() throws Exception {
        controller = new FujitsuNetconfControllerMock();
        driverHandler = controller.setUp(listener);
        voltConfig = new FujitsuVoltControllerConfig();
        voltConfig.setHandler(driverHandler);
    }

    /**
     * Run to verify handling of valid get operation.
     */
    @Test
    public void testGetControllers() throws Exception {
        List<ControllerInfo> controllers;
        List<ControllerInfo> expectedControllers = new ArrayList<>();

        for (int i = ZERO; i < GET_CONTROLLERS.length; i++) {
            String target = GET_CONTROLLERS[i];
            String[] data = target.split(TEST_COLON);
            currentKey = i + ONE;

            Annotations annotations = DefaultAnnotations
                                          .builder()
                                          .set(TEST_OFCONFIG_ID, currentKey.toString())
                                          .build();
            ControllerInfo controller = new ControllerInfo(
                    IpAddress.valueOf(data[SECOND_PART]),
                    Integer.parseInt(data[THIRD_PART]),
                        data[FIRST_PART], annotations);
            expectedControllers.add(controller);
        }

        controllers = voltConfig.getControllers();
        assertTrue("Incorrect response", controllers.equals(expectedControllers));
    }

    /**
     * Run to verify handling of valid set operation.
     */
    @Test
    public void testSetControllers() throws Exception {
        List<ControllerInfo> controllers = new ArrayList<>();

        for (int i = ZERO; i < SET_CONTROLLERS.length; i++) {
            String target = SET_CONTROLLERS[i];
            String[] data = target.split(TEST_COLON);
            currentKey = i + ONE;

            Annotations annotations = DefaultAnnotations
                                          .builder()
                                          .set(TEST_OFCONFIG_ID, currentKey.toString())
                                          .build();
            ControllerInfo controller = new ControllerInfo(
                    IpAddress.valueOf(data[SECOND_PART]),
                    Integer.parseInt(data[THIRD_PART]),
                        data[FIRST_PART], annotations);
            controllers.add(controller);
        }

        voltConfig.setControllers(controllers);
    }

    /**
     * Verifies XML request string by comparing with generated string.
     *
     * @param request XML string for set operation
     * @return true if XML string matches with generated
     */
    private boolean verifyGetRequest(String request) {
        StringBuilder rpc = new StringBuilder();
        rpc.append(TEST_VOLT_NE_OPEN + TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        rpc.append(emptyTag(TEST_VOLT_OFCONFIG))
            .append(TEST_VOLT_NE_CLOSE);

        String testRequest = rpc.toString();
        boolean result = request.equals(testRequest);
        assertTrue("Does not match with generated string", result);
        return result;
    }

    /**
     * Verifies XML request string by comparing with generated string.
     *
     * @param request XML string for set operation
     * @return true if XML string matches with generated
     */
    private boolean verifyEditConfigRequest(String request) {
        String testRequest;

        try {
            InputStream fileStream = getClass().getResourceAsStream(
                        SET_CONTROLLERS_REQ_FILE);
            testRequest = IOUtils.toString(fileStream, StandardCharsets.UTF_8);
            testRequest = testRequest.substring(testRequest.indexOf(
                    TEST_END_LICENSE_HEADER) + TEST_END_LICENSE_HEADER.length());
        } catch (IOException e) {
            fail("IOException while reading: " + SET_CONTROLLERS_REQ_FILE);
            return false;
        }

        testRequest = testRequest.replaceAll(TEST_WHITESPACES_REGEX, TEST_EMPTY_STRING);
        request = request.replaceAll(TEST_WHITESPACES_REGEX, TEST_EMPTY_STRING);
        boolean result = request.equals(testRequest);
        assertTrue("Does not match with string in " + SET_CONTROLLERS_REQ_FILE, result);

        return result;
    }

    /**
     * Internal listener for device service events.
     */
    private class InternalSessionListenerTest implements FujitsuNetconfSessionListenerTest {
        @Override
        public boolean verifyEditConfig(String request) {
            boolean result;

            request = request.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_VOLT_NAMESPACE,
                    request.contains(TEST_VOLT_NAMESPACE));

            result = verifyEditConfigRequest(request);
            assertTrue("XML verification failure", result);
            return result;
        }

        @Override
        public boolean verifyEditConfig(DatastoreId target, String mode, String request) {
            return false;
        }

        @Override
        public boolean verifyEditConfig(String target, String mode, String request) {
            return false;
        }

        @Override
        public boolean verifyGet(String filterSchema, String withDefaultsMode) {
            boolean result;

            assertTrue("Incorrect withDefaultsMode",
                    withDefaultsMode.equals(TEST_REPORT_ALL));
            filterSchema = filterSchema.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_VOLT_NAMESPACE,
                    filterSchema.contains(TEST_VOLT_NAMESPACE));

            result = verifyGetRequest(filterSchema);
            assertTrue("XML verification failure", result);
            return result;
        }

        @Override
        public String buildGetReply() {
            try {
                InputStream fileStream = getClass().getResourceAsStream(
                        GET_CONTROLLERS_RSP_FILE);
                String reply = IOUtils.toString(fileStream, StandardCharsets.UTF_8);
                return (reply);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public boolean verifyWrappedRpc(String request) {
            return false;
        }

        @Override
        public void verifyStartSubscription(String filterSchema) {
        }
    }

}
