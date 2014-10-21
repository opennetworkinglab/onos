package org.onlab.onos.mastership;

import java.util.Set;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.cluster.RoleInfo;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;

/**
 * Service responsible for determining the controller instance mastership of
 * a device in a clustered environment. This is the central authority for
 * determining mastership, but is not responsible for actually applying it
 * to the devices; this falls on the device service.
 */
public interface MastershipService {

    /**
     * Returns the role of the local node for the specified device, without
     * triggering master selection.
     *
     * @param deviceId the the identifier of the device
     * @return role of the current node
     */
    MastershipRole getLocalRole(DeviceId deviceId);

    /**
     * Returns the mastership status of the local controller for a given
     * device forcing master selection if necessary.
     *
     * @param deviceId the the identifier of the device
     * @return the role of this controller instance
     */
    MastershipRole requestRoleFor(DeviceId deviceId);

    /**
     * Abandons mastership of the specified device on the local node thus
     * forcing selection of a new master. If the local node is not a master
     * for this device, no master selection will occur.
     *
     * @param deviceId the identifier of the device
     */
    void relinquishMastership(DeviceId deviceId);

    /**
     * Returns the current master for a given device.
     *
     * @param deviceId the identifier of the device
     * @return the ID of the master controller for the device
     */
    NodeId getMasterFor(DeviceId deviceId);

    /**
     * Returns controllers connected to a given device, in order of
     * preference. The first entry in the list is the current master.
     *
     * @param deviceId the identifier of the device
     * @return a list of controller IDs
     */
    RoleInfo getNodesFor(DeviceId deviceId);

    /**
     * Returns the devices for which a controller is master.
     *
     * @param nodeId the ID of the controller
     * @return a set of device IDs
     */
    Set<DeviceId> getDevicesOf(NodeId nodeId);

    /**
     * Returns the mastership term service for getting read-only
     * term information.
     *
     * @return the MastershipTermService for this mastership manager
     */
    MastershipTermService requestTermService();

    /**
     * Adds the specified mastership change listener.
     *
     * @param listener the mastership listener
     */
    void addListener(MastershipListener listener);

    /**
     * Removes the specified mastership change listener.
     *
     * @param listener the mastership listener
     */
    void removeListener(MastershipListener listener);

}
