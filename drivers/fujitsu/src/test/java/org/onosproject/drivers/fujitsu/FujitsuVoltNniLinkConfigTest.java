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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;
import static org.onosproject.netconf.TargetConfig.RUNNING;

/**
 * Unit tests for methods of FujitsuVoltPonLinkConfig.
 */
public class FujitsuVoltNniLinkConfigTest {

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListenerTest();

    private static final String TEST_VOLT_PORTS = "volt-ports";
    private static final String TEST_ETH_NNILINK_PORTS = "eth-nnilink-ports";
    private static final String TEST_ETH_NNILINK_PORT = "eth-nnilink-port";
    private static final String TEST_NNILINK_ID = "nnilink-id";

    private static final String[] INVALID_GET_TCS = {
        "a-b-c",
        "--1",
        "s-1",
        "1-1",
        "1 A",
        "1*A",
        "-1",
    };
    private static final String[] VALID_GET_TCS = {
        "1",
        null,
    };
    private static final String[] INVALID_SET_TCS = {
        "1:loopback-enable:true:1",
        "-11:loopback-enable:false",
        "2:loopback:true",
        "3:loopback-enable:invalid",
        "1:loopback-uni:8",
        "-1:loopback-nni:nni",
        ":loopback-nni:nni",
    };
    private static final String[] VALID_SET_TCS = {
        "1:loopback-enable:true",
        "2:loopback-enable:false",
    };
    private Integer currentKey;
    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltNniLinkConfig voltConfig;

    @Before
    public void setUp() throws Exception {
        controller = new FujitsuNetconfControllerMock();
        driverHandler = controller.setUp(listener);
        voltConfig = new FujitsuVoltNniLinkConfig();
        voltConfig.setHandler(driverHandler);
    }

    /**
     * Run to verify handling of invalid input for get operation.
     */
    @Test
    public void testInvalidGetNniLinksInput() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < INVALID_GET_TCS.length; i++) {
            target = INVALID_GET_TCS[i];
            reply = voltConfig.getNniLinks(target);
            assertNull("Incorrect response for INVALID_GET_TCS", reply);
        }
    }

    /**
     * Run to verify handling of valid input for get operation.
     */
    @Test
    public void testValidGetNniLinksInput() throws Exception {
        String reply;
        String target;

        for (int i = ZERO; i < VALID_GET_TCS.length; i++) {
            target = VALID_GET_TCS[i];
            currentKey = i;
            reply = voltConfig.getNniLinks(target);
            assertNotNull("Incorrect response for VALID_GET_TCS", reply);
        }
    }

    /**
     * Run to verify handling of invalid input for set operation.
     */
    @Test
    public void testInvalidSetNniLinkInput() throws Exception {
        String target;
        boolean result;

        for (int i = ZERO; i < INVALID_SET_TCS.length; i++) {
            target = INVALID_SET_TCS[i];
            result = voltConfig.setNniLink(target);
            assertFalse("Incorrect response for INVALID_SET_TCS", result);
        }
    }

    /**
     * Run to verify handling of valid input for set operation.
     */
    @Test
    public void testValidSetNniLinkInput() throws Exception {
        String target;
        boolean result;

        for (int i = ZERO; i < VALID_SET_TCS.length; i++) {
            target = VALID_SET_TCS[i];
            currentKey = i;
            result = voltConfig.setNniLink(target);
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
            rpc.append(startTag(TEST_ETH_NNILINK_PORTS))
                .append(startTag(TEST_ETH_NNILINK_PORT))
                .append(startTag(TEST_NNILINK_ID, false))
                .append(target)
                .append(endTag(TEST_NNILINK_ID))
                .append(endTag(TEST_ETH_NNILINK_PORT))
                .append(endTag(TEST_ETH_NNILINK_PORTS));
        } else {
            rpc.append(emptyTag(TEST_ETH_NNILINK_PORTS));
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

        rpc.append(TEST_VOLT_NE_OPEN + TEST_VOLT_NE_NAMESPACE)
            .append(TEST_ANGLE_RIGHT + TEST_NEW_LINE)
            .append(startTag(TEST_VOLT_PORTS))
            .append(startTag(TEST_ETH_NNILINK_PORTS))
            .append(startTag(TEST_ETH_NNILINK_PORT))
            .append(startTag(TEST_NNILINK_ID, false))
            .append(data[FIRST_PART])
            .append(endTag(TEST_NNILINK_ID))
            .append(startTag(data[SECOND_PART], false))
            .append(data[THIRD_PART])
            .append(endTag(data[SECOND_PART]))
            .append(endTag(TEST_ETH_NNILINK_PORT))
            .append(endTag(TEST_ETH_NNILINK_PORTS))
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
        public boolean verifyEditConfig(TargetConfig target, String mode, String request) {
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
