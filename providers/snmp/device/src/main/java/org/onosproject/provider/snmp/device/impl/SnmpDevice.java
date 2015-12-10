/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.snmp.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;


import org.slf4j.Logger;

/**
 * This is a logical representation of actual SNMP device, carrying all the necessary information to connect and execute
 * SNMP operations.
 */
public class SnmpDevice {

    private final Logger log = getLogger(SnmpDevice.class);


    private static final int DEFAULT_SNMP_PORT = 161;

    private final String snmpHost;
    private int snmpPort = DEFAULT_SNMP_PORT;
    private final String community;
    private boolean reachable = false;

    private DeviceState deviceState = DeviceState.INVALID;

    protected SnmpDevice(String snmpHost, int snmpPort, String community) {

        this.snmpHost = checkNotNull(snmpHost, "SNMP Device IP cannot be null");
        this.snmpPort = checkNotNull(snmpPort, "SNMP Device snmp port cannot be null");
        this.community = community;
    }

    /**
     * This will try to connect to SNMP device.
     *
     */
    public void init() {

        reachable = true;
    }

    /**
     * This would return host IP and host Port, used by this particular SNMP Device.
     *
     * @return Device Information.
     */
    public String deviceInfo() {
        return new StringBuilder("host: ").append(snmpHost).append(". port: ")
                .append(snmpPort).toString();
    }

    /**
     * This will terminate the device connection.
     */
    public void disconnect() {
        log.info("disconnect");
        reachable = false;
    }

    /**
     * This api is intended to know whether the device is connected or not.
     *
     * @return true if connected
     */
    public boolean isReachable() {
        return reachable;
    }

    /**
     * This will return the IP used connect ssh on the device.
     *
     * @return SNMP Device IP
     */
    public String getSnmpHost() {
        return snmpHost;
    }

    /**
     * This will return the SSH Port used connect the device.
     *
     * @return SSH Port number
     */
    public int getSnmpPort() {
        return snmpPort;
    }

    public String getCommunity() {
        return community;
    }

    /**
     * Retrieve current state of the device.
     *
     * @return Current Device State
     */
    public DeviceState getDeviceState() {
        return deviceState;
    }

    /**
     * This is set the state information for the device.
     *
     * @param deviceState Next Device State
     */
    public void setDeviceState(DeviceState deviceState) {
        this.deviceState = deviceState;
    }

    /**
     * Check whether the device is in Active state.
     *
     * @return true if the device is Active
     */
    public boolean isActive() {
        return deviceState == DeviceState.ACTIVE;
    }

}
