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
package org.onosproject.incubator.net.l2monitoring.cfm;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class MepLbEntryTest {

    MepLbEntry lbResult;

    @Before
    public void setUp() throws Exception {
        lbResult = DefaultMepLbEntry.builder()
                .countLbrMacMisMatch(987654321L)
                .countLbrReceived(987654322L)
                .countLbrTransmitted(987654323L)
                .countLbrValidInOrder(987654324L)
                .countLbrValidOutOfOrder(987654325L)
                .nextLbmIdentifier(987654326L)
                .build();
    }

    @Test
    public void testNextLbmIdentifier() {
        assertEquals(987654326L, lbResult.nextLbmIdentifier());
    }

    @Test
    public void testCountLbrTransmitted() {
        assertEquals(987654323L, lbResult.countLbrTransmitted());
    }

    @Test
    public void testCountLbrReceived() {
        assertEquals(987654322L, lbResult.countLbrReceived());
    }

    @Test
    public void testCountLbrValidInOrder() {
        assertEquals(987654324L, lbResult.countLbrValidInOrder());
    }

    @Test
    public void testCountLbrValidOutOfOrder() {
        assertEquals(987654325L, lbResult.countLbrValidOutOfOrder());
    }

    @Test
    public void testCountLbrMacMisMatch() {
        assertEquals(987654321L, lbResult.countLbrMacMisMatch());
    }

}
