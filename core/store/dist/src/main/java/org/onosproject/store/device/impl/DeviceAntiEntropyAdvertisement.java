/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.store.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.store.Timestamp;


/**
 * Device Advertisement message.
 */
public class DeviceAntiEntropyAdvertisement {

    private final NodeId sender;
    private final Map<DeviceFragmentId, Timestamp> deviceFingerPrints;
    private final Map<PortFragmentId, Timestamp> portFingerPrints;
    private final Map<DeviceId, Timestamp> offline;


    public DeviceAntiEntropyAdvertisement(NodeId sender,
                Map<DeviceFragmentId, Timestamp> devices,
                Map<PortFragmentId, Timestamp> ports,
                Map<DeviceId, Timestamp> offline) {
        this.sender = checkNotNull(sender);
        this.deviceFingerPrints = checkNotNull(devices);
        this.portFingerPrints = checkNotNull(ports);
        this.offline = checkNotNull(offline);
    }

    public NodeId sender() {
        return sender;
    }

    public Map<DeviceFragmentId, Timestamp> deviceFingerPrints() {
        return deviceFingerPrints;
    }

    public Map<PortFragmentId, Timestamp> ports() {
        return portFingerPrints;
    }

    public Map<DeviceId, Timestamp> offline() {
        return offline;
    }

    // For serializer
    @SuppressWarnings("unused")
    private DeviceAntiEntropyAdvertisement() {
        this.sender = null;
        this.deviceFingerPrints = null;
        this.portFingerPrints = null;
        this.offline = null;
    }
}
