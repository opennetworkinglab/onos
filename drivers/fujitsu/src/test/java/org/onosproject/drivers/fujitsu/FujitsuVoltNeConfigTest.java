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
import static org.junit.Assert.assertNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtilityMock.*;

/**
 * Unit tests for methods of FujitsuVoltNeConfig.
 */
public class FujitsuVoltNeConfigTest {

    private FujitsuNetconfControllerMock controller;
    private FujitsuDriverHandlerAdapter driverHandler;
    private FujitsuVoltNeConfig voltConfig;

    private final FujitsuNetconfSessionListenerTest listener = new InternalSessionListener();

    @Before
    public void setUp() throws Exception {
        controller = new FujitsuNetconfControllerMock();
        driverHandler = controller.setUp(listener);
        voltConfig = new FujitsuVoltNeConfig();
        voltConfig.setHandler(driverHandler);
    }

    /**
     * Run to verify handling of valid input for get operation.
     */
    @Test
    public void testGetAll() throws Exception {
        String reply = voltConfig.getAll();
        assertNotNull("Incorrect response", reply);
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
        rpc.append(TEST_VOLT_NE_CLOSE);

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
        public boolean verifyEditConfig(TargetConfig target, String mode, String request) {
            return false;
        }

        @Override
        public boolean verifyEditConfig(String target, String mode, String request) {
            return false;
        }

        @Override
        public boolean verifyGet(String filterSchema, String withDefaultsMode) {
            assertTrue("Incorrect withDefaultsMode",
                    withDefaultsMode.equals(TEST_REPORT_ALL));

            filterSchema = filterSchema.replaceAll(TEST_DUPLICATE_SPACES_REGEX, TEST_SPACE);
            assertTrue("Does not contain:" + TEST_VOLT_NAMESPACE,
                    filterSchema.contains(TEST_VOLT_NAMESPACE));

            boolean result = verifyGetRequest(filterSchema);
            assertTrue("XML verification failure", result);
            return true;
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
