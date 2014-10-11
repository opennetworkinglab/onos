package org.onlab.onos.store.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.store.common.impl.Timestamped;

// FIXME: consider removing this class
public final class InitDeviceDescs
    implements ConcurrentInitializer<DeviceDescriptions> {

    private final Timestamped<DeviceDescription> deviceDesc;

    public InitDeviceDescs(Timestamped<DeviceDescription> deviceDesc) {
        this.deviceDesc = checkNotNull(deviceDesc);
    }
    @Override
    public DeviceDescriptions get() throws ConcurrentException {
        return new DeviceDescriptions(deviceDesc);
    }
}