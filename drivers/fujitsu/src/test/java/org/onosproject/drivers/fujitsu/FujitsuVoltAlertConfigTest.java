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

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.netconf.TargetConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.io.InputStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;
import static org.onosproject.netconf.TargetConfig.*;


/**
 * Unit tests for methods of FujitsuVoltAlertConfig.
 */
public class FujitsuVoltAlertConfigTest {

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListener();

    private static final String TEST_VOLT_ALERTS = "volt-alerts";
    private static final String TEST_ALERT_FILTER = "alert-filter";
    private static final String TEST_NOTIFY_ALERT = "notify-alert";

    private static final String TEST_NOTIFY_ALERT_WITH_NAMESPACE =
            TEST_ANGLE_LEFT + TEST_NOTIFY_ALERT + TEST_SPACE +
            TEST_VOLT_NE_NAMESPACE;

    private static final String NOTIFY_ALERT_FILE = "/notifyalert.xml";

    private static final String[] INVALID_SET_TCS = {
        ":abc",
        "@critical",
        "1234",
    };
    private static final String[] VALID_SET_TCS = {
        "minor",
        "critical",
        "none",
        "info",
        "major",
    };
    private static final String[] VERIFY_NOTIFY_ALERT_FILE_TCS = {
        "notify-alert",
        "alert-seqnum",
        "alert-type",
        "alert-clear",
        "severity",
        "resource-id",
        "ponlink-id",
        "alert-time",
        "date",
        "time",
    };
    private Integer currentKey;
    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltAlertConfig voltConfig;

    @Before
    public void setUp() throws Exception {
        controller = new FujitsuNetconfControllerMock();
        driverHandler = controller.setUp(listener);
        voltConfig = new FujitsuVoltAlertConfig();
        voltConfig.setHandler(driverHandler);
    }

    /**
     * Run to verify handling of subscription.
     */
    @Test
    public void testSubscribe() throws Exception {
        assertTrue("Incorrect response", voltConfig.subscribe(null));
        assertFalse("Incorrect response", voltConfig.subscribe("false"));
        assertTrue("Incorrect response", voltConfig.subscribe("disable"));
    }

    /**
     * Run to verify handling of subscription.
     */
    @Test
    public void testGetAlertFilter() throws Exception {
        voltConfig.getAlertFilter();
    }

    /**
     * Run to verify handling of invalid input for set operation.
     */
    @Test
    public void testInvalidSetAlertFilterInput() throws Exception {
        String target;
        boolean result;

        for (int i = ZERO; i < INVALID_SET_TCS.length; i++) {
            target = INVALID_SET_TCS[i];
            result = voltConfig.setAlertFilter(target);
            assertFalse("Incorrect response for ", result);
        }
    }

    /**
     * Run to verify handling of valid input for set operation.
     */
    @Test
    public void testValidSetAlertFilter() throws Exception {
        String target;
        boolean result;

        for (int i = ZERO; i < VALID_SET_TCS.length; i++) {
            target = VALID_SET_TCS[i];
            currentKey = i;
            result = voltConfig.setAlertFilter(target);
            assertTrue("Incorrect response for ", result);
        }
    }

    /**
     * Run to verify sample notify-alert components.
     */
    @Test
    public void testNotifyAlert() throws Exception {
        boolean result;
        result = verifyNotifyAlert();
        assertTrue("Incorrect response for ", result);
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
        rpc.append(startTag(TEST_VOLT_ALERTS))
            .append(emptyTag(TEST_ALERT_FILTER))
            .append(endTag(TEST_VOLT_ALERTS))
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

        rpc.append(TEST_VOLT_NE_OPEN + TEST_VOLT_NE_NAMESPACE);
        rpc.append(TEST_ANGLE_RIGHT + TEST_NEW_LINE);
        rpc.append(startTag(TEST_VOLT_ALERTS))
            .append(startTag(TEST_ALERT_FILTER, false))
            .append(target)
            .append(endTag(TEST_ALERT_FILTER))
            .append(endTag(TEST_VOLT_ALERTS))
            .append(TEST_VOLT_NE_CLOSE);

        String testRequest = rpc.toString();
        boolean result = request.equals(testRequest);
        assertTrue("Does not match with generated string", result);
        return result;
    }

    /**
     * Verifies notify-alert XML.
     */
    private boolean verifyNotifyAlert() {
        String testRequest;
        String target;

        try {
            InputStream fileStream = getClass().getResourceAsStream(
                    NOTIFY_ALERT_FILE);
            testRequest = IOUtils.toString(fileStream, StandardCharsets.UTF_8);
            testRequest = testRequest.substring(testRequest.indexOf(
                    NOTIFY_ALERT_FILE) + NOTIFY_ALERT_FILE.length());
        } catch (IOException e) {
            fail("IOException while reading: " + NOTIFY_ALERT_FILE);
            return false;
        }

        for (int i = ZERO; i < VERIFY_NOTIFY_ALERT_FILE_TCS.length; i++) {
            target = VERIFY_NOTIFY_ALERT_FILE_TCS[i];
            int index = testRequest.indexOf(target);
                if (index < ZERO) {
                    return false;
                }
        }
        return true;
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
        public boolean verifyEditConfig(String targetConfiguration, String mode, String newConfiguration) {
            return verifyEditConfig(TargetConfig.valueOf(targetConfiguration), mode, newConfiguration);
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
        public boolean verifyGet(String filterSchema, String withDefaultsMode) {
            boolean result;

            assertTrue("Incorrect withDefaultsMode", withDefaultsMode.equals(TEST_REPORT_ALL));
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

            filterSchema = filterSchema.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_NOTIFY_ALERT_WITH_NAMESPACE,
                    filterSchema.contains(TEST_NOTIFY_ALERT_WITH_NAMESPACE));

            StringBuilder rpc = new StringBuilder();
            rpc.append(TEST_ANGLE_LEFT + TEST_NOTIFY_ALERT + TEST_SPACE);
            rpc.append(TEST_VOLT_NE_NAMESPACE + TEST_SLASH + TEST_ANGLE_RIGHT);

            String testRequest = rpc.toString();
            boolean result = filterSchema.equals(testRequest);
            assertTrue("Does not match with generated string", result);
        }
    }

}
