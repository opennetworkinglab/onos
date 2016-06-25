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

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for DefaultPortPairGroup class.
 */
public class DefaultPortPairGroupTest {

    final PortPairGroupId portPairGroupId = PortPairGroupId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
    final TenantId tenantId = TenantId.tenantId("1");
    final String name = "PortPairGroup1";
    final String description = "PortPairGroup1";
    final List<PortPairId> portPairList = new LinkedList<PortPairId>();

    private PortPairGroup getPortPairGroup() {

        portPairList.clear();
        // Create same two port-pair-group objects.
        PortPairId portPairId = PortPairId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairList.add(portPairId);
        portPairId = PortPairId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairList.add(portPairId);

        DefaultPortPairGroup.Builder portPairGroupBuilder = new DefaultPortPairGroup.Builder();
        PortPairGroup portPairGroup = portPairGroupBuilder.setId(portPairGroupId).setTenantId(tenantId)
                .setName(name).setDescription(description).setPortPairs(portPairList).build();

        return portPairGroup;

    }

    /**
     * Checks that the DefaultPortPairGroup class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultPortPairGroup.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {

        final PortPairGroup portPairGroup1 = getPortPairGroup();

        DefaultPortPairGroup.Builder portPairGroupBuilder = new DefaultPortPairGroup.Builder();
        final PortPairGroup samePortPairGroup1 = getPortPairGroup();

        // Create different port-pair-group object.
        final PortPairGroupId portPairGroupId2 = PortPairGroupId.of("79999999-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId2 = TenantId.tenantId("2");
        final String name2 = "PortPairGroup2";
        final String description2 = "PortPairGroup2";
        // create port-pair-id list
        final List<PortPairId> portPairList2 = new LinkedList<PortPairId>();
        PortPairId portPairId = PortPairId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairList2.add(portPairId);
        portPairId = PortPairId.of("75555555-fc23-aeb6-f44b-56dc5e2fb3ae");
        portPairList2.add(portPairId);

        portPairGroupBuilder = new DefaultPortPairGroup.Builder();
        final PortPairGroup portPairGroup2 = portPairGroupBuilder.setId(portPairGroupId2).setTenantId(tenantId2)
                .setName(name2).setDescription(description2).setPortPairs(portPairList2).build();

        new EqualsTester().addEqualityGroup(portPairGroup1, samePortPairGroup1).addEqualityGroup(portPairGroup2)
        .testEquals();
    }

    /**
     * Checks the construction of a DefaultPortPairGroup object.
     */
    @Test
    public void testConstruction() {

        final PortPairGroup portPairGroup = getPortPairGroup();

        assertThat(portPairGroupId, is(portPairGroup.portPairGroupId()));
        assertThat(tenantId, is(portPairGroup.tenantId()));
        assertThat(name, is(portPairGroup.name()));
        assertThat(description, is(portPairGroup.description()));
        assertThat(portPairList, is(portPairGroup.portPairs()));
    }

    /**
     * Checks the port pair load map.
     */
    @Test
    public void testPortPairLod() {

        PortPairId portPairId = PortPairId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        final PortPairGroup portPairGroup = getPortPairGroup();
        int load1 = portPairGroup.getLoad(portPairId);
        portPairGroup.addLoad(portPairId);
        int load2 = portPairGroup.getLoad(portPairId);

        assertThat((load1 + 1), is(load2));
    }
}
