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
package org.onosproject.sfc.manager.impl;

import org.junit.Test;

import java.util.List;
import java.util.LinkedList;

import org.onlab.packet.IpPrefix;
import org.onosproject.sfc.manager.SfcService;
import org.onosproject.vtnrsc.DefaultPortChain;
import org.onosproject.vtnrsc.DefaultPortPair;
import org.onosproject.vtnrsc.DefaultPortPairGroup;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.DefaultFlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.FlowClassifier;

/**
 * Unit tests for SfcManager class.
 */
public class SfcManagerTest {
    /**
     * Checks the operation of onPortPairCreated() method.
     */
    @Test
    public void testOnPortPairCreated() {
        final PortPairId portPairId = PortPairId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortPair";
        final String description = "PortPair";
        final String ingress = "d3333333-24fc-4fae-af4b-321c5e2eb3d1";
        final String egress = "a4444444-4a56-2a6e-cd3a-9dee4e2ec345";
        DefaultPortPair.Builder portPairBuilder = new DefaultPortPair.Builder();
        PortPair portPair = null;
        SfcService sfcService = new SfcManager();

        // create port pair
        portPair = portPairBuilder.setId(portPairId).setTenantId(tenantId).setName(name).setDescription(description)
                .setIngress(ingress).setEgress(egress).build();
        sfcService.onPortPairCreated(portPair);
    }

    /**
     * Checks the operation of onPortPairDeleted() method.
     */
    @Test
    public void testOnPortPairDeleted() {
        final PortPairId portPairId = PortPairId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortPair";
        final String description = "PortPair";
        final String ingress = "d3333333-24fc-4fae-af4b-321c5e2eb3d1";
        final String egress = "a4444444-4a56-2a6e-cd3a-9dee4e2ec345";
        DefaultPortPair.Builder portPairBuilder = new DefaultPortPair.Builder();
        PortPair portPair = null;
        SfcService sfcService = new SfcManager();

        // create port pair
        portPair = portPairBuilder.setId(portPairId).setTenantId(tenantId).setName(name).setDescription(description)
                .setIngress(ingress).setEgress(egress).build();
        sfcService.onPortPairDeleted(portPair);
    }

    /**
     * Checks the operation of onPortPairGroupCreated() method.
     */
    @Test
    public void testOnPortPairGroupCreated() {
        final PortPairGroupId portPairGroupId = PortPairGroupId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortPairGroup";
        final String description = "PortPairGroup";
        final List<PortPairId> portPairIdList = new LinkedList<PortPairId>();
        DefaultPortPairGroup.Builder portPairGroupBuilder = new DefaultPortPairGroup.Builder();
        PortPairGroup portPairGroup = null;
        SfcService sfcService = new SfcManager();

        // create port-pair-id list
        PortPairId portPairId = PortPairId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairIdList.add(portPairId);
        portPairId = PortPairId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairIdList.add(portPairId);

        // create port pair
        portPairGroup = portPairGroupBuilder.setId(portPairGroupId).setTenantId(tenantId).setName(name)
                .setDescription(description).setPortPairs(portPairIdList).build();
        sfcService.onPortPairGroupCreated(portPairGroup);
    }

    /**
     * Checks the operation of onPortPairGroupDeleted() method.
     */
    @Test
    public void testOnPortPairGroupDeleted() {
        final PortPairGroupId portPairGroupId = PortPairGroupId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortPairGroup";
        final String description = "PortPairGroup";
        final List<PortPairId> portPairIdList = new LinkedList<PortPairId>();
        DefaultPortPairGroup.Builder portPairGroupBuilder = new DefaultPortPairGroup.Builder();
        PortPairGroup portPairGroup = null;
        SfcService sfcService = new SfcManager();

        // create port-pair-id list
        PortPairId portPairId = PortPairId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairIdList.add(portPairId);
        portPairId = PortPairId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairIdList.add(portPairId);

        // create port pair
        portPairGroup = portPairGroupBuilder.setId(portPairGroupId).setTenantId(tenantId).setName(name)
                .setDescription(description).setPortPairs(portPairIdList).build();
        sfcService.onPortPairGroupDeleted(portPairGroup);
    }

    /**
     * Checks the operation of onFlowClassifierCreated() method.
     */
    @Test
    public void testOnFlowClassifierCreated() {
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
        FlowClassifier flowClassifier = null;
        SfcService sfcService = new SfcManager();

        // create flow classifier
        flowClassifier = flowClassifierBuilder.setFlowClassifierId(flowClassifierId).setTenantId(tenantId)
                .setName(name).setDescription(description).setEtherType(ethType).setProtocol(protocol)
                .setMinSrcPortRange(minSrcPortRange).setMaxSrcPortRange(maxSrcPortRange)
                .setMinDstPortRange(minDstPortRange).setMaxDstPortRange(maxDstPortRange).setSrcIpPrefix(srcIpPrefix)
                .setDstIpPrefix(dstIpPrefix).setSrcPort(virtualSrcPort).setDstPort(virtualDstPort).build();
        sfcService.onFlowClassifierCreated(flowClassifier);
    }

    /**
     * Checks the operation of onFlowClassifierDeleted() method.
     */
    @Test
    public void testOnFlowClassifierDeleted() {
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
        FlowClassifier flowClassifier = null;
        SfcService sfcService = new SfcManager();

        // create flow classifier
        flowClassifier = flowClassifierBuilder.setFlowClassifierId(flowClassifierId).setTenantId(tenantId)
                .setName(name).setDescription(description).setEtherType(ethType).setProtocol(protocol)
                .setMinSrcPortRange(minSrcPortRange).setMaxSrcPortRange(maxSrcPortRange)
                .setMinDstPortRange(minDstPortRange).setMaxDstPortRange(maxDstPortRange).setSrcIpPrefix(srcIpPrefix)
                .setDstIpPrefix(dstIpPrefix).setSrcPort(virtualSrcPort).setDstPort(virtualDstPort).build();
        sfcService.onFlowClassifierDeleted(flowClassifier);
    }

    /**
     * Checks the operation of onPortChainCreated() method.
     */
    @Test
    public void testOnPortChainCreated() {
        final PortChainId portChainId = PortChainId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortChain";
        final String description = "PortChain";
        final List<PortPairGroupId> portPairGroupList = new LinkedList<PortPairGroupId>();
        final List<FlowClassifierId> flowClassifierList = new LinkedList<FlowClassifierId>();
        DefaultPortChain.Builder portChainBuilder = new DefaultPortChain.Builder();
        DefaultFlowClassifier.Builder flowClassifierBuilder = new DefaultFlowClassifier.Builder();
        PortChain portChain = null;
        SfcService sfcService = new SfcManager();

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
        //sfcService.onPortChainCreated(portChain);
    }

    /**
     * Checks the operation of onPortChainDeleted() method.
     */
    @Test
    public void testOnPortChainDeleted() {
        final PortChainId portChainId = PortChainId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortChain";
        final String description = "PortChain";
        final List<PortPairGroupId> portPairGroupList = new LinkedList<PortPairGroupId>();
        final List<FlowClassifierId> flowClassifierList = new LinkedList<FlowClassifierId>();
        DefaultPortChain.Builder portChainBuilder = new DefaultPortChain.Builder();
        DefaultFlowClassifier.Builder flowClassifierBuilder = new DefaultFlowClassifier.Builder();
        PortChain portChain = null;
        SfcService sfcService = new SfcManager();

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
        //sfcService.onPortChainDeleted(portChain);
    }
}
