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

import org.junit.Before;
import org.junit.Test;
import org.onosproject.netconf.DatastoreId;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;

/**
 * Unit tests for methods of FujitsuVoltOnuOperConfig.
 */
public class FujitsuVoltOnuOperConfigTest {

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListener();

    private static final String TEST_ONU_REBOOT = "onu-reboot";
    private static final String TEST_ONU_ETHPORT_LOOPBACK = "onu-ethport-loopback";
    private static final String TEST_ETHPORT_ID = "ethport-id";
    private static final String TEST_LOOPBACK_MODE = "mode";

    private static final String TEST_ONU_REBOOT_WITH_NAMESPACE = TEST_ANGLE_LEFT +
            TEST_ONU_REBOOT + TEST_SPACE + TEST_VOLT_NE_NAMESPACE;
    private static final String TEST_ONU_ETHPORT_LOOPBACK_WITH_NAMESPACE =
            TEST_ANGLE_LEFT + TEST_ONU_ETHPORT_LOOPBACK + TEST_SPACE +
            TEST_VOLT_NE_NAMESPACE;

    private static final String[] INVALID_REBOOT_TCS = {
        "xy1-b",
        "--1",
        "s-1",
        "16-1-1",
        "&AA-1",
        "-1-1",
    };
    private static final String[] VALID_REBOOT_TCS = {
        "1-2",
        "16-11",
    };
    private static final String[] INVALID_ETHPORT_LOOPBACK_TCS = {
        "-11-3--11",
        "1-CCa",
        "abc-1",
        "^1-1-3",
        "1-2:23-1",
        "1:33:2",
        "2-2-2:false",
    };
    private static final String[] VALID_ETHPORT_LOOPBACK_TCS = {
        "8-1-1",
        "1-11-3:release",
        "2-2-2:operate",
    };
    private Integer currentKey;
    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltOnuOperConfig voltConfig;

    @Before
    public void setUp() throws Exception {
        controller = new FujitsuNetconfControllerMock();
        driverHandler = controller.setUp(listener);
        voltConfig = new FujitsuVoltOnuOperConfig();
        voltConfig.setHandler(driverHandler);
    }

    /**
     * Run to verify handling of invalid input for rpc operation.
     */
    @Test
    public void testInvalidRebootOnuInput() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < INVALID_REBOOT_TCS.length; i++) {
            target = INVALID_REBOOT_TCS[i];
            reply = voltConfig.rebootOnu(target);
            assertNull("Incorrect response for INVALID_REBOOT_TCS", reply);
        }
    }

    /**
     * Run to verify handling of valid input for rpc operation.
     */
    @Test
    public void testValidRebootOnu() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < VALID_REBOOT_TCS.length; i++) {
            target = VALID_REBOOT_TCS[i];
            currentKey = i;
            reply = voltConfig.rebootOnu(target);
            assertNotNull("Incorrect response for VALID_REBOOT_TCS", reply);
        }
    }

    /**
     * Run to verify handling of invalid input for rpc operation.
     */
    @Test
    public void testInvalidEthLoopbackOnuInput() throws Exception {
        String target;
        String reply;

        for (int i = ZERO; i < INVALID_ETHPORT_LOOPBACK_TCS.length; i++) {
            target = INVALID_ETHPORT_LOOPBACK_TCS[i];
            reply = voltConfig.loopbackEthOnu(target);
            assertNull("Incorrect response for INVALID_ETHPORT_LOOPBACK_TCS", reply);
        }
    }

    /**
     * Run to verify handling of valid input for rpc operation.
     */
    @Test
    public void testValidLoopbackEthOnu() throws Exception {
        String target;
        String reply;

        for (int i = ZERO; i < VALID_ETHPORT_LOOPBACK_TCS.length; i++) {
            target = VALID_ETHPORT_LOOPBACK_TCS[i];
            currentKey = i;
            reply = voltConfig.loopbackEthOnu(target);
            assertNotNull("Incorrect response for VALID_ETHPORT_LOOPBACK_TCS", reply);
        }
    }

    /**
     * Verifies XML request string by comparing with generated string.
     *
     * @param request XML string for rpc operation
     * @return true or false
     */
    private boolean verifyWrappedRpcRequestForReboot(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_REBOOT_TCS[currentKey];
        String[] data = target.split(TEST_COLON);
        String[] onuId = data[FIRST_PART].split(TEST_HYPHEN);

        rpc.append(TEST_ANGLE_LEFT + TEST_ONU_REBOOT + TEST_SPACE);
        rpc.append(TEST_VOLT_NE_NAMESPACE + TEST_ANGLE_RIGHT + TEST_NEW_LINE);

        rpc.append(startTag(TEST_PONLINK_ID, false))
            .append(onuId[FIRST_PART])
            .append(endTag(TEST_PONLINK_ID))
            .append(startTag(TEST_ONU_ID, false))
            .append(onuId[SECOND_PART])
            .append(endTag(TEST_ONU_ID))
            .append(endTag(TEST_ONU_REBOOT));

        String testRequest = rpc.toString();
        boolean result = request.equals(testRequest);
        assertTrue("Does not match with generated string", result);
        return result;
    }

    /**
     * Verifies XML request string by comparing with generated string.
     *
     * @param request XML string for rpc operation
     * @return true or false
     */
    private boolean verifyWrappedRpcRequestForEthLoopback(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_ETHPORT_LOOPBACK_TCS[currentKey];
        String[] data = target.split(TEST_COLON);
        String[] ethId = data[FIRST_PART].split(TEST_HYPHEN);

        rpc.append(TEST_ANGLE_LEFT + TEST_ONU_ETHPORT_LOOPBACK + TEST_SPACE);
        rpc.append(TEST_VOLT_NE_NAMESPACE + TEST_ANGLE_RIGHT + TEST_NEW_LINE);

        rpc.append(startTag(TEST_PONLINK_ID, false))
            .append(ethId[FIRST_PART])
            .append(endTag(TEST_PONLINK_ID))
            .append(startTag(TEST_ONU_ID, false))
            .append(ethId[SECOND_PART])
            .append(endTag(TEST_ONU_ID))
            .append(startTag(TEST_ETHPORT_ID, false))
            .append(ethId[THIRD_PART])
            .append(endTag(TEST_ETHPORT_ID));
        if (data.length > SECOND_PART) {
            rpc.append(startTag(TEST_LOOPBACK_MODE, false))
                .append(data[SECOND_PART])
                .append(endTag(TEST_LOOPBACK_MODE));
        }
        rpc.append(endTag(TEST_ONU_ETHPORT_LOOPBACK));

        String testRequest = rpc.toString();
        boolean result = request.equals(testRequest);
        assertTrue("Does not match with generated string", result);
        return result;
    }

    /**
     * Internal listener for device service events.
     */
    private class InternalSessionListener implements FujitsuNetconfSessionListenerTest {
        @Override
        public boolean verifyEditConfig(String request) {
            return false;
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
            return false;
        }

        @Override
        public String buildGetReply() {
            return null;
        }

        @Override
        public boolean verifyWrappedRpc(String request) {
            boolean result;
            boolean reboot = false;

            if (request.contains(TEST_ONU_REBOOT)) {
                request = request.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
                assertTrue("Does not contain:" + TEST_ONU_REBOOT_WITH_NAMESPACE,
                        request.contains(TEST_ONU_REBOOT_WITH_NAMESPACE));
                reboot = true;
            } else {
                request = request.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
                assertTrue("Does not contain:" + TEST_ONU_ETHPORT_LOOPBACK_WITH_NAMESPACE,
                        request.contains(TEST_ONU_ETHPORT_LOOPBACK_WITH_NAMESPACE));
            }

            if (reboot) {
                result = verifyWrappedRpcRequestForReboot(request);
            } else {
                result = verifyWrappedRpcRequestForEthLoopback(request);
            }

            assertTrue("XML verification failure", result);
            return result;
        }

        @Override
        public void verifyStartSubscription(String filterSchema) {
        }
    }

}
