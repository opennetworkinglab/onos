package org.onosproject.igmp;

import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Information about an igmp enabled device.
 */
public class IgmpDeviceData {

    private static final String DEVICE_ID_MISSING = "Device ID cannot be null";

    private final DeviceId deviceId;

    public IgmpDeviceData(DeviceId deviceId) {
        this.deviceId = checkNotNull(deviceId, DEVICE_ID_MISSING);
    }

    /**
     * Retrieves the access device ID.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }
}
