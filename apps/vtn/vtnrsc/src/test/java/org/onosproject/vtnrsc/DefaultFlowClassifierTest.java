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

import org.junit.Test;
import org.onlab.packet.IpPrefix;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultFlowClassifier class.
 */
public class DefaultFlowClassifierTest {
    /**
     * Checks that the DefaultFlowClassifier class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultFlowClassifier.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        // Create same two flow classifier objects.
        final String name = "FlowClassifier1";
        final String description = "FlowClassifier1";
        final String ethType = "IPv4";
        final String protocol = "tcp";
        final int priority = 65535;
        final int minSrcPortRange = 5;
        final int maxSrcPortRange = 10;
        final int minDstPortRange = 5;
        final int maxDstPortRange = 10;
        final FlowClassifierId flowClassifierId = FlowClassifierId.of("78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final IpPrefix srcIpPrefix = IpPrefix.valueOf("0.0.0.0/0");
        final IpPrefix dstIpPrefix = IpPrefix.valueOf("10.10.10.10/0");
        final VirtualPortId virtualSrcPort = VirtualPortId.portId("1");
        final VirtualPortId virtualDstPort = VirtualPortId.portId("2");

        DefaultFlowClassifier.Builder flowClassifierBuilder = new DefaultFlowClassifier.Builder();
        final FlowClassifier flowClassifier1 = flowClassifierBuilder.setFlowClassifierId(flowClassifierId)
                .setTenantId(tenantId).setName(name).setDescription(description).setEtherType(ethType)
                .setProtocol(protocol).setPriority(priority).setMinSrcPortRange(minSrcPortRange)
                .setMaxSrcPortRange(maxSrcPortRange).setMinDstPortRange(minDstPortRange)
                .setMaxDstPortRange(maxDstPortRange).setSrcIpPrefix(srcIpPrefix).setDstIpPrefix(dstIpPrefix)
                .setSrcPort(virtualSrcPort).setDstPort(virtualDstPort).build();

        flowClassifierBuilder = new DefaultFlowClassifier.Builder();
        final FlowClassifier sameAsFlowClassifier1 = flowClassifierBuilder.setFlowClassifierId(flowClassifierId)
                .setTenantId(tenantId).setName(name).setDescription(description).setEtherType(ethType)
                .setProtocol(protocol).setPriority(priority).setMinSrcPortRange(minSrcPortRange)
                .setMaxSrcPortRange(maxSrcPortRange).setMinDstPortRange(minDstPortRange)
                .setMaxDstPortRange(maxDstPortRange).setSrcIpPrefix(srcIpPrefix).setDstIpPrefix(dstIpPrefix)
                .setSrcPort(virtualSrcPort).setDstPort(virtualDstPort).build();

        // Create different classifier object.
        final String name2 = "FlowClassifier2";
        final String description2 = "FlowClassifier2";
        final String ethType2 = "IPv6";
        final String protocol2 = "udp";
        final int priority2 = 50000;
        final int minSrcPortRange2 = 5;
        final int maxSrcPortRange2 = 10;
        final int minDstPortRange2 = 5;
        final int maxDstPortRange2 = 10;
        final FlowClassifierId flowClassifierId2 = FlowClassifierId.of("78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId2 = TenantId.tenantId("2");
        final IpPrefix srcIpPrefix2 = IpPrefix.valueOf("0.0.0.0/0");
        final IpPrefix dstIpPrefix2 = IpPrefix.valueOf("10.10.10.10/0");
        final VirtualPortId virtualSrcPort2 = VirtualPortId.portId("3");
        final VirtualPortId virtualDstPort2 = VirtualPortId.portId("4");

        DefaultFlowClassifier.Builder flowClassifierBuilder3 = new DefaultFlowClassifier.Builder();
        final FlowClassifier flowClassifier2 = flowClassifierBuilder3.setFlowClassifierId(flowClassifierId2)
                .setTenantId(tenantId2).setName(name2).setDescription(description2).setEtherType(ethType2)
                .setProtocol(protocol2).setMinSrcPortRange(minSrcPortRange2).setMaxSrcPortRange(maxSrcPortRange2)
                .setMinDstPortRange(minDstPortRange2).setMaxDstPortRange(maxDstPortRange2).setSrcIpPrefix(srcIpPrefix2)
                .setDstIpPrefix(dstIpPrefix2).setSrcPort(virtualSrcPort2).setDstPort(virtualDstPort2)
                .setPriority(priority2).build();

        new EqualsTester().addEqualityGroup(flowClassifier1, sameAsFlowClassifier1).addEqualityGroup(flowClassifier2)
                .testEquals();
    }

    /**
     * Checks the construction of a DefaultFlowClassifier object.
     */
    @Test
    public void testConstruction() {
        final String name = "FlowClassifier";
        final String description = "FlowClassifier";
        final String ethType = "IPv4";
        final String protocol = "tcp";
        final int priority = 30000;
        final int minSrcPortRange = 5;
        final int maxSrcPortRange = 10;
        final int minDstPortRange = 5;
        final int maxDstPortRange = 10;
        final FlowClassifierId flowClassifierId = FlowClassifierId.of("78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final IpPrefix srcIpPrefix = IpPrefix.valueOf("0.0.0.0/0");
        final IpPrefix dstIpPrefix = IpPrefix.valueOf("10.10.10.10/0");
        final VirtualPortId virtualSrcPort = VirtualPortId.portId("1");
        final VirtualPortId virtualDstPort = VirtualPortId.portId("2");

        DefaultFlowClassifier.Builder flowClassifierBuilder = new DefaultFlowClassifier.Builder();
        final FlowClassifier flowClassifier = flowClassifierBuilder.setFlowClassifierId(flowClassifierId)
                .setTenantId(tenantId).setName(name).setDescription(description).setEtherType(ethType)
                .setProtocol(protocol).setMinSrcPortRange(minSrcPortRange).setMaxSrcPortRange(maxSrcPortRange)
                .setMinDstPortRange(minDstPortRange).setMaxDstPortRange(maxDstPortRange).setSrcIpPrefix(srcIpPrefix)
                .setDstIpPrefix(dstIpPrefix).setSrcPort(virtualSrcPort).setDstPort(virtualDstPort)
                .setPriority(priority).build();

        assertThat(flowClassifierId, is(flowClassifier.flowClassifierId()));
        assertThat(tenantId, is(flowClassifier.tenantId()));
        assertThat(name, is(flowClassifier.name()));
        assertThat(description, is(flowClassifier.description()));
        assertThat(ethType, is(flowClassifier.etherType()));
        assertThat(protocol, is(flowClassifier.protocol()));
        assertThat(priority, is(flowClassifier.priority()));
        assertThat(minSrcPortRange, is(flowClassifier.minSrcPortRange()));
        assertThat(maxSrcPortRange, is(flowClassifier.maxSrcPortRange()));
        assertThat(minDstPortRange, is(flowClassifier.minDstPortRange()));
        assertThat(maxDstPortRange, is(flowClassifier.maxDstPortRange()));
        assertThat(srcIpPrefix, is(flowClassifier.srcIpPrefix()));
        assertThat(dstIpPrefix, is(flowClassifier.dstIpPrefix()));
        assertThat(virtualSrcPort, is(flowClassifier.srcPort()));
        assertThat(virtualDstPort, is(flowClassifier.dstPort()));
    }
}
