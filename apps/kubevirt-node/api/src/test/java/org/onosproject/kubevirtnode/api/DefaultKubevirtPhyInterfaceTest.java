/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.api;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.DeviceId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultOpenstackPhyInterface.
 */
public class DefaultKubevirtPhyInterfaceTest {

    private static final String NETWORK_1 = "mgmtnetwork";
    private static final String NETWORK_2 = "oamnetwork";
    private static final String INTERFACE_1 = "eth3";
    private static final String INTERFACE_2 = "eth4";
    private static final DeviceId BRIDGE_1 = DeviceId.deviceId("phys1");
    private static final DeviceId BRIDGE_2 = DeviceId.deviceId("phys2");

    private static final KubevirtPhyInterface KV_PHY_INTF_1 = createPhysIntf(
            NETWORK_1, INTERFACE_1, BRIDGE_1);
    private static final KubevirtPhyInterface KV_PHY_INTF_2 = createPhysIntf(
            NETWORK_1, INTERFACE_1, BRIDGE_1);
    private static final KubevirtPhyInterface KV_PHY_INTF_3 = createPhysIntf(
            NETWORK_2, INTERFACE_2, BRIDGE_2);

    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(KV_PHY_INTF_1, KV_PHY_INTF_2)
                .addEqualityGroup(KV_PHY_INTF_3)
                .testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultKubevirtPhyInterface phyIntf = (DefaultKubevirtPhyInterface)
                KV_PHY_INTF_1;

        assertThat(phyIntf.network(), is(NETWORK_1));
        assertThat(phyIntf.intf(), is(INTERFACE_1));
    }

    private static KubevirtPhyInterface createPhysIntf(String network,
                                                       String intf, DeviceId physBridge) {
        return DefaultKubevirtPhyInterface.builder()
                .network(network)
                .intf(intf)
                .physBridge(physBridge)
                .build();
    }
}
