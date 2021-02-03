/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.util;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.segmentIdHex;

/**
 * Unit tests for kubevirt networking utils.
 */
public final class KubevirtNetworkingUtilTest {

    /**
     * Tests the segmentIdHex method.
     */
    @Test
    public void testSegmentIdHex() {
        assertEquals("000001", segmentIdHex("1"));
        assertEquals("00000a", segmentIdHex("10"));
        assertEquals("ffffff", segmentIdHex("16777215"));
    }
}
