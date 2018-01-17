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
package org.onosproject.t3.impl;

import org.junit.Test;
import org.onlab.packet.MacAddress;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for util methods of the Trellis Troubleshoot Toolkit.
 */
public class TroubleshootUtilsTest {

    @Test
    public void testMacMatch() {

        MacAddress min = MacAddress.valueOf("01:00:5E:00:00:00");
        MacAddress mask = MacAddress.valueOf("FF:FF:FF:80:00:00");
        MacAddress macOk = MacAddress.valueOf("01:00:5E:00:00:01");

        assertTrue("False on correct match", TroubleshootUtils.compareMac(macOk, min, mask));

        MacAddress macWrong = MacAddress.valueOf("01:00:5E:80:00:00");

        assertFalse("True on false match", TroubleshootUtils.compareMac(macWrong, min, mask));

        MacAddress maskEmpty = MacAddress.valueOf("00:00:00:00:00:00");

        assertTrue("False on empty Mask", TroubleshootUtils.compareMac(macOk, min, maskEmpty));

        MacAddress maskFull = MacAddress.valueOf("FF:FF:FF:FF:FF:FF");

        assertFalse("True on full Mask", TroubleshootUtils.compareMac(macOk, min, maskFull));

    }
}