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
 * Unit tests for methods of FujitsuVoltOnuConfig.
 */
public class FujitsuVoltOnuConfigTest {

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

    private static final String[] INVALID_GET_TCS = {
        "a-b",
        "--1-2",
        "s-1",
        "16-1-1",
        "1 A-1",
        "1*A-1",
    };
    private static final String[] VALID_GET_TCS = {
        "1",
        "1-2",
        null,
    };
    private static final String[] INVALID_SET_TCS = {
        "-11-3:admin-state:enable",
        "1-2:admin-state:false",
        "1-2:pm-enable:123",
        "^1-2:pm-enable:false",
        "1-2:fec-enable:xyz",
        "1-2:security-enable:123abc",
        "2-3:password:-1&",
        "2:admin-state:disable",
    };
    private static final String[] VALID_SET_TCS = {
        "1-11:admin-state:disable",
        "8-1:pm-enable:true",
        "1-1:fec-enable:true",
        "1-21:security-enable:false",
        "3-2:password:abc123",
    };
    private static final String[] INVALID_GET_STATS_TCS = {
        "1-a",
        "1:1",
        "a-1",
        "1-1-1",
        "2 A-1",
        "2/A-1",
    };
    private static final String[] VALID_GET_STATS_TCS = {
        "1",
        "3-12",
        null,
    };
    private Integer currentKey;
    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltOnuConfig voltConfig;

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

        for (int i = ZERO; i < INVALID_GET_TCS.length; i++) {
            target = INVALID_GET_TCS[i];
            reply = voltConfig.getOnus(target);
            assertNull("Incorrect response for INVALID_GET_TCS", reply);
        }
    }

    /**
     * Run to verify handling of valid input for get operation.
     */
    @Test
    public void testValidGetOnus() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < VALID_GET_TCS.length; i++) {
            target = VALID_GET_TCS[i];
            currentKey = i;
            reply = voltConfig.getOnus(target);
            assertNotNull("Incorrect response for VALID_GET_TCS", reply);
        }
    }

    /**
     * Run to verify handling of invalid input for set operation.
     */
    @Test
    public void testInvalidSetOnuInput() throws Exception {
        String target;
        String reply;

        for (int i = ZERO; i < INVALID_SET_TCS.length; i++) {
            target = INVALID_SET_TCS[i];
            reply = voltConfig.setOnu(target);
            assertNull("Incorrect response for INVALID_SET_TCS", reply);
        }
    }

    /**
     * Run to verify handling of valid input for set operation.
     */
    @Test
    public void testValidSetOnu() throws Exception {
        String target;
        String reply;

        for (int i = ZERO; i < VALID_SET_TCS.length; i++) {
            target = VALID_SET_TCS[i];
            currentKey = i;
            reply = voltConfig.setOnu(target);
            assertNotNull("Incorrect response for VALID_SET_TCS", reply);
        }
    }

    /**
     * Run to verify handling of invalid input for get statistics operation.
     */
    @Test
    public void testInvalidGetOnuStatsInput() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < INVALID_GET_STATS_TCS.length; i++) {
            target = INVALID_GET_STATS_TCS[i];
            reply = voltConfig.getOnuStatistics(target);
            assertNull("Incorrect response for INVALID_GET_STATS_TCS", reply);
        }
    }

    /**
     * Run to verify handling of valid input for get statistics operation.
     */
    @Test
    public void testValidGetOnuStats() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < VALID_GET_STATS_TCS.length; i++) {
            target = VALID_GET_STATS_TCS[i];
            currentKey = i;
            reply = voltConfig.getOnuStatistics(target);
            assertNotNull("Incorrect response for VALID_GET_STATS_TCS", reply);
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
        String target = VALID_GET_TCS[currentKey];
        String[] onuId = null;

        if (target != null) {
            onuId = target.split(TEST_HYPHEN);
        }

        rpc.append(TEST_VOLT_NE_OPEN + TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        if (onuId != null) {
            rpc.append(startTag(TEST_VOLT_ONUS))
                .append(startTag(TEST_ONUS_PERLINK))
                .append(startTag(TEST_PONLINK_ID, false))
                .append(onuId[FIRST_PART])
                .append(endTag(TEST_PONLINK_ID));
            if (onuId.length > ONE) {
                rpc.append(startTag(TEST_ONUS_LIST))
                    .append(startTag(TEST_ONU_INFO))
                    .append(startTag(TEST_ONU_ID, false))
                    .append(onuId[SECOND_PART])
                    .append(endTag(TEST_ONU_ID))
                    .append(endTag(TEST_ONU_INFO))
                    .append(endTag(TEST_ONUS_LIST));
            }
            rpc.append(endTag(TEST_ONUS_PERLINK))
                .append(endTag(TEST_VOLT_ONUS));
        } else {
            rpc.append(emptyTag(TEST_VOLT_ONUS));
        }
        rpc.append(TEST_VOLT_NE_CLOSE);

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
    private boolean verifyWrappedRpcRequest(String request) {
        StringBuilder rpc = new StringBuilder();
        String target = VALID_SET_TCS[currentKey];
        String[] data = target.split(TEST_COLON);
        String[] onuId = data[FIRST_PART].split(TEST_HYPHEN);

        rpc.append(TEST_ANGLE_LEFT + TEST_ONU_SET_CONFIG + TEST_SPACE);
        rpc.append(TEST_VOLT_NE_NAMESPACE + TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        rpc.append(startTag(TEST_PONLINK_ID, false))
            .append(onuId[FIRST_PART])
            .append(endTag(TEST_PONLINK_ID))
            .append(startTag(TEST_ONU_ID, false))
            .append(onuId[SECOND_PART])
            .append(endTag(TEST_ONU_ID))
            .append(startTag(TEST_CONFIG_INFO))
            .append(startTag(data[SECOND_PART], false))
            .append(data[THIRD_PART])
            .append(endTag(data[SECOND_PART]))
            .append(endTag(TEST_CONFIG_INFO))
            .append(endTag(TEST_ONU_SET_CONFIG));

        String testRequest = rpc.toString();
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
        String target = VALID_GET_STATS_TCS[currentKey];
        String[] onuId = null;

        if (target != null) {
            onuId = target.split(TEST_HYPHEN);
        }

        rpc.append(TEST_VOLT_NE_OPEN + TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        rpc.append(startTag(TEST_VOLT_STATISTICS));
        if (onuId != null) {
            rpc.append(startTag(TEST_ONU_STATISTICS))
                .append(startTag(TEST_ONU_GEM_STATS))
                .append(startTag(TEST_GEM_STATS))
                .append(startTag(TEST_PONLINK_ID, false))
                .append(onuId[FIRST_PART])
                .append(endTag(TEST_PONLINK_ID));
            if (onuId.length > ONE) {
                rpc.append(startTag(TEST_ONU_ID, false))
                    .append(onuId[SECOND_PART])
                    .append(endTag(TEST_ONU_ID));
            }
            rpc.append(endTag(TEST_GEM_STATS))
                .append(endTag(TEST_ONU_GEM_STATS));

            rpc.append(startTag(TEST_ONU_ETH_STATS))
                .append(startTag(TEST_ETH_STATS))
                .append(startTag(TEST_PONLINK_ID, false))
                .append(onuId[FIRST_PART])
                .append(endTag(TEST_PONLINK_ID));
            if (onuId.length > ONE) {
                rpc.append(startTag(TEST_ONU_ID, false))
                    .append(onuId[SECOND_PART])
                    .append(endTag(TEST_ONU_ID));
            }
            rpc.append(endTag(TEST_ETH_STATS))
                .append(endTag(TEST_ONU_ETH_STATS))
                .append(endTag(TEST_ONU_STATISTICS));
        } else {
            rpc.append(emptyTag(TEST_ONU_STATISTICS));
        }
        rpc.append(endTag(TEST_VOLT_STATISTICS))
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
        public boolean verifyEditConfig(TargetConfig target, String mode, String request) {
            return false;
        }

        @Override
        public boolean verifyEditConfig(String targetConfiguration, String mode, String newConfiguration) {
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
