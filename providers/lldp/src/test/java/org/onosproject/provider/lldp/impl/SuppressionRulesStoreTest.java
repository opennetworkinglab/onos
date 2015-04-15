/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onosproject.provider.lldp.impl;

import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onosproject.net.Device;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class SuppressionRulesStoreTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // "lldp_suppression.json"
    SuppressionRules testData
        = new SuppressionRules(ImmutableSet.of(deviceId("of:2222000000000000")),
                               ImmutableSet.of(Device.Type.ROADM),
                               ImmutableMap.of("no-lldp", SuppressionRules.ANY_VALUE,
                                               "sendLLDP", "false"));

    private static void assertRulesEqual(SuppressionRules expected, SuppressionRules actual) {
        assertEquals(expected.getSuppressedDevice(),
                     actual.getSuppressedDevice());
        assertEquals(expected.getSuppressedDeviceType(),
                     actual.getSuppressedDeviceType());
        assertEquals(expected.getSuppressedAnnotation(),
                     actual.getSuppressedAnnotation());
    }

    @Test
    public void testRead() throws URISyntaxException, IOException {
        Path path = Paths.get(Resources.getResource("lldp_suppression.json").toURI());

        SuppressionRulesStore store = new SuppressionRulesStore(path.toString());

        SuppressionRules rules = store.read();

        assertRulesEqual(testData, rules);
    }

    @Test
    public void testWrite() throws IOException {
        File newFile = tempFolder.newFile();
        SuppressionRulesStore store = new SuppressionRulesStore(newFile);
        store.write(testData);

        SuppressionRulesStore reload = new SuppressionRulesStore(newFile);
        SuppressionRules rules = reload.read();

        assertRulesEqual(testData, rules);
    }
}
