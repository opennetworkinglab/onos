/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.sfc.util;

import java.util.Iterator;

import org.onlab.packet.MacAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantRouter;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.event.VtnRscListener;
import org.onosproject.vtnrsc.service.VtnRscService;

/**
 * Provides implementation of the VtnRsc service.
 */
public class VtnRscAdapter implements VtnRscService {
    @Override
    public void addListener(VtnRscListener listener) {
    }

    @Override
    public void removeListener(VtnRscListener listener) {
    }

    @Override
    public SegmentationId getL3vni(TenantId tenantId) {
        return null;
    }

    @Override
    public Iterator<Device> getClassifierOfTenant(TenantId tenantId) {
        return null;
    }

    @Override
    public Iterator<Device> getSffOfTenant(TenantId tenantId) {
        return null;
    }

    @Override
    public MacAddress getGatewayMac(HostId hostId) {
        return null;
    }

    @Override
    public boolean isServiceFunction(VirtualPortId portId) {
        return false;
    }

    @Override
    public DeviceId getSfToSffMaping(VirtualPortId portId) {
        return DeviceId.deviceId("of:000000000000001");
    }

    @Override
    public void addDeviceIdOfOvsMap(VirtualPortId virtualPortId,
                                    TenantId tenantId, DeviceId deviceId) {
    }

    @Override
    public void removeDeviceIdOfOvsMap(Host host, TenantId tenantId,
                                       DeviceId deviceId) {
    }

    @Override
    public SegmentationId getL3vni(TenantRouter tenantRouter) {
        return null;
    }
}
