package org.onlab.onos.net.device;

/**
 * Representation of a relationship role of a controller instance to a device
 * or a region of network environment.
 */
public enum DeviceMastershipRole {

    /**
     * Represents a relationship where the controller instance is the master
     * to a device or a region of network environment.
     */
    MASTER,

    /**
     * Represents a relationship where the controller instance is the standby,
     * i.e. potential master to a device or a region of network environment.
     */
    STANDBY,

    /**
     * Represents that the controller instance is not eligible to be the master
     * to a device or a region of network environment.
     */
    NONE

}
