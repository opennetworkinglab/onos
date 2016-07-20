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
package org.onosproject.vtnrsc.portchain.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;
import java.util.List;
import java.util.LinkedList;

import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.DefaultPortChain;
import org.onosproject.vtnrsc.DefaultFlowClassifier;
import org.onosproject.vtnrsc.util.VtnStorageServiceTest;
import org.onosproject.common.event.impl.TestEventDispatcher;

import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Unit tests for PortChainManager class.
 */
public class PortChainManagerTest {
    final PortChainId portChainId = PortChainId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
    final TenantId tenantId = TenantId.tenantId("1");
    final String name = "PortChain";
    final String description = "PortChain";
    final List<PortPairGroupId> portPairGroupList = new LinkedList<PortPairGroupId>();
    final List<FlowClassifierId> flowClassifierList = new LinkedList<FlowClassifierId>();
    DefaultPortChain.Builder portChainBuilder = new DefaultPortChain.Builder();
    DefaultFlowClassifier.Builder flowClassifierBuilder = new DefaultFlowClassifier.Builder();
    PortChainManager portChainMgr = new PortChainManager();
    PortChain portChain = null;
    private final VtnStorageServiceTest storageService = new VtnStorageServiceTest();

    /**
     * Checks the operation of createPortChain() method.
     */
    @Test
    public void testCreatePortChain() {
        // initialize port chain manager
        portChainMgr.storageService = storageService;
        injectEventDispatcher(portChainMgr, new TestEventDispatcher());
        portChainMgr.activate();

        // create list of Port Pair Groups.
        PortPairGroupId portPairGroupId = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairGroupList.add(portPairGroupId);
        portPairGroupId = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3af");
        portPairGroupList.add(portPairGroupId);

        // create list of Flow classifiers.
        FlowClassifierId flowClassifierId = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
        flowClassifierList.add(flowClassifierId);
        flowClassifierId = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3af");
        flowClassifierList.add(flowClassifierId);

        // create port chain
        portChain = portChainBuilder.setId(portChainId).setTenantId(tenantId).setName(name).setDescription(description)
                .setPortPairGroups(portPairGroupList).setFlowClassifiers(flowClassifierList).build();
        assertThat(portChainMgr.createPortChain(portChain), is(true));
    }

    /**
     * Checks the operation of exists() method.
     */
    @Test
    public void testExists() {
        testCreatePortChain();
        assertThat(portChainMgr.exists(portChainId), is(true));
    }

    /**
     * Checks the operation of getPortChainCount() method.
     */
    @Test
    public void testGetPortChainCount() {
        testCreatePortChain();
        assertThat(portChainMgr.getPortChainCount(), is(1));
    }

    /**
     * Checks the operation of getPortChains() method.
     */
    @Test
    public void testGetPortChains() {
        testCreatePortChain();
        final Iterable<PortChain> portChainList = portChainMgr.getPortChains();
        assertThat(portChainList, is(notNullValue()));
        assertThat(portChainList.iterator().hasNext(), is(true));
    }

    /**
     * Checks the operation of getPortChain() method.
     */
    @Test
    public void testGetPortChain() {
        testCreatePortChain();
        assertThat(portChain, is(notNullValue()));
        assertThat(portChainMgr.getPortChain(portChainId), is(portChain));
    }

    /**
     * Checks the operation of updatePortChain() method.
     */
    @Test
    public void testUpdatePortChain() {
        // create a port chain
        testCreatePortChain();

        // new updates
        final TenantId tenantId2 = TenantId.tenantId("2");
        final String name2 = "PortChain2";
        final String description2 = "PortChain2";
        // create list of Port Pair Groups.
        final List<PortPairGroupId> portPairGroupList = new LinkedList<PortPairGroupId>();
        PortPairGroupId portPairGroupId = PortPairGroupId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairGroupList.add(portPairGroupId);
        portPairGroupId = PortPairGroupId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3af");
        portPairGroupList.add(portPairGroupId);
        // create list of Flow classifiers.
        final List<FlowClassifierId> flowClassifierList = new LinkedList<FlowClassifierId>();
        FlowClassifierId flowClassifierId = FlowClassifierId.of("76666666-fc23-aeb6-f44b-56dc5e2fb3ae");
        flowClassifierList.add(flowClassifierId);
        flowClassifierId = FlowClassifierId.of("76666666-fc23-aeb6-f44b-56dc5e2fb3af");
        flowClassifierList.add(flowClassifierId);
        portChain = portChainBuilder.setId(portChainId).setTenantId(tenantId2).setName(name2)
                .setDescription(description2).setPortPairGroups(portPairGroupList)
                .setFlowClassifiers(flowClassifierList).build();
        assertThat(portChainMgr.updatePortChain(portChain), is(true));
    }

    /**
     * Checks the operation of removePortChain() method.
     */
    @Test
    public void testRemovePortChain() {
        testCreatePortChain();
        assertThat(portChainMgr.removePortChain(portChainId), is(true));
    }
}
