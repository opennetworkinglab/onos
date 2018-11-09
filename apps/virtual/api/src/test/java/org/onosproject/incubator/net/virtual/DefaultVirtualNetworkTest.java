/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.TenantId;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test of the default virtual network model entity.
 */
public class DefaultVirtualNetworkTest {
    final String tenantIdValue1 = "TENANT_ID1";
    final String tenantIdValue2 = "TENANT_ID2";

    /**
     * Checks that the DefaultVirtualNetwork class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultVirtualNetwork.class);
    }

    @Test
    public void testEquality() {
        DefaultVirtualNetwork network1 =
                new DefaultVirtualNetwork(NetworkId.networkId(0), TenantId.tenantId(tenantIdValue1));
        DefaultVirtualNetwork network2 =
                new DefaultVirtualNetwork(NetworkId.networkId(0), TenantId.tenantId(tenantIdValue1));
        DefaultVirtualNetwork network3 =
                new DefaultVirtualNetwork(NetworkId.networkId(0), TenantId.tenantId(tenantIdValue2));
        DefaultVirtualNetwork network4 =
                new DefaultVirtualNetwork(NetworkId.networkId(1), TenantId.tenantId(tenantIdValue2));

        new EqualsTester().addEqualityGroup(network1, network2).addEqualityGroup(network3)
                .addEqualityGroup(network4).testEquals();
    }
}
