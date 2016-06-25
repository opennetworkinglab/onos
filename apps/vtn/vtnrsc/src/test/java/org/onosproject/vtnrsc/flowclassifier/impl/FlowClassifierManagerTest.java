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
package org.onosproject.vtnrsc.flowclassifier.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

import org.onlab.packet.IpPrefix;

import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.DefaultFlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.util.VtnStorageServiceTest;
import org.onosproject.common.event.impl.TestEventDispatcher;

import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Unit tests for FlowClassifierManager class.
 */
public class FlowClassifierManagerTest {

    final String name = "FlowClassifier";
    final String description = "FlowClassifier";
    final String ethType = "IPv4";
    final String protocol = "udp";
    final int minSrcPortRange = 1024;
    final int maxSrcPortRange = 5000;
    final int minDstPortRange = 1024;
    final int maxDstPortRange = 5000;
    final FlowClassifierId flowClassifierId = FlowClassifierId.of("71111111-fc23-aeb6-f44b-56dc5e2fb3ae");
    final TenantId tenantId = TenantId.tenantId("8");
    final IpPrefix srcIpPrefix = IpPrefix.valueOf("0.0.0.0/0");
    final IpPrefix dstIpPrefix = IpPrefix.valueOf("100.100.100.100/0");
    final VirtualPortId virtualSrcPort = VirtualPortId.portId("100");
    final VirtualPortId virtualDstPort = VirtualPortId.portId("200");
    DefaultFlowClassifier.Builder flowClassifierBuilder = new DefaultFlowClassifier.Builder();
    FlowClassifierManager flowClassifierMgr = new FlowClassifierManager();
    FlowClassifier flowClassifier = null;
    private final VtnStorageServiceTest storageService = new VtnStorageServiceTest();

    /**
     * Checks the operation of createFlowClassifier() method.
     */
    @Test
    public void testCreateFlowClassifier() {
        // initialize flow classifier manager
        flowClassifierMgr.storageService = storageService;
        injectEventDispatcher(flowClassifierMgr, new TestEventDispatcher());
        flowClassifierMgr.activate();

        // create flow classifier
        flowClassifier = flowClassifierBuilder.setFlowClassifierId(flowClassifierId).setTenantId(tenantId)
                .setName(name).setDescription(description).setEtherType(ethType).setProtocol(protocol)
                .setMinSrcPortRange(minSrcPortRange).setMaxSrcPortRange(maxSrcPortRange)
                .setMinDstPortRange(minDstPortRange).setMaxDstPortRange(maxDstPortRange).setSrcIpPrefix(srcIpPrefix)
                .setDstIpPrefix(dstIpPrefix).setSrcPort(virtualSrcPort).setDstPort(virtualDstPort).build();
        assertThat(flowClassifierMgr.createFlowClassifier(flowClassifier), is(true));
    }

    /**
     * Checks the operation of exists() method.
     */
    @Test
    public void testExists() {
        testCreateFlowClassifier();
        assertThat(flowClassifierMgr.exists(flowClassifierId), is(true));
    }

    /**
     * Checks the operation of getFlowClassifierCount() method.
     */
    @Test
    public void testGetFlowClassifierCount() {
        testCreateFlowClassifier();
        assertThat(flowClassifierMgr.getFlowClassifierCount(), is(1));
    }

    /**
     * Checks the operation of getFlowClassifiers() method.
     */
    @Test
    public void testGetFlowClassifiers() {
        testCreateFlowClassifier();
        final Iterable<FlowClassifier> flowClassifierList = flowClassifierMgr.getFlowClassifiers();
        assertThat(flowClassifierList, is(notNullValue()));
        assertThat(flowClassifierList.iterator().hasNext(), is(true));
    }

    /**
     * Checks the operation of getFlowClassifier() method.
     */
    @Test
    public void testGetFlowClassifier() {
        testCreateFlowClassifier();
        assertThat(flowClassifier, is(notNullValue()));
        assertThat(flowClassifierMgr.getFlowClassifier(flowClassifierId), is(flowClassifier));
    }

    /**
     * Checks the operation of updateFlowClassifier() method.
     */
    @Test
    public void testUpdateFlowClassifier() {
        // create a flow classifier
        testCreateFlowClassifier();

        // new updates
        final String name2 = "FlowClassifier2";
        final String description2 = "FlowClassifier2";
        final String ethType2 = "IPv6";
        final String protocol2 = "tcp";
        final TenantId tenantId2 = TenantId.tenantId("10");
        final VirtualPortId virtualSrcPort2 = VirtualPortId.portId("300");
        final VirtualPortId virtualDstPort2 = VirtualPortId.portId("400");
        flowClassifier = flowClassifierBuilder.setFlowClassifierId(flowClassifierId)
                .setTenantId(tenantId2).setName(name2).setDescription(description2).setEtherType(ethType2)
                .setProtocol(protocol2).setMinSrcPortRange(minSrcPortRange).setMaxSrcPortRange(maxSrcPortRange)
                .setMinDstPortRange(minDstPortRange).setMaxDstPortRange(maxDstPortRange).setSrcIpPrefix(srcIpPrefix)
                .setDstIpPrefix(dstIpPrefix).setSrcPort(virtualSrcPort2).setDstPort(virtualDstPort2).build();
        assertThat(flowClassifierMgr.updateFlowClassifier(flowClassifier), is(true));
    }

    /**
     * Checks the operation of removeFlowClassifier() method.
     */
    @Test
    public void testRemoveFlowClassifier() {
        testCreateFlowClassifier();
        assertThat(flowClassifierMgr.removeFlowClassifier(flowClassifierId), is(true));
    }
}
