/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class MepIdTest {
    @Test
    public void testLowRange() {
        try {
            MepId.valueOf((short) -1);
            fail("Exception expected for MepId = -1");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid value for Mep Id"));
        }

        try {
            MepId.valueOf((short) 0);
            fail("Exception expected for MepId = 0");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid value for Mep Id"));
        }
    }

    @Test
    public void testHighRange() {
        try {
            MepId.valueOf((short) 8192);
            fail("Exception expected for MepId = 8192");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid value for Mep Id"));
        }

        try {
            MepId.valueOf((short) 33333); //Above the range of short
            fail("Exception expected for MepId = 33333");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid value for Mep Id"));
        }
    }

}
