package org.onlab.onos.cluster;

/**
 * Service responsible for determining the controller instance mastership of
 * a device in a clustered environment. This is the central authority for
 * determining mastership, but is not responsible for actually applying it
 * to the devices; this falls on the device service.
 */
public interface MastershipService {

    // InstanceId getMasterFor(DeviceId deviceId)
    // Set<DeviceId> getDevicesOf(InstanceId instanceId);

    // MastershipRole requestRoleFor(DeviceId deviceId);

    // addListener/removeLister(MastershipListener listener);
        // types of events would be MASTER_CHANGED (subject ==> deviceId; master ==> instanceId)

}
