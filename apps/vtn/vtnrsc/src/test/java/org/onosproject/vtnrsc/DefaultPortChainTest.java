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
package org.onosproject.vtnrsc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;

/**
 * Unit tests for DefaultPortChain class.
 */
public class DefaultPortChainTest {

    final PortChainId portChainId = PortChainId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
    final TenantId tenantId = TenantId.tenantId("1");
    final String name = "PortChain";
    final String description = "PortChain";
    final List<PortPairGroupId> portPairGroups = new LinkedList<PortPairGroupId>();
    final List<FlowClassifierId> flowClassifiers = new LinkedList<FlowClassifierId>();

    private PortChain getPortChain() {

        portPairGroups.clear();
        flowClassifiers.clear();
        // create list of Port Pair Groups.
        PortPairGroupId portPairGroupId = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairGroups.add(portPairGroupId);
        portPairGroupId = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3af");
        portPairGroups.add(portPairGroupId);
        // create list of Flow classifiers.
        FlowClassifierId flowClassifierId = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
        flowClassifiers.add(flowClassifierId);
        flowClassifierId = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3af");
        flowClassifiers.add(flowClassifierId);

        DefaultPortChain.Builder portChainBuilder = new DefaultPortChain.Builder();
        final PortChain portChain = portChainBuilder.setId(portChainId).setTenantId(tenantId).setName(name)
                .setDescription(description).setPortPairGroups(portPairGroups).setFlowClassifiers(flowClassifiers)
                .build();

        return portChain;
    }

    /**
     * Checks that the DefaultPortChain class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultPortChain.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {

        // Create same two port chain objects.
        final PortChain portChain1 = getPortChain();
        final PortChain samePortChain1 = getPortChain();

        // Create different port chain object.
        final PortChainId portChainId2 = PortChainId.of("79999999-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId2 = TenantId.tenantId("2");
        final String name2 = "PortChain2";
        final String description2 = "PortChain2";
        // create list of Port Pair Groups.
        final List<PortPairGroupId> portPairGroups2 = new LinkedList<PortPairGroupId>();
        PortPairGroupId portPairGroupId = PortPairGroupId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairGroups2.add(portPairGroupId);
        portPairGroupId = PortPairGroupId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3af");
        portPairGroups2.add(portPairGroupId);
        // create list of Flow classifiers.
        final List<FlowClassifierId> flowClassifiers2 = new LinkedList<FlowClassifierId>();
        FlowClassifierId flowClassifierId = FlowClassifierId.of("76666666-fc23-aeb6-f44b-56dc5e2fb3ae");
        flowClassifiers2.add(flowClassifierId);
        flowClassifierId = FlowClassifierId.of("76666666-fc23-aeb6-f44b-56dc5e2fb3af");
        flowClassifiers2.add(flowClassifierId);

        DefaultPortChain.Builder portChainBuilder = new DefaultPortChain.Builder();
        final PortChain portChain2 = portChainBuilder.setId(portChainId2).setTenantId(tenantId2).setName(name2)
                .setDescription(description2).setPortPairGroups(portPairGroups2).setFlowClassifiers(flowClassifiers2)
                .build();

        new EqualsTester().addEqualityGroup(portChain1, samePortChain1).addEqualityGroup(portChain2).testEquals();
    }

    /**
     * Checks the construction of a DefaultPortChain object.
     */
    @Test
    public void testConstruction() {

        final PortChain portChain = getPortChain();

        assertThat(portChainId, is(portChain.portChainId()));
        assertThat(tenantId, is(portChain.tenantId()));
        assertThat(name, is(portChain.name()));
        assertThat(description, is(portChain.description()));
        assertThat(portPairGroups, is(portChain.portPairGroups()));
        assertThat(flowClassifiers, is(portChain.flowClassifiers()));
    }

    /**
     * Verifies the load balance data structures.
     */
    @Test
    public void testLoadBalanceIdMap() {

        final PortChain portChain = getPortChain();

        final FiveTuple fiveTuple1 = DefaultFiveTuple.builder().setIpSrc(IpAddress.valueOf("1.1.1.1"))
                .setIpDst(IpAddress.valueOf("2.2.2.2"))
                .setPortSrc(PortNumber.portNumber(500))
                .setPortDst(PortNumber.portNumber(1000))
                .setProtocol(IPv4.PROTOCOL_TCP)
                .build();

        PortPairId portPairId = PortPairId.of("a4444444-4a56-2a6e-cd3a-9dee4e2ec345");

        final LoadBalanceId id1 = LoadBalanceId.of((byte) 1);

        List<PortPairId> tempPath = Lists.newArrayList();
        tempPath.add(portPairId);

        portChain.addLoadBalancePath(fiveTuple1, id1, tempPath);
        Set<FiveTuple> keys = portChain.getLoadBalanceIdMapKeys();
        List<PortPairId> path = portChain.getLoadBalancePath(fiveTuple1);

        assertThat(portChain.getLoadBalancePath(fiveTuple1), is(path));
        assertThat(portChain.getLoadBalancePath(id1), is(path));
        assertThat(portChain.getLoadBalanceId(fiveTuple1), is(id1));
        assertThat(keys.contains(fiveTuple1), is(true));
        assertThat(path.contains(portPairId), is(true));
    }

    /**
     * Verifies sfc classifiers.
     */
    @Test
    public void testSfcClassifier() {
        final PortChain portChain = getPortChain();

        final LoadBalanceId id1 = LoadBalanceId.of((byte) 1);
        List<DeviceId> classifierList = Lists.newArrayList();
        DeviceId deviceId1 = DeviceId.deviceId("of:000000001");
        classifierList.add(deviceId1);
        DeviceId deviceId2 = DeviceId.deviceId("of:000000002");
        classifierList.add(deviceId2);
        portChain.addSfcClassifiers(id1, classifierList);

        assertThat(portChain.getSfcClassifiers(id1).contains(deviceId1), is(true));
    }

    /**
     * Verifies sfc forwarders.
     */
    @Test
    public void testSfcForwarder() {
        final PortChain portChain = getPortChain();

        final LoadBalanceId id1 = LoadBalanceId.of((byte) 1);
        List<DeviceId> forwarderList = Lists.newArrayList();
        DeviceId deviceId1 = DeviceId.deviceId("of:000000001");
        forwarderList.add(deviceId1);
        DeviceId deviceId2 = DeviceId.deviceId("of:000000002");
        forwarderList.add(deviceId2);
        portChain.addSfcForwarders(id1, forwarderList);

        assertThat(portChain.getSfcForwarders(id1).contains(deviceId1), is(true));
    }
}
