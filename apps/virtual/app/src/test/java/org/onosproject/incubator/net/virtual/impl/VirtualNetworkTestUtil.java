/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl;

import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.store.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;

import static org.onosproject.net.DeviceId.deviceId;

public class VirtualNetworkTestUtil extends TestDeviceParams {

    protected static final TenantId TID1 = TenantId.tenantId("tid1");
    protected static final TenantId TID2 = TenantId.tenantId("tid2");

    protected static final DeviceId VDID1 = deviceId("of:foo_v");
    protected static final DeviceId VDID2 = deviceId("of:bar_v");
    protected static final DeviceId VDID3 = deviceId("of:who_v");
    protected static final DeviceId VDID4 = deviceId("of:what_v");

    protected static final DeviceId PHYDID1 = deviceId("physical:1");
    protected static final DeviceId PHYDID2 = deviceId("physical:2");
    protected static final DeviceId PHYDID3 = deviceId("physical:3");
    protected static final DeviceId PHYDID4 = deviceId("physical:4");

    /**
     * Method to create the virtual network for further testing.
     *
     * @return virtual network
     */
    public static VirtualNetwork setupVirtualNetworkTopology(VirtualNetworkManager manager,
                                                             TenantId tenantId) {
        manager.registerTenantId(tenantId);
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(tenantId);

        VirtualDevice virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork.id(), VDID1);
        VirtualDevice virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork.id(), VDID2);
        VirtualDevice virtualDevice3 =
                manager.createVirtualDevice(virtualNetwork.id(), VDID3);
        VirtualDevice virtualDevice4 =
                manager.createVirtualDevice(virtualNetwork.id(), VDID4);

        ConnectPoint vcp1 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(1));
        ConnectPoint cp1 = new ConnectPoint(DID1, PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), vcp1.deviceId(), vcp1.port(), cp1);

        ConnectPoint vcp2 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(2));
        ConnectPoint cp2 = new ConnectPoint(DID1, PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), vcp2.deviceId(), vcp2.port(), cp2);

        ConnectPoint vcp3 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(3));
        ConnectPoint cp3 = new ConnectPoint(DID2, PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), vcp3.deviceId(), vcp3.port(), cp3);

        ConnectPoint vcp4 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(4));
        ConnectPoint cp4 = new ConnectPoint(DID2, PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), vcp4.deviceId(), vcp4.port(), cp4);

        ConnectPoint vcp5 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(5));
        ConnectPoint cp5 = new ConnectPoint(DID3, PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), vcp5.deviceId(), vcp5.port(), cp5);

        ConnectPoint vcp6 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(6));
        ConnectPoint cp6 = new ConnectPoint(DID3, PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), vcp6.deviceId(), vcp6.port(), cp6);

        DistributedVirtualNetworkStore virtualNetworkManagerStore =
                (DistributedVirtualNetworkStore) manager.store;
        VirtualLink link1 = manager.createVirtualLink(virtualNetwork.id(), vcp1, vcp3);
        virtualNetworkManagerStore.updateLink(link1, link1.tunnelId(), Link.State.ACTIVE);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), vcp3, vcp1);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);
        VirtualLink link3 = manager.createVirtualLink(virtualNetwork.id(), vcp4, vcp5);
        virtualNetworkManagerStore.updateLink(link3, link3.tunnelId(), Link.State.ACTIVE);
        VirtualLink link4 = manager.createVirtualLink(virtualNetwork.id(), vcp5, vcp4);
        virtualNetworkManagerStore.updateLink(link4, link4.tunnelId(), Link.State.ACTIVE);
        VirtualLink link5 = manager.createVirtualLink(virtualNetwork.id(), vcp2, vcp6);
        virtualNetworkManagerStore.updateLink(link5, link5.tunnelId(), Link.State.ACTIVE);
        VirtualLink link6 = manager.createVirtualLink(virtualNetwork.id(), vcp6, vcp2);
        virtualNetworkManagerStore.updateLink(link6, link6.tunnelId(), Link.State.ACTIVE);

        return virtualNetwork;
    }
}
