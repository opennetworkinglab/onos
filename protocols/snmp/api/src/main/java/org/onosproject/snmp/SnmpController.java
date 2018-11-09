/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.snmp;

import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.google.common.annotations.Beta;

import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

/**
 * Snmp Controller.
 */
@Beta
public interface SnmpController {

    /**
     * Return all the devices that this controller has notion of.
     * @return Set of all Snmp devices
     */
    Collection<SnmpDevice> getDevices();

    /**
     * Gets a device for a specific deviceId.
     * @param deviceId device id of the device
     * @return SnmpDevice for given deviceId
     */
    SnmpDevice getDevice(DeviceId deviceId);

    /**
     * Gets a device for a specific host address.
     *
     * @param uri URI of the device
     * @return SnmpDevice for given deviceId
     */
    SnmpDevice getDevice(URI uri);

    /**
     * Removes a specific device.
     * @param deviceId device id of the device to be removed
     */
    void removeDevice(DeviceId deviceId);

    /**
     * Add a snmp device.
     *
     * @param device device to add to this controller
     */
    void addDevice(SnmpDevice device);

    /**
     * Gets an Instance of ISnmpSession for a specific device.
     *
     * @param deviceId device to retrieve the session for.
     * @return ISnmp session.
     * @throws IOException if the session can't be established.
     * @deprecated 1.14.0 moved to snmp4j
     */
    @Deprecated
    ISnmpSession getSession(DeviceId deviceId) throws IOException;

    /**
     * Creates an error alarm if the interaction with the device failed.
     *
     * @param deviceId the device with a failed interaction
     * @return default alarm error
     */
    DefaultAlarm buildWalkFailedAlarm(DeviceId deviceId);


}
