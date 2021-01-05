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

import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

import static org.onosproject.net.Device.Type.SWITCH;

/**
 * Provides a set of test KubevirtNode parameters for use with KubevirtNode related tests.
 */
public abstract class KubevirtNodeTest {

    public static Device createDevice(long devIdNum) {
        return new DefaultDevice(new ProviderId("of", "foo"),
                DeviceId.deviceId(String.format("of:%016d", devIdNum)),
                SWITCH,
                "manufacturer",
                "hwVersion",
                "swVersion",
                "serialNumber",
                new ChassisId(1));
    }

    public static KubevirtNode createNode(String hostname, KubevirtNode.Type type,
                                          Device intgBridge, IpAddress ipAddr,
                                          KubevirtNodeState state) {
        return DefaultKubevirtNode.builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(intgBridge.id())
                .tunBridge(intgBridge.id())
                .managementIp(ipAddr)
                .dataIp(ipAddr)
                .state(state)
                .build();
    }
}
