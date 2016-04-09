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
package org.onosproject.vtnrsc.portpair.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.DefaultPortPair;
import org.onosproject.vtnrsc.util.VtnStorageServiceTest;
import org.onosproject.common.event.impl.TestEventDispatcher;

import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Unit tests for PortPairManager class.
 */
public class PortPairManagerTest {
    final PortPairId portPairId = PortPairId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
    final TenantId tenantId = TenantId.tenantId("1");
    final String name = "PortPair";
    final String description = "PortPair";
    final String ingress = "d3333333-24fc-4fae-af4b-321c5e2eb3d1";
    final String egress = "a4444444-4a56-2a6e-cd3a-9dee4e2ec345";
    DefaultPortPair.Builder portPairBuilder = new DefaultPortPair.Builder();
    PortPairManager portPairMgr = new PortPairManager();
    PortPair portPair = null;
    private final VtnStorageServiceTest storageService = new VtnStorageServiceTest();

    /**
     * Checks the operation of createPortPair() method.
     */
    @Test
    public void testCreatePortPair() {
        // initialize port pair manager
        portPairMgr.storageService = storageService;
        injectEventDispatcher(portPairMgr, new TestEventDispatcher());
        portPairMgr.activate();

        // create port pair
        portPair = portPairBuilder.setId(portPairId).setTenantId(tenantId).setName(name)
                .setDescription(description).setIngress(ingress).setEgress(egress).build();
        assertThat(portPairMgr.createPortPair(portPair), is(true));
    }

    /**
     * Checks the operation of exists() method.
     */
    @Test
    public void testExists() {
        testCreatePortPair();
        assertThat(portPairMgr.exists(portPairId), is(true));
    }

    /**
     * Checks the operation of getPortPairCount() method.
     */
    @Test
    public void testGetPortPairCount() {
        testCreatePortPair();
        assertThat(portPairMgr.getPortPairCount(), is(1));
    }

    /**
     * Checks the operation of getPortPairs() method.
     */
    @Test
    public void testGetPortPairs() {
        testCreatePortPair();
        final Iterable<PortPair> portPairList = portPairMgr.getPortPairs();
        assertThat(portPairList, is(notNullValue()));
        assertThat(portPairList.iterator().hasNext(), is(true));
    }

    /**
     * Checks the operation of getPortPair() method.
     */
    @Test
    public void testGetPortPair() {
        testCreatePortPair();
        assertThat(portPair, is(notNullValue()));
        assertThat(portPairMgr.getPortPair(portPairId), is(portPair));
    }

    /**
     * Checks the operation of updatePortPair() method.
     */
    @Test
    public void testUpdatePortPair() {
        // create a port pair
        testCreatePortPair();

        // new updates
        final TenantId tenantId2 = TenantId.tenantId("2");
        final String name2 = "PortPair2";
        final String description2 = "PortPair2";
        final String ingress2 = "d5555555-24fc-4fae-af4b-321c5e2eb3d1";
        final String egress2 = "a6666666-4a56-2a6e-cd3a-9dee4e2ec345";
        portPair = portPairBuilder.setId(portPairId).setTenantId(tenantId2).setName(name2)
                .setDescription(description2).setIngress(ingress2).setEgress(egress2).build();
        assertThat(portPairMgr.updatePortPair(portPair), is(true));
    }

    /**
     * Checks the operation of removePortPair() method.
     */
    @Test
    public void testRemovePortPair() {
        testCreatePortPair();
        assertThat(portPairMgr.removePortPair(portPairId), is(true));
    }
}
