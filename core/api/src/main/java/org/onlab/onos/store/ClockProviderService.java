package org.onlab.onos.store;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceMastershipTerm;

//TODO: Consider renaming to DeviceClockProviderService?
/**
* Interface for feeding term information to a logical clock service
* that vends per device timestamps.
*/
public interface ClockProviderService {

    /**
     * Updates the mastership term for the specified deviceId.
     *
     * @param deviceId device identifier.
     * @param term mastership term.
     */
    public void setMastershipTerm(DeviceId deviceId, DeviceMastershipTerm term);
}
