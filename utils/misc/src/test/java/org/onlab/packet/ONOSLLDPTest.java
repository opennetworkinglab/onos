/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onlab.packet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the ONOSLLDP class.
 */
public class ONOSLLDPTest {

    private static final String DEVICE_ID = "of:c0a80a6e00000001";
    private static final ChassisId CHASSIS_ID = new ChassisId(67890);
    private static final Integer PORT_NUMBER = 2;
    private static final Integer PORT_NUMBER_2 = 98761234;
    private static final String PORT_DESC = "Ethernet1";
    private static final String PORT_NAME = "Ethernet2";
    private static final String TEST_SECRET = "test";

    private ONOSLLDP onoslldp = ONOSLLDP.onosSecureLLDP(DEVICE_ID, CHASSIS_ID, PORT_NUMBER, PORT_DESC, TEST_SECRET);

    /**
     * Tests port number and getters.
     */
    @Test
    public void testPortNumber() throws Exception {
        assertEquals("the value from constructor with getPort value is mismatched",
                PORT_NUMBER, onoslldp.getPort());

        onoslldp.setPortId(PORT_NUMBER_2);
        assertEquals("the value from setPortId with getPort value is mismatched",
                PORT_NUMBER_2, onoslldp.getPort());
    }

    /**
     * Tests port name and getters.
     */
    @Test
    public void testPortName() throws Exception {
        onoslldp.setPortName(PORT_NAME);
        assertEquals("the value from setPortName with getPortNameString value is mismatched",
                PORT_NAME, onoslldp.getPortNameString());
    }
}