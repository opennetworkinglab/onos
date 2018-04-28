/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.device;

import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import java.util.List;

/**
 * Service for interacting with the inventory of infrastructure devices.
 */
public interface DeviceService
    extends ListenerService<DeviceEvent, DeviceListener> {

    /**
     * Returns the number of infrastructure devices known to the system.
     *
     * @return number of infrastructure devices
     */
    int getDeviceCount();

    /**
     * Returns the number of currently available devices known to the system.
     *
     * @return number of available devices
     */
    default int getAvailableDeviceCount() {
        return getDeviceCount();
    }

    /**
     * Returns a collection of the currently known infrastructure
     * devices.
     *
     * @return collection of devices
     */
    Iterable<Device> getDevices();

    /**
     * Returns a collection of the currently known infrastructure
     * devices by device type.
     *
     * @param type device type
     * @return collection of devices
     */
    Iterable<Device> getDevices(Device.Type type);

    /**
     * Returns an iterable collection of all devices
     * currently available to the system.
     *
     * @return device collection
     */
    Iterable<Device> getAvailableDevices();

    /**
     * Returns an iterable collection of all devices currently available to the system by device type.
     *
     * @param type device type
     * @return device collection
     */
    Iterable<Device> getAvailableDevices(Device.Type type);

    /**
     * Returns the device with the specified identifier.
     *
     * @param deviceId device identifier
     * @return device or null if one with the given identifier is not known
     */
    Device getDevice(DeviceId deviceId);

    /**
     * Returns the current mastership role for the specified device.
     *
     * @param deviceId device identifier
     * @return designated mastership role
     */
    //XXX do we want this method here when MastershipService already does?
    MastershipRole getRole(DeviceId deviceId);


    /**
     * Returns the list of ports associated with the device.
     *
     * @param deviceId device identifier
     * @return list of ports
     */
    List<Port> getPorts(DeviceId deviceId);

    /**
     * Returns the list of port statistics associated with the device.
     *
     * @param deviceId device identifier
     * @return list of port statistics
     */
    List<PortStatistics> getPortStatistics(DeviceId deviceId);

    /**
     * Returns the list of port delta statistics associated with the device.
     *
     * @param deviceId device identifier
     * @return list of port statistics
     */
    List<PortStatistics> getPortDeltaStatistics(DeviceId deviceId);

    /**
     * Returns the port specific port statistics associated with the device and port.
     *
     * @param deviceId device identifier
     * @param portNumber port identifier
     * @return port statistics of specified port
     */
    default PortStatistics getStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    /**
     * Returns the port specific port delta statistics associated with the device and port.
     *
     * @param deviceId device identifier
     * @param portNumber port identifier
     * @return port delta statistics of specified port
     */
    default PortStatistics getDeltaStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    /**
     * Returns the port with the specified number and hosted by the given device.
     *
     * @param deviceId   device identifier
     * @param portNumber port number
     * @return device port
     */
    Port getPort(DeviceId deviceId, PortNumber portNumber);

    /**
     * Returns the port with the specified connect point.
     *
     * @param cp connect point
     * @return device port
     */
    default Port getPort(ConnectPoint cp) {
        return getPort(cp.deviceId(), cp.port());
    }

    /**
     * Indicates whether or not the device is presently online and available.
     * Availability, unlike reachability, denotes whether ANY node in the
     * cluster can discover that this device is in an operational state,
     * this does not necessarily mean that there exists a node that can
     * control this device.
     *
     * @param deviceId device identifier
     * @return true if the device is available
     */
    boolean isAvailable(DeviceId deviceId);

    /**
     * Indicates how long ago the device connected or disconnected from this
     * controller instance.
     *
     * @param deviceId device identifier
     * @return a human readable string indicating the time since the device
     *          connected-to or disconnected-from this controller instance.
     */
    String localStatus(DeviceId deviceId);


    /**
     * Indicates the time at which the given device connected or disconnected
     * from this controller instance.
     *
     * @param deviceId device identifier
     * @return time offset in miliseconds from Epoch
     */
    long getLastUpdatedInstant(DeviceId deviceId);

}
