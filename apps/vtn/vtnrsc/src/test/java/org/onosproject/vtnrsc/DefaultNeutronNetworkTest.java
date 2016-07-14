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

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultNeutronNetwork class.
 */
public class DefaultNeutronNetworkTest {

    private String networkIdStr1 = "123";
    private String networkIdStr2 = "234";
    private String physicalNetworkStr = "1234";
    private String tenantIdStr = "345";
    private String segmentationIdStr = "1";
    private String name = "456";

    /**
     * Checks that the DefaultNeutronNetwork class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultTenantNetwork.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquality() {
        TenantNetworkId networkid1 = TenantNetworkId.networkId(networkIdStr1);
        TenantNetworkId networkid2 = TenantNetworkId.networkId(networkIdStr2);
        PhysicalNetwork physicalNetwork = PhysicalNetwork
                .physicalNetwork(physicalNetworkStr);
        TenantId tenantId = TenantId.tenantId(tenantIdStr);
        SegmentationId segmentationID = SegmentationId
                .segmentationId(segmentationIdStr);
        TenantNetwork p1 = new DefaultTenantNetwork(networkid1, name, false,
                                                    TenantNetwork.State.ACTIVE,
                                                    false, tenantId, false,
                                                    TenantNetwork.Type.LOCAL,
                                                    physicalNetwork,
                                                    segmentationID);
        TenantNetwork p2 = new DefaultTenantNetwork(networkid1, name, false,
                                                    TenantNetwork.State.ACTIVE,
                                                    false, tenantId, false,
                                                    TenantNetwork.Type.LOCAL,
                                                    physicalNetwork,
                                                    segmentationID);
        TenantNetwork p3 = new DefaultTenantNetwork(networkid2, name, false,
                                                    TenantNetwork.State.ACTIVE,
                                                    false, tenantId, false,
                                                    TenantNetwork.Type.LOCAL,
                                                    physicalNetwork,
                                                    segmentationID);
        new EqualsTester().addEqualityGroup(p1, p2).addEqualityGroup(p3)
                .testEquals();
    }
}
