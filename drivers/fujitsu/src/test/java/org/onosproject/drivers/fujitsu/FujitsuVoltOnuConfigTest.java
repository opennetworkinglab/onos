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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;


/**
 * Unit tests for methods of FujitsuVoltOnuConfig.
 */
public class FujitsuVoltOnuConfigTest {

    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltOnuConfig voltConfig;

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListenerTest();

    private static final String TEST_VOLT_ONUS = "volt-onus";
    private static final String TEST_ONUS_PERLINK = "onus-perlink";
    private static final String TEST_ONUS_LIST = "onus-list";
    private static final String TEST_ONU_INFO = "onu-info";
    private static final String TEST_ONU_SET_CONFIG = "onu-set-config";
    private static final String TEST_CONFIG_INFO = "config-info";
    private static final String TEST_VOLT_STATISTICS = "volt-statistics";
    private static final String TEST_ONU_STATISTICS = "onu-statistics";
    private static final String TEST_ONU_ETH_STATS = "onu-eth-stats";
    private static final String TEST_ETH_STATS = "eth-stats";
    private static final String TEST_ONU_GEM_STATS = "onu-gem-stats";
    private static final String TEST_GEM_STATS = "gem-stats";

    private static final String TEST_ONU_SET_CONFIG_WITH_NAMESPACE =
            TEST_ANGLE_LEFT + TEST_ONU_SET_CONFIG + TEST_SPACE +
            TEST_VOLT_NE_NAMESPACE;

    private static final Map<Integer, String> INVALID_GET_TCS = new HashMap<Integer, String>() {
        {
            put(1, "a-b");
            put(2, "--1-2");
            put(3, "s-1");
            put(4, "16-1-1");
            put(5, "1 A-1");
            put(6, "1*A-1");
        }
    };
    private static final Map<Integer, String> VALID_GET_TCS = new HashMap<Integer, String>() {
        {
            put(1, "1");
            put(2, "1-2");
            put(3, null);
        }
    };
    private static final Map<Integer, String> INVALID_SET_TCS = new HashMap<Integer, String>() {
        {
            put(1, "-11-3:admin-state:enable");
            put(2, "1-2:admin-state:false");
            put(3, "1-2:pm-enable:123");
            put(4, "^1-2:pm-enable:false");
            put(5, "1-2:fec-enable:xyz");
            put(6, "1-2:security-enable:123abc");
            put(7, "2-3:password:-1&");
            put(8, "2:admin-state:disable");
        }
    };
    private static final Map<Integer, String> VALID_SET_TCS = new HashMap<Integer, String>() {
        {
            put(1, "1-11:admin-state:disable");
            put(2, "8-1:pm-enable:true");
            put(3, "1-1:fec-enable:true");
            put(4, "1-21:security-enable:false");
            put(5, "3-2:password:abc123");
        }
    };
    private static final Map<Integer, String> INVALID_GET_STATS_TCS = new HashMap<Integer, String>() {
        {
            put(1, "1-a");
            put(2, "1:1");
            put(3, "a-1");
            put(4, "1-1-1");
            put(5, "2 A-1");
            put(6, "2/A-1");
        }
    };
    private static final Map<Integer, String> VALID_GET_STATS_TCS = new HashMap<Integer, String>() {
        {
            put(1, "1");
            put(2, "3-12");
            put(3, null);
        }
    };
    private Integer currentKey;


    @Before
    public void setUp() throws Exception {
        controller = new FujitsuNetconfControllerMock();
        driverHandler = controller.setUp(listener);
        voltConfig = new FujitsuVoltOnuConfig();
        voltConfig.setHandler(driverHandler);
    }

    /**
     * Run to verify handling of invalid input for get operation.
     */
    @Test
    public void testInvalidGetOnusInput() throws Exception {
        String reply;
        String target;

        for (Integer key : INVALID_GET_TCS.keySet()) {
            target = INVALID_GET_TCS.get(key);
            reply = voltConfig.getOnus(target);
            assertNull("Incorrect response for " + target, reply);
        }
    }

    /**
     * Run to verify handling of valid input for get operation.
     */
    @Test
    public void testValidGetOnus() throws Exception {
        String reply;
        String target;

        for (Integer key : VALID_GET_TCS.keySet()) {
            target = VALID_GET_TCS.get(key);
            currentKey = key;
            reply = voltConfig.getOnus(target);
            assertNotNull("Incorrect response for " + target, reply);
        }
    }

    /**
     * Run to verify handling of invalid input for set operation.
     */
    @Test
    public void testInvalidSetOnuInput() throws Exception {
        String target;
        String reply;

        for (Integer key : INVALID_SET_TCS.keySet()) {
            target = INVALID_SET_TCS.get(key);
            reply = voltConfig.setOnu(target);
            assertNull("Incorrect response for " + target, reply);
        }
    }

    /**
     * Run to verify handling of valid input for set operation.
     */
    @Test
    public void testValidSetOnu() throws Exception {
        String target;
        String reply;

        for (Integer key : VALID_SET_TCS.keySet()) {
            target = VALID_SET_TCS.get(key);
            currentKey = key;
            reply = voltConfig.setOnu(target);
            assertNotNull("Incorrect response for " + target, reply);
        }
    }

    /**
     * Run to verify handling of invalid input for get statistics operation.
     */
    @Test
    public void testInvalidGetOnuStatsInput() throws Exception {
        String reply;
        String target;

        for (Integer key : INVALID_GET_STATS_TCS.keySet()) {
            target = INVALID_GET_STATS_TCS.get(key);
            reply = voltConfig.getOnuStatistics(target);
            assertNull("Incorrect response for " + target, reply);
        }
    }

    /**
     * Run to verify handling of valid input for get statistics operation.
     */
    @Test
    public void testValidGetOnuStats() throws Exception {
        String reply;
        String target;

        for (Integer key : VALID_GET_STATS_TCS.keySet()) {
            target = VALID_GET_STATS_TCS.get(key);
            currentKey = key;
            reply = voltConfig.getOnuStatistics(target);
            assertNotNull("Incorrect response for " + target, reply);
        }
    }

    /**
     * Verifies XML request string by comparing with generated string.
     *
     * @param request XML string for get operation
     * @return true if XML string matches with generated
     */
    private boolean verifyGetRequest(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_GET_TCS.get(currentKey);
        String[] onuId = null;

        if (target != null) {
            onuId = target.split(TEST_HYPHEN);
        }

        rpc.append(TEST_VOLT_NE_OPEN).append(TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT).append(TEST_NEW_LINE);
        if (onuId != null) {
            rpc.append(startTag(TEST_VOLT_ONUS));
            rpc.append(startTag(TEST_ONUS_PERLINK));
            rpc.append(startTag(TEST_PONLINK_ID, false));
            rpc.append(onuId[FIRST_PART]);
            rpc.append(endTag(TEST_PONLINK_ID));
            if (onuId.length > ONE) {
                rpc.append(startTag(TEST_ONUS_LIST));
                rpc.append(startTag(TEST_ONU_INFO));
                rpc.append(startTag(TEST_ONU_ID, false));
                rpc.append(onuId[SECOND_PART]);
                rpc.append(endTag(TEST_ONU_ID));
                rpc.append(endTag(TEST_ONU_INFO));
                rpc.append(endTag(TEST_ONUS_LIST));
            }
            rpc.append(endTag(TEST_ONUS_PERLINK));
            rpc.append(endTag(TEST_VOLT_ONUS));
        } else {
            rpc.append(emptyTag(TEST_VOLT_ONUS));
        }
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
    private boolean verifyWrappedRpcRequest(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_SET_TCS.get(currentKey);
        String[] data = target.split(TEST_COLON);
        String[] onuId = data[FIRST_PART].split(TEST_HYPHEN);

        rpc.append(TEST_ANGLE_LEFT).append(TEST_ONU_SET_CONFIG).append(TEST_SPACE);
        rpc.append(TEST_VOLT_NE_NAMESPACE).append(TEST_ANGLE_RIGHT).append(TEST_NEW_LINE);
        rpc.append(startTag(TEST_PONLINK_ID, false));
        rpc.append(onuId[FIRST_PART]);
        rpc.append(endTag(TEST_PONLINK_ID));
        rpc.append(startTag(TEST_ONU_ID, false));
        rpc.append(onuId[SECOND_PART]);
        rpc.append(endTag(TEST_ONU_ID));
        rpc.append(startTag(TEST_CONFIG_INFO));
        rpc.append(startTag(data[SECOND_PART], false));
        rpc.append(data[THIRD_PART]);
        rpc.append(endTag(data[SECOND_PART]));
        rpc.append(endTag(TEST_CONFIG_INFO));
        rpc.append(endTag(TEST_ONU_SET_CONFIG));

        String testRequest = rpc.toString();
        testRequest = testRequest.replaceAll(TEST_WHITESPACES_REGEX, TEST_EMPTY_STRING);
        request = request.replaceAll(TEST_WHITESPACES_REGEX, TEST_EMPTY_STRING);
        boolean result = request.equals(testRequest);
        assertTrue("Does not match with generated string", result);
        return result;
    }

    /**
     * Verifies XML request string by comparing with generated string (statistics).
     *
     * @param request XML string for get operation
     * @return true if XML string matches with generated
     */
    private boolean verifyGetRequestForStats(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_GET_STATS_TCS.get(currentKey);
        String[] onuId = null;

        if (target != null) {
            onuId = target.split(TEST_HYPHEN);
        }

        rpc.append(TEST_VOLT_NE_OPEN).append(TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT).append(TEST_NEW_LINE);
        rpc.append(startTag(TEST_VOLT_STATISTICS));
        if (onuId != null) {
            rpc.append(startTag(TEST_ONU_STATISTICS));
            rpc.append(startTag(TEST_ONU_GEM_STATS));
            rpc.append(startTag(TEST_GEM_STATS));
            rpc.append(startTag(TEST_PONLINK_ID, false));
            rpc.append(onuId[FIRST_PART]);
            rpc.append(endTag(TEST_PONLINK_ID));
            if (onuId.length > ONE) {
                rpc.append(startTag(TEST_ONU_ID, false));
                rpc.append(onuId[SECOND_PART]);
                rpc.append(endTag(TEST_ONU_ID));
            }
            rpc.append(endTag(TEST_GEM_STATS));
            rpc.append(endTag(TEST_ONU_GEM_STATS));

            rpc.append(startTag(TEST_ONU_ETH_STATS));
            rpc.append(startTag(TEST_ETH_STATS));
            rpc.append(startTag(TEST_PONLINK_ID, false));
            rpc.append(onuId[FIRST_PART]);
            rpc.append(endTag(TEST_PONLINK_ID));
            if (onuId.length > ONE) {
                rpc.append(startTag(TEST_ONU_ID, false));
                rpc.append(onuId[SECOND_PART]);
                rpc.append(endTag(TEST_ONU_ID));
            }
            rpc.append(endTag(TEST_ETH_STATS));
            rpc.append(endTag(TEST_ONU_ETH_STATS));
            rpc.append(endTag(TEST_ONU_STATISTICS));
        } else {
            rpc.append(emptyTag(TEST_ONU_STATISTICS));
        }
        rpc.append(endTag(TEST_VOLT_STATISTICS));
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
            return false;
        }

        @Override
        public boolean verifyGet(String filterSchema, String withDefaultsMode) {
            boolean result;
            boolean forStats;

            assertTrue("Incorrect withDefaultsMode", withDefaultsMode.equals(TEST_REPORT_ALL));
            filterSchema = filterSchema.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_VOLT_NAMESPACE,
                    filterSchema.contains(TEST_VOLT_NAMESPACE));

            forStats = filterSchema.contains(TEST_VOLT_STATISTICS);
            if (forStats) {
                result = verifyGetRequestForStats(filterSchema);
            } else {
                result = verifyGetRequest(filterSchema);
            }
            assertTrue("XML verification failure", result);
            return result;
        }

        @Override
        public String buildGetReply() {
            return null;
        }

        @Override
        public boolean verifyWrappedRpc(String request) {
            boolean result;

            request = request.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_ONU_SET_CONFIG_WITH_NAMESPACE,
                    request.contains(TEST_ONU_SET_CONFIG_WITH_NAMESPACE));

            result = verifyWrappedRpcRequest(request);
            assertTrue("XML verification failure", result);
            return result;
        }

        @Override
        public void verifyStartSubscription(String filterSchema) {
        }
    }

}
