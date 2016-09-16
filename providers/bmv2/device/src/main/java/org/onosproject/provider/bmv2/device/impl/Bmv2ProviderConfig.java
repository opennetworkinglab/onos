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

package org.onosproject.provider.bmv2.device.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration decoder for Bmv2 provider.
 */
@Beta
public class Bmv2ProviderConfig extends Config<ApplicationId> {
    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String IP = "ip";
    private static final int DEFAULT_THRIFT_PORT = 9090;
    private static final String PORT = "port";

    /**
     * Retrieves a set of Bmv2DeviceInfo containing all the device
     * configuration pertaining to the Bmv2 device provider.
     *
     * @return set of device configurations.
     * @throws ConfigException if configuration can't be read
     */
    public Set<Bmv2DeviceInfo> getDevicesInfo() throws ConfigException {
        Set<Bmv2DeviceInfo> deviceInfos = Sets.newHashSet();

        try {
            for (JsonNode node : array) {
                String ip = node.path(IP).asText();
                IpAddress ipAddr = ip.isEmpty() ? null : IpAddress.valueOf(ip);
                int port = node.path(PORT).asInt(DEFAULT_THRIFT_PORT);
                deviceInfos.add(new Bmv2DeviceInfo(ipAddr, port));

            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return deviceInfos;
    }

    /**
     * Contains information about a Bmv2 device retrieved from the net-cfg
     * subsystem.
     */
    public static class Bmv2DeviceInfo {
        private final IpAddress ip;
        private final int port;

        /**
         * Build an information object containing the given device specifics.
         *
         * @param ip   ip
         * @param port port
         */
        public Bmv2DeviceInfo(IpAddress ip, int port) {
            // TODO use generalized host string instead of IP address
            this.ip = checkNotNull(ip, "ip cannot be null");
            this.port = checkNotNull(port, "port cannot be null");
        }

        /**
         * Returns IpAddress of the device.
         *
         * @return ip
         */
        public IpAddress ip() {
            return ip;
        }

        /**
         * Returns port of the device.
         *
         * @return port
         */
        public int port() {
            return port;
        }
    }

}
