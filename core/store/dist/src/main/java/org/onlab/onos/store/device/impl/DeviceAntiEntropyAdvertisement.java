package org.onlab.onos.store.device.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.cluster.messaging.AntiEntropyAdvertisement;

// TODO DeviceID needs to be changed to something like (ProviderID, DeviceID)
// TODO: Handle Port as part of these messages, or separate messages for Ports?

public class DeviceAntiEntropyAdvertisement
    extends AntiEntropyAdvertisement<DeviceId> {


    public DeviceAntiEntropyAdvertisement(NodeId sender,
            Map<DeviceId, Timestamp> advertisement) {
        super(sender, advertisement);
    }

    // May need to add ProviderID, etc.
    public static DeviceAntiEntropyAdvertisement create(
            NodeId self,
            Collection<VersionedValue<Device>> localValues) {

        Map<DeviceId, Timestamp> ads = new HashMap<>(localValues.size());
        for (VersionedValue<Device> e : localValues) {
            ads.put(e.entity().id(), e.timestamp());
        }
        return new DeviceAntiEntropyAdvertisement(self, ads);
    }

    // For serializer
    protected DeviceAntiEntropyAdvertisement() {}
}
