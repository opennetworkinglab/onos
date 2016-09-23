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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;

/**
 * Unit tests for methods of FujitsuVoltPonLinkConfig.
 */
public class FujitsuVoltPonLinkConfigTest {

    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltPonLinkConfig voltConfig;

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListenerTest();

    private static final String TEST_VOLT_PORTS = "volt-ports";
    private static final String TEST_GPON_PONLINK_PORTS = "gpon-ponlink-ports";
    private static final String TEST_GPON_PONLINK_PORT = "gpon-ponlink-port";

    private static final Map<Integer, String> INVALID_GET_TCS = new HashMap<Integer, String>() {
        {
            put(1, "a-b-c");
            put(2, "--1");
            put(3, "s-1");
            put(4, "1-1");
            put(5, "1 A");
            put(6, "1*A");
        }
    };
    private static final Map<Integer, String> VALID_GET_TCS = new HashMap<Integer, String>() {
        {
            put(1, "1");
            put(2, null);
        }
    };
    private static final Map<Integer, String> INVALID_SET_TCS = new HashMap<Integer, String>() {
        {
            put(1, "-11:admin-state:enable");
            put(2, "1:admin-state:false");
            put(3, "2-1:onu-discovery-mode:manual");
            put(4, "2:onu-discovery-mode:abcdef");
            put(5, "3:a:onu-discovery-interval:8");
            put(6, "3:onu-discovery-interval:-1");
            put(7, "3:onu-discovery-interval:s1");
            put(8, "4:dba-cycle-time:41");
            put(9, "5*8:mac-age-time:30000");
            put(10, "8:mac-age-time:3699999");
            put(11, "1:lof-threshold:111");
            put(12, "2:los-threshold:22");
            put(13, "3:pm-enable:xyz");
            put(14, "3:abc-enable:xyz");
        }
    };
    private static final Map<Integer, String> VALID_SET_TCS = new HashMap<Integer, String>() {
        {
            put(1, "1:admin-state:disable");
            put(2, "2:onu-discovery-mode:manual");
            put(3, "3:onu-discovery-interval:8");
            put(4, "4:dba-cycle-time:8");
            put(5, "5:mac-age-time:33333");
            put(6, "6:lof-threshold:7");
            put(7, "7:los-threshold:5");
            put(8, "8:pm-enable:true");
        }
    };
    private Integer currentKey;


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

        for (Integer key : INVALID_GET_TCS.keySet()) {
            target = INVALID_GET_TCS.get(key);
            reply = voltConfig.getPonLinks(target);
            assertNull("Incorrect response for " + target, reply);
        }
    }

    /**
     * Run to verify handling of valid input for get operation.
     */
    @Test
    public void testValidGetPonLinks() throws Exception {
        String reply;
        String target;

        for (Integer key : VALID_GET_TCS.keySet()) {
            target = VALID_GET_TCS.get(key);
            currentKey = key;
            reply = voltConfig.getPonLinks(target);
            assertNotNull("Incorrect response for " + target, reply);
        }
    }

    /**
     * Run to verify handling of invalid input for set operation.
     */
    @Test
    public void testInvalidSetPonLinkInput() throws Exception {
        String target;
        boolean result;

        for (Integer key : INVALID_SET_TCS.keySet()) {
            target = INVALID_SET_TCS.get(key);
            result = voltConfig.setPonLink(target);
            assertFalse("Incorrect response for ", result);
        }
    }

    /**
     * Run to verify handling of valid input for set operation.
     */
    @Test
    public void testValidSetPonLink() throws Exception {
        String target;
        boolean result;

        for (Integer key : VALID_SET_TCS.keySet()) {
            target = VALID_SET_TCS.get(key);
            currentKey = key;
            result = voltConfig.setPonLink(target);
            assertTrue("Incorrect response for ", result);
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
        String target = VALID_GET_TCS.get(currentKey);

        rpc.append(TEST_VOLT_NE_OPEN).append(TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT).append(TEST_NEW_LINE);
        rpc.append(startTag(TEST_VOLT_PORTS));
        if (target != null) {
            rpc.append(startTag(TEST_GPON_PONLINK_PORTS));
            rpc.append(startTag(TEST_GPON_PONLINK_PORT));
            rpc.append(startTag(TEST_PONLINK_ID, false));
            rpc.append(target);
            rpc.append(endTag(TEST_PONLINK_ID));
            rpc.append(endTag(TEST_GPON_PONLINK_PORT));
            rpc.append(endTag(TEST_GPON_PONLINK_PORTS));
        } else {
            rpc.append(emptyTag(TEST_GPON_PONLINK_PORTS));
        }
        rpc.append(endTag(TEST_VOLT_PORTS));
        rpc.append(TEST_VOLT_NE_CLOSE);

        String testRequest = rpc.toString();
        testRequest = testRequest.replaceAll(TEST_WHITESPACES_REGEX, TEST_EMPTY_STRING);
        request = request.replaceAll(TEST_WHITESPACES_REGEX, TEST_EMPTY_STRING);
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
        String target = VALID_SET_TCS.get(currentKey);
        String[] data = target.split(TEST_COLON);

        rpc.append(TEST_VOLT_NE_OPEN).append(TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT).append(TEST_NEW_LINE);
        rpc.append(startTag(TEST_VOLT_PORTS));
        rpc.append(startTag(TEST_GPON_PONLINK_PORTS));
        rpc.append(startTag(TEST_GPON_PONLINK_PORT));
        rpc.append(startTag(TEST_PONLINK_ID, false));
        rpc.append(data[FIRST_PART]);
        rpc.append(endTag(TEST_PONLINK_ID));
        rpc.append(startTag(data[SECOND_PART], false));
        rpc.append(data[THIRD_PART]);
        rpc.append(endTag(data[SECOND_PART]));
        rpc.append(endTag(TEST_GPON_PONLINK_PORT));
        rpc.append(endTag(TEST_GPON_PONLINK_PORTS));
        rpc.append(endTag(TEST_VOLT_PORTS));
        rpc.append(TEST_VOLT_NE_CLOSE);

        String testRequest = rpc.toString();
        testRequest = testRequest.replaceAll(TEST_WHITESPACES_REGEX, TEST_EMPTY_STRING);
        request = request.replaceAll(TEST_WHITESPACES_REGEX, TEST_EMPTY_STRING);
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
