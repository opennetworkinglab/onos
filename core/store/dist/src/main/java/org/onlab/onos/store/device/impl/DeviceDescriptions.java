package org.onlab.onos.store.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.DefaultAnnotations.union;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.SparseAnnotations;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DefaultPortDescription;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.common.impl.Timestamped;

/*
 * Collection of Description of a Device and Ports, given from a Provider.
 */
class DeviceDescriptions {

    private volatile Timestamped<DeviceDescription> deviceDesc;

    private final ConcurrentMap<PortNumber, Timestamped<PortDescription>> portDescs;

    public DeviceDescriptions(Timestamped<DeviceDescription> desc) {
        this.deviceDesc = checkNotNull(desc);
        this.portDescs = new ConcurrentHashMap<>();
    }

    public Timestamp getLatestTimestamp() {
        Timestamp latest = deviceDesc.timestamp();
        for (Timestamped<PortDescription> desc : portDescs.values()) {
            if (desc.timestamp().compareTo(latest) > 0) {
                latest = desc.timestamp();
            }
        }
        return latest;
    }

    public Timestamped<DeviceDescription> getDeviceDesc() {
        return deviceDesc;
    }

    public Timestamped<PortDescription> getPortDesc(PortNumber number) {
        return portDescs.get(number);
    }

    public Map<PortNumber, Timestamped<PortDescription>> getPortDescs() {
        return Collections.unmodifiableMap(portDescs);
    }

    /**
     * Puts DeviceDescription, merging annotations as necessary.
     *
     * @param newDesc new DeviceDescription
     */
    public void putDeviceDesc(Timestamped<DeviceDescription> newDesc) {
        Timestamped<DeviceDescription> oldOne = deviceDesc;
        Timestamped<DeviceDescription> newOne = newDesc;
        if (oldOne != null) {
            SparseAnnotations merged = union(oldOne.value().annotations(),
                                             newDesc.value().annotations());
            newOne = new Timestamped<DeviceDescription>(
                    new DefaultDeviceDescription(newDesc.value(), merged),
                    newDesc.timestamp());
        }
        deviceDesc = newOne;
    }

    /**
     * Puts PortDescription, merging annotations as necessary.
     *
     * @param newDesc new PortDescription
     */
    public void putPortDesc(Timestamped<PortDescription> newDesc) {
        Timestamped<PortDescription> oldOne = portDescs.get(newDesc.value().portNumber());
        Timestamped<PortDescription> newOne = newDesc;
        if (oldOne != null) {
            SparseAnnotations merged = union(oldOne.value().annotations(),
                                             newDesc.value().annotations());
            newOne = new Timestamped<PortDescription>(
                    new DefaultPortDescription(newDesc.value(), merged),
                    newDesc.timestamp());
        }
        portDescs.put(newOne.value().portNumber(), newOne);
    }
}
