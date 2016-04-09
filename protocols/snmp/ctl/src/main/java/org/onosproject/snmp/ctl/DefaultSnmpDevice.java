/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.snmp.ctl;

import org.onosproject.net.DeviceId;
import org.onosproject.snmp.SnmpDevice;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This is a logical representation of actual SNMP device, carrying all the necessary information to connect and execute
 * SNMP operations.
 */
public class DefaultSnmpDevice implements SnmpDevice {

    private final Logger log = getLogger(DefaultSnmpDevice.class);


    private static final int DEFAULT_SNMP_PORT = 161;

    private static final String SCHEME = "snmp";

    private final String snmpHost;
    private final DeviceId deviceId;
    private int snmpPort = DEFAULT_SNMP_PORT;
    private final String username;
    //Community is a conventional name for password in SNMP.
    private final String community;
    private boolean reachable = false;


    public DefaultSnmpDevice(String snmpHost, int snmpPort, String username, String community) {

        this.snmpHost = checkNotNull(snmpHost, "SNMP Device IP cannot be null");
        this.snmpPort = checkNotNull(snmpPort, "SNMP Device port cannot be null");
        this.username = username;
        this.community = community;
        this.deviceId = createDeviceId();
        reachable = true;
    }

    @Override
    public String deviceInfo() {
        return new StringBuilder("host: ").append(snmpHost).append(". port: ")
                .append(snmpPort).toString();
    }

    @Override
    public void disconnect() {
        log.info("disconnect");
        reachable = false;
    }

    @Override
    public boolean isReachable() {
        return reachable;
    }

    @Override
    public String getSnmpHost() {
        return snmpHost;
    }


    @Override
    public int getSnmpPort() {
        return snmpPort;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getCommunity() {
        return community;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    private DeviceId createDeviceId() {
        String additionalSsp = new StringBuilder(
                snmpHost).append(":")
                .append(snmpPort).toString();
        try {
            return DeviceId.deviceId(new URI(SCHEME, additionalSsp,
                                             null));
        } catch (URISyntaxException e) {
            log.error("Syntax Error while creating URI for the device: "
                              + additionalSsp
                              + " couldn't persist the device onto the store", e);
            throw new IllegalArgumentException("Can't create device ID from " + additionalSsp, e);
        }
    }
}
