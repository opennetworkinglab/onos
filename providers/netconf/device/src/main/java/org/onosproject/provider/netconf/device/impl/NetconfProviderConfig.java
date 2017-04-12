/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.provider.netconf.device.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;

import java.util.Set;

/**
 * Configuration for Netconf provider.
 * @deprecated 1.10.0 Kingfisher
 */
@Beta
@Deprecated
public class NetconfProviderConfig extends Config<ApplicationId> {

    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String IP = "ip";
    private static final int DEFAULT_TCP_PORT = 830;
    private static final String PORT = "port";
    private static final String NAME = "username";
    private static final String PASSWORD = "password";
    private static final String SSHKEY = "sshkey";

    public Set<NetconfDeviceAddress> getDevicesAddresses() throws ConfigException {
        Set<NetconfDeviceAddress> devicesAddresses = Sets.newHashSet();
        try {
            for (JsonNode node : array) {
                String ip = node.path(IP).asText();
                IpAddress ipAddr = ip.isEmpty() ? null : IpAddress.valueOf(ip);
                int port = node.path(PORT).asInt(DEFAULT_TCP_PORT);
                String name = node.path(NAME).asText();
                String password = node.path(PASSWORD).asText();
                String sshkey = node.path(SSHKEY).asText();
                devicesAddresses.add(new NetconfDeviceAddress(ipAddr, port, name, password, sshkey));

            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return devicesAddresses;
    }

    public class
    NetconfDeviceAddress {
        private final IpAddress ip;
        private final int port;
        private final String name;
        private final String password;
        private final String sshkey;

        public NetconfDeviceAddress(IpAddress ip, int port, String name, String password) {
            this.ip = ip;
            this.port = port;
            this.name = name;
            this.password = password;
            this.sshkey = "";
        }

        public NetconfDeviceAddress(IpAddress ip, int port, String name, String password, String sshkey) {
            this.ip = ip;
            this.port = port;
            this.name = name;
            this.password = password;
            this.sshkey = sshkey;
        }

        public IpAddress ip() {
            return ip;
        }

        public int port() {
            return port;
        }

        public String name() {
            return name;
        }

        public String password() {
            return password;
        }

        public String sshkey() {
            return sshkey;
        }
    }

}