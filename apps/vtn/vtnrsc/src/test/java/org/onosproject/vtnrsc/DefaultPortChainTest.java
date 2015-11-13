/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultPortChain class.
 */
public class DefaultPortChainTest {
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
        final PortChainId portChainId = PortChainId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortChain1";
        final String description = "PortChain1";
        // create list of Port Pair Groups.
        final List<PortPairGroupId> portPairGroups = new LinkedList<PortPairGroupId>();
        PortPairGroupId portPairGroupId = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairGroups.add(portPairGroupId);
        portPairGroupId = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3af");
        portPairGroups.add(portPairGroupId);
        // create list of Flow classifiers.
        final List<FlowClassifierId> flowClassifiers = new LinkedList<FlowClassifierId>();
        FlowClassifierId flowClassifierId = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
        flowClassifiers.add(flowClassifierId);
        flowClassifierId = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3af");
        flowClassifiers.add(flowClassifierId);

        DefaultPortChain.Builder portChainBuilder = new DefaultPortChain.Builder();
        final PortChain portChain1 = portChainBuilder.setId(portChainId).setTenantId(tenantId).setName(name)
                .setDescription(description).setPortPairGroups(portPairGroups).setFlowClassifiers(flowClassifiers)
                .build();

        portChainBuilder = new DefaultPortChain.Builder();
        final PortChain samePortChain1 = portChainBuilder.setId(portChainId).setTenantId(tenantId).setName(name)
                .setDescription(description).setPortPairGroups(portPairGroups).setFlowClassifiers(flowClassifiers)
                .build();

        // Create different port chain object.
        final PortChainId portChainId2 = PortChainId.of("79999999-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId2 = TenantId.tenantId("2");
        final String name2 = "PortChain2";
        final String description2 = "PortChain2";
        // create list of Port Pair Groups.
        final List<PortPairGroupId> portPairGroups2 = new LinkedList<PortPairGroupId>();
        portPairGroupId = PortPairGroupId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairGroups2.add(portPairGroupId);
        portPairGroupId = PortPairGroupId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3af");
        portPairGroups2.add(portPairGroupId);
        // create list of Flow classifiers.
        final List<FlowClassifierId> flowClassifiers2 = new LinkedList<FlowClassifierId>();
        flowClassifierId = FlowClassifierId.of("76666666-fc23-aeb6-f44b-56dc5e2fb3ae");
        flowClassifiers2.add(flowClassifierId);
        flowClassifierId = FlowClassifierId.of("76666666-fc23-aeb6-f44b-56dc5e2fb3af");
        flowClassifiers2.add(flowClassifierId);

        portChainBuilder = new DefaultPortChain.Builder();
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
        final PortChainId portChainId = PortChainId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortChain";
        final String description = "PortChain";
        // create list of Port Pair Groups.
        final List<PortPairGroupId> portPairGroups = new LinkedList<PortPairGroupId>();
        PortPairGroupId portPairGroupId = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairGroups.add(portPairGroupId);
        portPairGroupId = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3af");
        portPairGroups.add(portPairGroupId);
        // create list of Flow classifiers.
        final List<FlowClassifierId> flowClassifiers = new LinkedList<FlowClassifierId>();
        FlowClassifierId flowClassifierId = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
        flowClassifiers.add(flowClassifierId);
        flowClassifierId = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3af");
        flowClassifiers.add(flowClassifierId);

        DefaultPortChain.Builder portChainBuilder = new DefaultPortChain.Builder();
        final PortChain portChain = portChainBuilder.setId(portChainId).setTenantId(tenantId).setName(name)
                .setDescription(description).setPortPairGroups(portPairGroups).setFlowClassifiers(flowClassifiers)
                .build();

        assertThat(portChainId, is(portChain.portChainId()));
        assertThat(tenantId, is(portChain.tenantId()));
        assertThat(name, is(portChain.name()));
        assertThat(description, is(portChain.description()));
        assertThat(portPairGroups, is(portChain.portPairGroups()));
        assertThat(flowClassifiers, is(portChain.flowClassifiers()));
    }
}
