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
package org.onosproject.d.config;

import static org.junit.Assert.*;
import static org.onosproject.d.config.DeviceResourceIds.DCS_NAMESPACE;

import org.junit.Test;
import org.onosproject.yang.model.ResourceId;

public class ResourceIdsTest {

    static final ResourceId DEVICES = ResourceId.builder()
            .addBranchPointSchema(DeviceResourceIds.ROOT_NAME, DCS_NAMESPACE)
            .addBranchPointSchema(DeviceResourceIds.DEVICES_NAME, DCS_NAMESPACE)
            .build();

    @Test
    public void testConcat() {
        ResourceId devices = ResourceId.builder()
            .addBranchPointSchema(DeviceResourceIds.DEVICES_NAME,
                                  DCS_NAMESPACE)
            .build();

        assertEquals(DEVICES, ResourceIds.concat(DeviceResourceIds.ROOT_ID, devices));
    }

    @Test
    public void testRelativize() {
        ResourceId relDevices = ResourceIds.relativize(DeviceResourceIds.ROOT_ID, DEVICES);
        assertEquals(DeviceResourceIds.DEVICES_NAME,
                     relDevices.nodeKeys().get(0).schemaId().name());
        assertEquals(DCS_NAMESPACE,
                     relDevices.nodeKeys().get(0).schemaId().namespace());
        assertEquals(1, relDevices.nodeKeys().size());
    }

    @Test
    public void testRelativizeEmpty() {
        ResourceId relDevices = ResourceIds.relativize(DEVICES, DEVICES);
        // equivalent of . in file path, expressed as ResourceId with empty
        assertTrue(relDevices.nodeKeys().isEmpty());

    }
}
