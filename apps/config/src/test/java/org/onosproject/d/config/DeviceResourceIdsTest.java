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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.d.config.DeviceResourceIds.DCS_NAMESPACE;
import static org.onosproject.d.config.DeviceResourceIds.DEVICES_NAME;
import static org.onosproject.d.config.DeviceResourceIds.DEVICE_ID_KL_NAME;
import static org.onosproject.d.config.DeviceResourceIds.DEVICE_NAME;
import static org.onosproject.d.config.DeviceResourceIds.ROOT_NAME;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.yang.model.KeyLeaf;
import org.onosproject.yang.model.ListKey;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.SchemaId;

public class DeviceResourceIdsTest {

    static final DeviceId DID_A = DeviceId.deviceId("test:a");

    static final ResourceId DEVICES = ResourceId.builder()
            .addBranchPointSchema(ROOT_NAME, DCS_NAMESPACE)
            .addBranchPointSchema(DEVICES_NAME, DCS_NAMESPACE)
            .build();

    ResourceId ridA;
    ResourceId ridAcopy;

    @Before
    public void setUp() throws Exception {
        ridA = DeviceResourceIds.toResourceId(DID_A);
        ridAcopy = ridA.copyBuilder().build();
    }

    @Test
    public void testToDeviceId() throws CloneNotSupportedException {
        ResourceId ridAchild = ridA.copyBuilder()
                    .addBranchPointSchema("some", "ns")
                    .addBranchPointSchema("random", "ns")
                    .addBranchPointSchema("child", "ns")
                    .build();

        assertEquals(DID_A, DeviceResourceIds.toDeviceId(ridAchild));

        NodeKey<?> nodeKey = ridA.nodeKeys().get(2);
        assertThat(nodeKey, is(instanceOf(ListKey.class)));
        assertThat(nodeKey.schemaId(), is(equalTo(new SchemaId(DEVICE_NAME, DCS_NAMESPACE))));
        ListKey listKey = (ListKey) nodeKey;
        assertThat(listKey.keyLeafs(), is(contains(new KeyLeaf(DEVICE_ID_KL_NAME, DCS_NAMESPACE, DID_A.toString()))));
    }

    @Test
    public void testDeviceRootNode() {
        assertTrue(DeviceResourceIds.isDeviceRootNode(ridA));

        assertFalse(DeviceResourceIds.isRootOrDevicesNode(ridA));
    }

    @Test
    public void testDeviceSubtreeTest() {
        ResourceId absDevice = ResourceId.builder()
                    .addBranchPointSchema(ROOT_NAME, DCS_NAMESPACE)
                    .addBranchPointSchema(DEVICES_NAME, DCS_NAMESPACE)
                    .addBranchPointSchema(DEVICE_NAME, DCS_NAMESPACE)
                    .addKeyLeaf(DEVICE_ID_KL_NAME, DCS_NAMESPACE, DID_A.toString())
                    .build();

        NodeKey<?> deviceKey = absDevice.nodeKeys().get(2);
        assertThat(deviceKey, is(instanceOf(ListKey.class)));
        assertThat(deviceKey.schemaId().namespace(), is(equalTo(DCS_NAMESPACE)));
        assertThat(deviceKey.schemaId().name(), is(equalTo(DEVICE_NAME)));
        assertTrue(DeviceResourceIds.isUnderDeviceRootNode(absDevice));
    }

    @Test
    public void testDeviceSubtreeEventTest() {
        // root relative ResourceId used by DynamicConfigEvent
        ResourceId evtDevice = ResourceId.builder()
                    .addBranchPointSchema(DEVICES_NAME, DCS_NAMESPACE)
                    .addBranchPointSchema(DEVICE_NAME, DCS_NAMESPACE)
                    .addKeyLeaf(DEVICE_ID_KL_NAME, DCS_NAMESPACE, DID_A.toString())
                    .build();

        NodeKey<?> deviceKey = evtDevice.nodeKeys().get(1);
        assertThat(deviceKey, is(instanceOf(ListKey.class)));
        assertThat(deviceKey.schemaId().namespace(), is(equalTo(DCS_NAMESPACE)));
        assertThat(deviceKey.schemaId().name(), is(equalTo(DEVICE_NAME)));
        assertTrue(DeviceResourceIds.isUnderDeviceRootNode(evtDevice));
    }

}
