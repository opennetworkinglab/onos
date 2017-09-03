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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;
import static org.onosproject.netconf.DatastoreId.RUNNING;

/**
 * Unit tests for methods of FujitsuVoltPonLinkConfig.
 */
public class FujitsuVoltPonLinkConfigTest {

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListenerTest();

    private static final String TEST_VOLT_PORTS = "volt-ports";
    private static final String TEST_GPON_PONLINK_PORTS = "gpon-ponlink-ports";
    private static final String TEST_GPON_PONLINK_PORT = "gpon-ponlink-port";

    private static final String[] INVALID_GET_TCS = {
        "a-b-c",
        "--1",
        "s-1",
        "1-1",
        "1 A",
        "1*A",
    };
    private static final String[] VALID_GET_TCS = {
        "1",
        null,
    };
    private static final String[] INVALID_SET_TCS = {
        "-11:admin-state:enable",
        "1:admin-state:false",
        "2-1:onu-discovery-mode:manual",
        "2:onu-discovery-mode:abcdef",
        "3:a:onu-discovery-interval:8",
        "3:onu-discovery-interval:-1",
        "3:onu-discovery-interval:s1",
        "4:dba-cycle-time:41",
        "5*8:mac-age-time:30000",
        "8:mac-age-time:3699999",
        "1:lof-threshold:111",
        "2:los-threshold:22",
        "3:pm-enable:xyz",
        "3:abc-enable:xyz",
    };
    private static final String[] VALID_SET_TCS = {
        "1:admin-state:disable",
        "2:onu-discovery-mode:manual",
        "3:onu-discovery-interval:8",
        "4:dba-cycle-time:8",
        "5:mac-age-time:33333",
        "6:lof-threshold:7",
        "7:los-threshold:5",
        "8:pm-enable:true",
    };
    private Integer currentKey;
    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltPonLinkConfig voltConfig;

    @Before
    public void setUp() throws Exception {
        controller = new FujitsuNetconfControllerMock();
        driverHandler = controller.setUp(listener);
        voltConfig = new FujitsuVoltPonLinkConfig();
        voltConfig.setHandler(driverHandler);
    }

    /**
     * Run to verify handling of invalid input for get operation.
     */
    @Test
    public void testInvalidGetPonLinksInput() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < INVALID_GET_TCS.length; i++) {
            target = INVALID_GET_TCS[i];
            reply = voltConfig.getPonLinks(target);
            assertNull("Incorrect response for INVALID_GET_TCS", reply);
        }
    }

    /**
     * Run to verify handling of valid input for get operation.
     */
    @Test
    public void testValidGetPonLinks() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < VALID_GET_TCS.length; i++) {
            target = VALID_GET_TCS[i];
            currentKey = i;
            reply = voltConfig.getPonLinks(target);
            assertNotNull("Incorrect response for VALID_GET_TCS", reply);
        }
    }

    /**
     * Run to verify handling of invalid input for set operation.
     */
    @Test
    public void testInvalidSetPonLinkInput() throws Exception {
        String target;
        boolean result;

        for (int i = ZERO; i < INVALID_SET_TCS.length; i++) {
            target = INVALID_SET_TCS[i];
            result = voltConfig.setPonLink(target);
            assertFalse("Incorrect response for INVALID_SET_TCS", result);
        }
    }

    /**
     * Run to verify handling of valid input for set operation.
     */
    @Test
    public void testValidSetPonLink() throws Exception {
        String target;
        boolean result;

        for (int i = ZERO; i < VALID_SET_TCS.length; i++) {
            target = VALID_SET_TCS[i];
            currentKey = i;
            result = voltConfig.setPonLink(target);
            assertTrue("Incorrect response for VALID_SET_TCS", result);
        }
    }

    /**
     * Verifies XML request string by comparing with generated string.
     *
     * @param request XML string for set operation
     * @return true if XML string matches with generated
     */
    private boolean verifyGetRequest(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_GET_TCS[currentKey];

        rpc.append(TEST_VOLT_NE_OPEN + TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        rpc.append(startTag(TEST_VOLT_PORTS));
        if (target != null) {
            rpc.append(startTag(TEST_GPON_PONLINK_PORTS))
                .append(startTag(TEST_GPON_PONLINK_PORT))
                .append(startTag(TEST_PONLINK_ID, false))
                .append(target)
                .append(endTag(TEST_PONLINK_ID))
                .append(endTag(TEST_GPON_PONLINK_PORT))
                .append(endTag(TEST_GPON_PONLINK_PORTS));
        } else {
            rpc.append(emptyTag(TEST_GPON_PONLINK_PORTS));
        }
        rpc.append(endTag(TEST_VOLT_PORTS))
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
     * @return true or false
     */
    private boolean verifyEditConfigRequest(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_SET_TCS[currentKey];
        String[] data = target.split(TEST_COLON);

        rpc.append(TEST_VOLT_NE_OPEN + TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        rpc.append(startTag(TEST_VOLT_PORTS))
            .append(startTag(TEST_GPON_PONLINK_PORTS))
            .append(startTag(TEST_GPON_PONLINK_PORT))
            .append(startTag(TEST_PONLINK_ID, false))
            .append(data[FIRST_PART])
            .append(endTag(TEST_PONLINK_ID))
            .append(startTag(data[SECOND_PART], false))
            .append(data[THIRD_PART])
            .append(endTag(data[SECOND_PART]))
            .append(endTag(TEST_GPON_PONLINK_PORT))
            .append(endTag(TEST_GPON_PONLINK_PORTS))
            .append(endTag(TEST_VOLT_PORTS))
            .append(TEST_VOLT_NE_CLOSE);

        String testRequest = rpc.toString();
        boolean result = request.equals(testRequest);
        assertTrue("Does not match with generated string", result);
        return result;
    }

    /**
     * Internal listener for device service events.
     */
    private class InternalSessionListenerTest implements FujitsuNetconfSessionListenerTest {
        @Override
        public boolean verifyEditConfig(String request) {
            return false;
        }

        @Override
        public boolean verifyEditConfig(DatastoreId target, String mode, String request) {
            boolean result;

            assertTrue("Incorrect target", target.equals(RUNNING));
            assertNull("Incorrect mode", mode);

            request = request.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_VOLT_NAMESPACE,
                    request.contains(TEST_VOLT_NAMESPACE));
            result = verifyEditConfigRequest(request);
            assertTrue("XML verification failure", result);
            return result;
        }

        @Override
        public boolean verifyEditConfig(String target, String mode, String request) {
            boolean result;

            assertTrue("Incorrect target", target.equals(TEST_RUNNING));
            assertNull("Incorrect mode", mode);

            request = request.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_VOLT_NAMESPACE,
                       request.contains(TEST_VOLT_NAMESPACE));
            result = verifyEditConfigRequest(request);
            assertTrue("XML verification failure", result);
            return result;
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
            return null;
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
