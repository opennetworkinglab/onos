/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Collection;

import org.onosproject.cluster.NodeId;

/**
 * Message to request for other peers information.
 */
public class DeviceAntiEntropyRequest {

    private final NodeId sender;
    private final Collection<DeviceFragmentId> devices;
    private final Collection<PortFragmentId> ports;

    public DeviceAntiEntropyRequest(NodeId sender,
                                   Collection<DeviceFragmentId> devices,
                                   Collection<PortFragmentId> ports) {

        this.sender = checkNotNull(sender);
        this.devices = checkNotNull(devices);
        this.ports = checkNotNull(ports);
    }

    public NodeId sender() {
        return sender;
    }

    public Collection<DeviceFragmentId> devices() {
        return devices;
    }

    public Collection<PortFragmentId> ports() {
        return ports;
    }

    // For serializer
    @SuppressWarnings("unused")
    private DeviceAntiEntropyRequest() {
        this.sender = null;
        this.devices = null;
        this.ports = null;
    }
}
