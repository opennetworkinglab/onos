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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.l2monitoring.cfm.Component.IdPermissionType;
import org.onosproject.incubator.net.l2monitoring.cfm.Component.MhfCreationType;

public class ComponentTest {
    Component c1;

    @Before
    public void setUp() throws Exception {
        c1 =  DefaultComponent.builder(1)
        .idPermission(IdPermissionType.MANAGE)
        .mhfCreationType(MhfCreationType.EXPLICIT)
        .addToVidList(VlanId.vlanId((short) 1))
        .addToVidList(VlanId.vlanId((short) 2))
        .addToVidList(VlanId.vlanId((short) 3))
        .build();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testComponentId() {
        assertEquals(1, c1.componentId());
    }

    @Test
    public void testVidList() {
        assertEquals(3, c1.vidList().size());
    }

    @Test
    public void testMhfCreationType() {
        assertEquals(MhfCreationType.EXPLICIT, c1.mhfCreationType());
    }

    @Test
    public void testIdPermission() {
        assertEquals(IdPermissionType.MANAGE, c1.idPermission());
    }

}
