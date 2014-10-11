package org.onlab.onos.store.trivial.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceClockProviderService;

//FIXME: Code clone in onos-core-trivial, onos-core-hz-net
/**
 * Dummy implementation of {@link DeviceClockProviderService}.
 */
@Component(immediate = true)
@Service
public class NoOpClockProviderService implements DeviceClockProviderService {

    @Override
    public void setMastershipTerm(DeviceId deviceId, MastershipTerm term) {
    }
}
