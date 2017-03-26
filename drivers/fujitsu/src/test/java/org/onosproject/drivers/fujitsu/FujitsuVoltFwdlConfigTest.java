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

package org.onosproject.drivers.fujitsu;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.netconf.TargetConfig;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;


/**
 * Unit tests for methods of FujitsuVoltFwdlConfig.
 */
public class FujitsuVoltFwdlConfigTest {

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListener();

    private static final String TEST_ONDEMAND_FIRMWARE_UPGRADE = "ondemand-firmware-upgrade";
    private static final String TEST_PARTICIPANT_LIST = "participant-list";
    private static final String TEST_MEMBER = "member";
    private static final String TEST_IMAGE_NAME = "image-name";
    private static final String TEST_REBOOT_MODE = "reboot-mode";
    private static final String TEST_COMMA = ",";

    private static final String TEST_ONDEMAND_FWDL_WITH_NAMESPACE =
            TEST_ANGLE_LEFT + TEST_ONDEMAND_FIRMWARE_UPGRADE +
            TEST_SPACE + TEST_VOLT_NE_NAMESPACE;

    private static final String[] INVALID_ONDEMAND_FWDL_TCS = {
        "xy1-b:a1-1",
        "AAA:1-2,--1",
        "CcC:s-1,2-2,3-2:auto",
        "xYZam:1-1,2-2,3-3:false",
        "JKml901:16-1-1,2-16:a-2",
        "abc:&AA-1,11-2:auto",
        "abc:xyz:-1-1",
        "@bcf11:xyz:auto",
        "FJ123:1-1&5-2:auto",
    };
    private static final String[] VALID_ONDEMAND_FWDL_TCS = {
        "Fujitsu123:1-2",
        "abcDE90f:16-11,1-1,17-3:auto",
        "fujitsuONU12:1-1,2-2,3-3,4-4,5-5,6-6,7-7",
    };
    private Integer currentKey;
    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltFwdlConfig voltConfig;

    @Before
    public void setUp() throws Exception {
        controller = new FujitsuNetconfControllerMock();
        driverHandler = controller.setUp(listener);
        voltConfig = new FujitsuVoltFwdlConfig();
        voltConfig.setHandler(driverHandler);
    }

    /**
     * Run to verify handling of invalid input for rpc operation.
     */
    @Test
    public void testInvalidOndemandFirmwareUpgradeInput() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < INVALID_ONDEMAND_FWDL_TCS.length; i++) {
            target = INVALID_ONDEMAND_FWDL_TCS[i];
            reply = voltConfig.upgradeFirmwareOndemand(target);
            assertNull("Incorrect response for INVALID_ONDEMAND_FWDL_TCS", reply);
        }
    }

    /**
     * Run to verify handling of valid input for rpc operation.
     */
    @Test
    public void testValidOndemandFirmwareUpgrade() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < VALID_ONDEMAND_FWDL_TCS.length; i++) {
            target = VALID_ONDEMAND_FWDL_TCS[i];
            currentKey = i;
            reply = voltConfig.upgradeFirmwareOndemand(target);
            assertNotNull("Incorrect response for VALID_ONDEMAND_FWDL_TCS", reply);
        }
    }

    /**
     * Verifies XML request string by comparing with generated string.
     *
     * @param request XML string for rpc operation
     * @return true or false
     */
    private boolean verifyWrappedRpcRequest(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_ONDEMAND_FWDL_TCS[currentKey];
        String[] data = target.split(TEST_COLON);
        String[] onuList = data[SECOND_PART].split(TEST_COMMA);
        int count;

        rpc.append(TEST_ANGLE_LEFT + TEST_ONDEMAND_FIRMWARE_UPGRADE + TEST_SPACE);
        rpc.append(TEST_VOLT_NE_NAMESPACE + TEST_ANGLE_RIGHT + TEST_NEW_LINE);

        rpc.append(startTag(TEST_PARTICIPANT_LIST));
        for (count = ZERO; count < onuList.length; count++) {
            String[] onuId = onuList[count].split(TEST_HYPHEN);
            rpc.append(startTag(TEST_MEMBER))
                .append(startTag(TEST_PONLINK_ID))
                .append(onuId[FIRST_PART])
                .append(endTag(TEST_PONLINK_ID))
                .append(startTag(TEST_ONU_ID))
                .append(onuId[SECOND_PART])
                .append(endTag(TEST_ONU_ID))
                .append(endTag(TEST_MEMBER));
        }
        rpc.append(endTag(TEST_PARTICIPANT_LIST))
            .append(startTag(TEST_IMAGE_NAME))
            .append(data[FIRST_PART])
            .append(endTag(TEST_IMAGE_NAME));
        if (data.length == THREE) {
            rpc.append(startTag(TEST_REBOOT_MODE))
                .append(data[THIRD_PART])
                .append(endTag(TEST_REBOOT_MODE));
        }
        rpc.append(endTag(TEST_ONDEMAND_FIRMWARE_UPGRADE));

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
        public boolean verifyEditConfig(String target, String mode, String request) {
            return false;
        }

        @Override
        public boolean verifyEditConfig(TargetConfig target, String mode, String request) {
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

            request = request.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_ONDEMAND_FWDL_WITH_NAMESPACE,
                    request.contains(TEST_ONDEMAND_FWDL_WITH_NAMESPACE));

            result = verifyWrappedRpcRequest(request);
            assertTrue("XML verification failure", result);
            return result;
        }

        @Override
        public void verifyStartSubscription(String filterSchema) {
        }
    }

}
