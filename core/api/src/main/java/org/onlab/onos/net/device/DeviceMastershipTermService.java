package org.onlab.onos.net.device;

import org.onlab.onos.net.DeviceId;

// TODO give me a better name
/**
 * Service to obtain mastership term information.
 */
public interface DeviceMastershipTermService {

    // TBD: manage/increment per device mastership change
    //      or increment on any change
    /**
     * Returns the term number of mastership change occurred for given device.
     *
     * @param deviceId the identifier of the device
     * @return current master's term.
     */
    DeviceMastershipTerm getMastershipTerm(DeviceId deviceId);
}
