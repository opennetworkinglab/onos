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

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultPortPair class.
 */
public class DefaultPortPairTest {
    /**
     * Checks that the DefaultPortPair class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultPortPair.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        // Create same two port pair objects.
        final PortPairId portPairId = PortPairId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortPair1";
        final String description = "PortPair1";
        final String ingress = "d3333333-24fc-4fae-af4b-321c5e2eb3d1";
        final String egress = "a4444444-4a56-2a6e-cd3a-9dee4e2ec345";

        DefaultPortPair.Builder portPairBuilder = new DefaultPortPair.Builder();
        final PortPair portPair1 = portPairBuilder.setId(portPairId).setTenantId(tenantId).setName(name)
                .setDescription(description).setIngress(ingress).setEgress(egress).build();

        portPairBuilder = new DefaultPortPair.Builder();
        final PortPair samePortPair1 = portPairBuilder.setId(portPairId).setTenantId(tenantId).setName(name)
                .setDescription(description).setIngress(ingress).setEgress(egress).build();

        // Create different port pair object.
        final PortPairId portPairId2 = PortPairId.of("79999999-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId2 = TenantId.tenantId("2");
        final String name2 = "PortPair2";
        final String description2 = "PortPair2";
        final String ingress2 = "d5555555-24fc-4fae-af4b-321c5e2eb3d1";
        final String egress2 = "a6666666-4a56-2a6e-cd3a-9dee4e2ec345";

        portPairBuilder = new DefaultPortPair.Builder();
        final PortPair portPair2 = portPairBuilder.setId(portPairId2).setTenantId(tenantId2).setName(name2)
                .setDescription(description2).setIngress(ingress2).setEgress(egress2).build();

        new EqualsTester().addEqualityGroup(portPair1, samePortPair1).addEqualityGroup(portPair2).testEquals();
    }

    /**
     * Checks the construction of a DefaultPortPair object.
     */
    @Test
    public void testConstruction() {
        final PortPairId portPairId = PortPairId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
        final TenantId tenantId = TenantId.tenantId("1");
        final String name = "PortPair";
        final String description = "PortPair";
        final String ingress = "d3333333-24fc-4fae-af4b-321c5e2eb3d1";
        final String egress = "a4444444-4a56-2a6e-cd3a-9dee4e2ec345";

        DefaultPortPair.Builder portPairBuilder = new DefaultPortPair.Builder();
        final PortPair portPair = portPairBuilder.setId(portPairId).setTenantId(tenantId).setName(name)
                .setDescription(description).setIngress(ingress).setEgress(egress).build();

        assertThat(portPairId, is(portPair.portPairId()));
        assertThat(tenantId, is(portPair.tenantId()));
        assertThat(name, is(portPair.name()));
        assertThat(description, is(portPair.description()));
        assertThat(ingress, is(portPair.ingress()));
        assertThat(egress, is(portPair.egress()));
    }
}
