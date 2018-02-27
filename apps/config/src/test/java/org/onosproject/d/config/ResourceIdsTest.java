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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.onosproject.d.config.DeviceResourceIds.DCS_NAMESPACE;
import static org.onosproject.d.config.DeviceResourceIds.DEVICES_ID;
import static org.onosproject.d.config.DeviceResourceIds.toResourceId;
import static org.onosproject.d.config.ResourceIds.fromInstanceIdentifier;

import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.yang.model.ResourceId;

public class ResourceIdsTest {

    static final ResourceId DEVICES = ResourceId.builder()
            .addBranchPointSchema(DeviceResourceIds.ROOT_NAME, DCS_NAMESPACE)
            .addBranchPointSchema(DeviceResourceIds.DEVICES_NAME, DCS_NAMESPACE)
            .build();

    @Test
    public void testFromInstanceIdentifier() {

        ResourceId eth0 = ResourceId.builder()
                .addBranchPointSchema("interfaces", "ietf-interfaces")
                .addBranchPointSchema("interface", "ietf-interfaces")
                .addKeyLeaf("name", "ietf-interfaces", "eth0")
                .build();
        assertThat(ResourceIds.fromInstanceIdentifier("/ietf-interfaces:interfaces/interface[name=\"eth0\"]"),
                   is(eth0));

        assertThat("fromInstanceIdentifier return path relative to virtual root",
                   ResourceIds.fromInstanceIdentifier("/org.onosproject.dcs:devices"),
                   is(ResourceIds.relativize(ResourceIds.ROOT_ID, DEVICES_ID)));

        assertThat(ResourceIds.prefixDcsRoot(
                     ResourceIds.fromInstanceIdentifier("/org.onosproject.dcs:devices")),
                   is(DEVICES_ID));

        assertThat(ResourceIds.fromInstanceIdentifier("/"),
                    is(nullValue()));

        DeviceId deviceId = DeviceId.deviceId("test:device-identifier");
        assertThat(ResourceIds.prefixDcsRoot(
                 fromInstanceIdentifier("/org.onosproject.dcs:devices/device[device-id=\"test:device-identifier\"]")),
                   is(toResourceId(deviceId)));

    }

    @Test
    public void testToInstanceIdentifier() {

        assertThat(ResourceIds.toInstanceIdentifier(ResourceIds.ROOT_ID),
                   is("/"));
        assertThat(ResourceIds.toInstanceIdentifier(DEVICES_ID),
                   is("/org.onosproject.dcs:devices"));

        DeviceId deviceId = DeviceId.deviceId("test:device-identifier");
        assertThat(ResourceIds.toInstanceIdentifier(toResourceId(deviceId)),
                   is("/org.onosproject.dcs:devices/device[device-id=\"test:device-identifier\"]"));

        assertThat(ResourceIds.toInstanceIdentifier(ResourceIds.relativize(DEVICES_ID, toResourceId(deviceId))),
                   is("/org.onosproject.dcs:device[device-id=\"test:device-identifier\"]"));

        ResourceId eth0 = ResourceId.builder()
                .addBranchPointSchema("interfaces", "ietf-interfaces")
                .addBranchPointSchema("interface", "ietf-interfaces")
                .addKeyLeaf("name", "ietf-interfaces", "eth0")
                .build();
        assertThat(ResourceIds.toInstanceIdentifier(eth0),
                   is("/ietf-interfaces:interfaces/interface[name=\"eth0\"]"));


        assertThat(ResourceIds.toInstanceIdentifier(ResourceIds.concat(toResourceId(deviceId), eth0)),
                   is("/org.onosproject.dcs:devices/device[device-id=\"test:device-identifier\"]"
                           + "/ietf-interfaces:interfaces/interface[name=\"eth0\"]"));
    }

    @Test
    public void testConcat() {
        ResourceId devices = ResourceId.builder()
            .addBranchPointSchema(DeviceResourceIds.DEVICES_NAME,
                                  DCS_NAMESPACE)
            .build();

        assertEquals(DEVICES, ResourceIds.concat(ResourceIds.ROOT_ID, devices));
    }

    @Test
    public void testRelativize() {
        ResourceId relDevices = ResourceIds.relativize(ResourceIds.ROOT_ID, DEVICES);
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
