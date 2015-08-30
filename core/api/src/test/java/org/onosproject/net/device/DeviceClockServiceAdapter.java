package org.onosproject.net.device;

import org.onosproject.net.DeviceId;
import org.onosproject.store.Timestamp;

/**
 * Test adapter for device clock service.
 */
public class DeviceClockServiceAdapter implements DeviceClockService {

    @Override
    public boolean isTimestampAvailable(DeviceId deviceId) {
        return false;
    }

    @Override
    public Timestamp getTimestamp(DeviceId deviceId) {
        return null;
    }

}
