package org.onlab.onos.net.device;

import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.net.DeviceId;

/**
* Interface for feeding term information to a logical clock service
* that vends per device timestamps.
*/
public interface DeviceClockProviderService {

    /**
     * Updates the mastership term for the specified deviceId.
     *
     * @param deviceId device identifier.
     * @param term mastership term.
     */
    public void setMastershipTerm(DeviceId deviceId, MastershipTerm term);
}
