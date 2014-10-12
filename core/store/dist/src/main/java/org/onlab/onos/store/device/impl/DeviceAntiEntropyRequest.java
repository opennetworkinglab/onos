package org.onlab.onos.store.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import org.onlab.onos.cluster.NodeId;

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
