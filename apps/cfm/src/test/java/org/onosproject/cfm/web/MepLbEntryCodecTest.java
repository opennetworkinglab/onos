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
package org.onosproject.cfm.web;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLbEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbEntry;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MepLbEntryCodecTest {
    ObjectMapper mapper;
    CfmCodecContext context;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
    }

    @Test
    public void testEncodeMepLbEntryCodecContext() {
        MepLbEntry mepLbEntry1 = DefaultMepLbEntry.builder()
                .countLbrMacMisMatch(987654321L)
                .countLbrReceived(987654322L)
                .countLbrTransmitted(987654323L)
                .countLbrValidInOrder(987654324L)
                .countLbrValidOutOfOrder(987654325L)
                .nextLbmIdentifier(987654326L)
                .build();

        assertEquals(987654321L, mepLbEntry1.countLbrMacMisMatch());
        assertEquals(987654322L, mepLbEntry1.countLbrReceived());
        assertEquals(987654323L, mepLbEntry1.countLbrTransmitted());
        assertEquals(987654324L, mepLbEntry1.countLbrValidInOrder());
        assertEquals(987654325L, mepLbEntry1.countLbrValidOutOfOrder());
        assertEquals(987654326L, mepLbEntry1.nextLbmIdentifier());
    }

}
