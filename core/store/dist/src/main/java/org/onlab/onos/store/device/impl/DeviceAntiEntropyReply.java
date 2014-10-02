package org.onlab.onos.store.device.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.cluster.messaging.AntiEntropyReply;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class DeviceAntiEntropyReply
    extends AntiEntropyReply<DeviceId, VersionedValue<Device>> {


    public DeviceAntiEntropyReply(NodeId sender,
            Map<DeviceId, VersionedValue<Device>> suggestion,
            Set<DeviceId> request) {
        super(sender, suggestion, request);
    }

    /**
     * Creates a reply to Anti-Entropy advertisement.
     *
     * @param advertisement to respond to
     * @param self node identifier representing local node
     * @param localValues local values held on this node
     * @return reply message
     */
    public static DeviceAntiEntropyReply reply(
            DeviceAntiEntropyAdvertisement advertisement,
            NodeId self,
            Collection<VersionedValue<Device>> localValues
            ) {

        ImmutableMap<DeviceId, Timestamp> ads = advertisement.advertisement();

        ImmutableMap.Builder<DeviceId, VersionedValue<Device>>
            sug = ImmutableMap.builder();

        Set<DeviceId> req = new HashSet<>(ads.keySet());

        for (VersionedValue<Device> e : localValues) {
            final DeviceId id = e.entity().id();
            final Timestamp local = e.timestamp();
            final Timestamp theirs = ads.get(id);
            if (theirs == null) {
                // they don't have it, suggest
                sug.put(id, e);
                // don't need theirs
                req.remove(id);
            } else if (local.compareTo(theirs) < 0) {
                // they got older one, suggest
                sug.put(id, e);
                // don't need theirs
                req.remove(id);
            } else if (local.equals(theirs)) {
                // same, don't need theirs
                req.remove(id);
            }
        }

        return new DeviceAntiEntropyReply(self, sug.build(), req);
    }

    /**
     * Creates a reply to request for values held locally.
     *
     * @param requests message containing the request
     * @param self node identifier representing local node
     * @param localValues local valeds held on this node
     * @return reply message
     */
    public static DeviceAntiEntropyReply reply(
            DeviceAntiEntropyReply requests,
            NodeId self,
            Map<DeviceId, VersionedValue<Device>> localValues
            ) {

        Set<DeviceId> reqs = requests.request();

        Map<DeviceId, VersionedValue<Device>> requested = new HashMap<>(reqs.size());
        for (DeviceId id : reqs) {
            final VersionedValue<Device> value = localValues.get(id);
            if (value != null) {
                requested.put(id, value);
            }
        }

        Set<DeviceId> empty = ImmutableSet.of();
        return new DeviceAntiEntropyReply(self, requested, empty);
    }

    // For serializer
    protected DeviceAntiEntropyReply() {}
}
