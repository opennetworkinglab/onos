package org.onlab.onos.store.device.impl.peermsg;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.Timestamp;


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
