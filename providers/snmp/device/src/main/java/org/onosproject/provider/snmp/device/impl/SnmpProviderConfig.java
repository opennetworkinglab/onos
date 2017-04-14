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
package org.onosproject.provider.snmp.device.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;

import java.util.Set;

/**
 * Configuration decoder for SNMP provider.
 * @deprecated 1.10.0 Kingfisher
 */
@Deprecated
@Beta
public class SnmpProviderConfig extends Config<ApplicationId> {

    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String IP = "ip";
    private static final int DEFAULT_TCP_PORT = 830;
    private static final String PORT = "port";
    private static final String NAME = "username";
    private static final String PASSWORD = "password";

    /**
     * Retrieves a set of SnmpDeviceInfo containing all the device
     * configuration pertaining to the SNMP device provider.
     * @return set of device configurations.
     *
     * @throws ConfigException if configuration can't be read
     */
    public Set<SnmpDeviceInfo> getDevicesInfo() throws ConfigException {
        Set<SnmpDeviceInfo> deviceInfos = Sets.newHashSet();

        try {
            for (JsonNode node : array) {
                String ip = node.path(IP).asText();
                IpAddress ipAddr = ip.isEmpty() ? null : IpAddress.valueOf(ip);
                int port = node.path(PORT).asInt(DEFAULT_TCP_PORT);
                String name = node.path(NAME).asText();
                String password = node.path(PASSWORD).asText();
                deviceInfos.add(new SnmpDeviceInfo(ipAddr, port, name, password));

            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return deviceInfos;
    }

    /**
     * Contains information about a SNMP device retrieved form the net-cfg subsystem.
     */
    public class SnmpDeviceInfo {
        private final IpAddress ip;
        private final int port;
        private final String username;
        private final String password;

        /**
         * Build an information object containing the given device specifics.
         * @param ip ip
         * @param port port
         * @param username username
         * @param password password (a.k.a community in SNMP)
         */
        public SnmpDeviceInfo(IpAddress ip, int port, String username, String password) {
            this.ip = ip;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        /**
         * Returns IpAddress of the device.
         * @return ip
         */
        public IpAddress ip() {
            return ip;
        }

        /**
         * Returns port of the device.
         * @return port
         */
        public int port() {
            return port;
        }

        /**
         * Returns username of the device.
         * @return username
         */
        public String username() {
            return username;
        }

        /**
         * Returns password of the device.
         * @return password
         */
        public String password() {
            return password;
        }
    }

}

