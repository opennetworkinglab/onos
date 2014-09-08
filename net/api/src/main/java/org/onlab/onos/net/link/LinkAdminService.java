package org.onlab.onos.net.link;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;

/**
 * Service for administering the inventory of infrastructure links.
 */
public interface LinkAdminService {

    /**
     * Removes all infrastructure links leading to and from the
     * specified connection point.
     *
     * @param connectPoint connection point
     */
    void removeLinks(ConnectPoint connectPoint);

    /**
     * Removes all infrastructure links leading to and from the
     * specified device.
     *
     * @param deviceId device identifier
     */
    void removeLinks(DeviceId deviceId);

}
