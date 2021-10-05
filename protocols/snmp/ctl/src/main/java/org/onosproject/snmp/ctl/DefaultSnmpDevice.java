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
package org.onosproject.snmp.ctl;

import org.onosproject.net.DeviceId;
import org.onosproject.snmp.SnmpDevice;
import org.onosproject.snmp.SnmpDeviceConfig;
import org.slf4j.Logger;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
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

    private static final String SCHEME = "snmp";

    private final String snmpHost;
    private final DeviceId deviceId;
    private final int snmpPort;
    private final int notificationPort;
    private final String username;
    //Community is a conventional name for password in SNMP.
    private final String community;
    private boolean reachable = false;
    private final String protocol;
    private final String notificationProtocol;
    private final int version;

    private Snmp session;

    public DefaultSnmpDevice(String snmpHost, int snmpPort,
                             String username, String community) {
        this.protocol = GenericAddress.TYPE_UDP;
        this.snmpHost = checkNotNull(snmpHost, "SNMP Device IP cannot be null");
        this.snmpPort = snmpPort;
        this.notificationProtocol = GenericAddress.TYPE_UDP;
        this.notificationPort = 0;
        this.username = username;
        this.community = community;
        this.deviceId = createDeviceId();
        this.version = SnmpConstants.version2c;
        initializeSession();
    }

    public DefaultSnmpDevice(SnmpDeviceConfig snmpDeviceConfig) {
        checkNotNull(snmpDeviceConfig.ip(), "SNMP Device IP address cannot be null");
        this.protocol = snmpDeviceConfig.protocol();
        this.notificationProtocol = snmpDeviceConfig.notificationProtocol();
        this.snmpHost = checkNotNull(snmpDeviceConfig.ip().toString(), "SNMP Device IP cannot be null");
        this.snmpPort = snmpDeviceConfig.port();
        this.notificationPort = snmpDeviceConfig.notificationPort();
        this.username = snmpDeviceConfig.username();
        this.community = snmpDeviceConfig.password();
        this.deviceId = createDeviceId();
        this.version = snmpDeviceConfig.version();
        initializeSession();
    }

    private void initializeSession() {
        reachable = true;
        try {
            TransportMapping transport;
            if (protocol.equals(GenericAddress.TYPE_TCP)) {
                transport = new DefaultTcpTransportMapping();
            } else {
                transport = new DefaultUdpTransportMapping();
            }
            transport.listen();
            session = new Snmp(transport);
        } catch (IOException e) {
            log.error("Failed to connect to device: ", e);
        }
    }

    @Override
    public String deviceInfo() {
        return new StringBuilder("host: ").append(snmpHost).append(". port: ")
                .append(snmpPort).toString();
    }

    @Override
    public Snmp getSession() {
        return session;
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
    public int getNotificationPort() {
        return notificationPort;
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
    public int getVersion() {
        return version;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getNotificationProtocol() {
        return notificationProtocol;
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
