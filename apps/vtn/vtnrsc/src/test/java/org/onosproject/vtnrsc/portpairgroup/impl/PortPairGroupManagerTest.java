/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.portpairgroup.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;
import java.util.List;
import java.util.LinkedList;

import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.DefaultPortPairGroup;
import org.onosproject.vtnrsc.util.VtnStorageServiceTest;
import org.onosproject.common.event.impl.TestEventDispatcher;

import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Unit tests for PortPairGroupManager class.
 */
public class PortPairGroupManagerTest {
    final PortPairGroupId portPairGroupId = PortPairGroupId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
    final TenantId tenantId = TenantId.tenantId("1");
    final String name = "PortPairGroup";
    final String description = "PortPairGroup";
    final List<PortPairId> portPairIdList = new LinkedList<PortPairId>();
    DefaultPortPairGroup.Builder portPairGroupBuilder = new DefaultPortPairGroup.Builder();
    PortPairGroupManager portPairGroupMgr = new PortPairGroupManager();
    PortPairGroup portPairGroup = null;
    private final VtnStorageServiceTest storageService = new VtnStorageServiceTest();

    /**
     * Checks the operation of createPortPairGroup() method.
     */
    @Test
    public void testCreatePortPairGroup() {
        // initialize port pair group manager
        portPairGroupMgr.storageService = storageService;
        injectEventDispatcher(portPairGroupMgr, new TestEventDispatcher());
        portPairGroupMgr.activate();

        // create port-pair-id list
        PortPairId portPairId = PortPairId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairIdList.add(portPairId);
        portPairId = PortPairId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairIdList.add(portPairId);

        // create port pair
        portPairGroup = portPairGroupBuilder.setId(portPairGroupId).setTenantId(tenantId).setName(name)
                .setDescription(description).setPortPairs(portPairIdList).build();
        assertThat(portPairGroupMgr.createPortPairGroup(portPairGroup), is(true));
    }

    /**
     * Checks the operation of exists() method.
     */
    @Test
    public void testExists() {
        testCreatePortPairGroup();
        assertThat(portPairGroupMgr.exists(portPairGroupId), is(true));
    }

    /**
     * Checks the operation of getPortPairGroupCount() method.
     */
    @Test
    public void testGetPortPairGroupCount() {
        testCreatePortPairGroup();
        assertThat(portPairGroupMgr.getPortPairGroupCount(), is(1));
    }

    /**
     * Checks the operation of getPortPairGroups() method.
     */
    @Test
    public void testGetPortPairGroups() {
        testCreatePortPairGroup();
        final Iterable<PortPairGroup> portPairGroupList = portPairGroupMgr.getPortPairGroups();
        assertThat(portPairGroupList, is(notNullValue()));
        assertThat(portPairGroupList.iterator().hasNext(), is(true));
    }

    /**
     * Checks the operation of getPortPairGroup() method.
     */
    @Test
    public void testGetPortPairGroup() {
        testCreatePortPairGroup();
        assertThat(portPairGroup, is(notNullValue()));
        assertThat(portPairGroupMgr.getPortPairGroup(portPairGroupId), is(portPairGroup));
    }

    /**
     * Checks the operation of updatePortPairGroup() method.
     */
    @Test
    public void testUpdatePortPairGroup() {
        // create a port pair group
        testCreatePortPairGroup();

        // new updates
        // create port-pair-id list
        final TenantId tenantId2 = TenantId.tenantId("2");
        final String name2 = "PortPairGroup2";
        final String description2 = "PortPairGroup2";
        final List<PortPairId> portPairIdList = new LinkedList<PortPairId>();
        PortPairId portPairId = PortPairId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairIdList.add(portPairId);
        portPairId = PortPairId.of("76666666-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairIdList.add(portPairId);

        // create port pair
        portPairGroup = portPairGroupBuilder.setId(portPairGroupId).setTenantId(tenantId2).setName(name2)
                .setDescription(description2).setPortPairs(portPairIdList).build();
        assertThat(portPairGroupMgr.updatePortPairGroup(portPairGroup), is(true));
    }

    /**
     * Checks the operation of removePortPairGroup() method.
     */
    @Test
    public void testRemovePortPairGroup() {
        testCreatePortPairGroup();
        assertThat(portPairGroupMgr.removePortPairGroup(portPairGroupId), is(true));
    }
}
