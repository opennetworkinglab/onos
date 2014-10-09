package org.onlab.onos.store.device.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceMastershipTerm;
import org.onlab.onos.store.ClockProviderService;

// FIXME: Code clone in onos-core-trivial, onos-core-hz-net
/**
 * Dummy implementation of {@link ClockProviderService}.
 */
@Component(immediate = true)
@Service
public class NoOpClockProviderService implements ClockProviderService {

    @Override
    public void setMastershipTerm(DeviceId deviceId, DeviceMastershipTerm term) {
    }
}
