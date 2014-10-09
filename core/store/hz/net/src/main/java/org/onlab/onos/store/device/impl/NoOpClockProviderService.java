package org.onlab.onos.store.device.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceClockProviderService;
import org.onlab.onos.net.device.DeviceMastershipTerm;

// FIXME: Code clone in onos-core-trivial, onos-core-hz-net
/**
 * Dummy implementation of {@link DeviceClockProviderService}.
 */
@Component(immediate = true)
@Service
public class NoOpClockProviderService implements DeviceClockProviderService {

    @Override
    public void setDeviceMastershipTerm(DeviceId deviceId, DeviceMastershipTerm term) {
    }
}
