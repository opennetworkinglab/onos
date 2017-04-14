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

package org.onosproject.provider.rest.device.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;
import org.onosproject.protocol.rest.DefaultRestSBDevice;
import org.onosproject.protocol.rest.RestSBDevice;

import java.util.Set;

/**
 * Configuration for RestSB provider.
 * @deprecated 1.10.0 Kingfisher. Please Use RestDeviceConfig
 */
@Deprecated
@Beta
public class RestProviderConfig extends Config<ApplicationId> {

    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String IP = "ip";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String PROTOCOL = "protocol";
    private static final String URL = "url";
    private static final String TESTURL = "testUrl";
    private static final String MANUFACTURER = "manufacturer";
    private static final String HWVERSION = "hwVersion";
    private static final String SWVERSION = "swVersion";

    public Set<RestSBDevice> getDevicesAddresses() throws ConfigException {
        Set<RestSBDevice> devicesAddresses = Sets.newHashSet();

        try {
            for (JsonNode node : array) {
                String ip = node.path(IP).asText();
                IpAddress ipAddr = ip.isEmpty() ? null : IpAddress.valueOf(ip);
                int port = node.path(PORT).asInt(DEFAULT_HTTP_PORT);
                String username = node.path(USERNAME).asText();
                String password = node.path(PASSWORD).asText();
                String protocol = node.path(PROTOCOL).asText();
                String url = node.path(URL).asText();
                String testUrl = node.path(TESTURL).asText();
                String manufacturer = node.path(MANUFACTURER).asText();
                String hwVersion = node.path(HWVERSION).asText();
                String swVersion = node.path(SWVERSION).asText();

                devicesAddresses.add(new DefaultRestSBDevice(ipAddr, port, username,
                                                             password, protocol,
                                                             url, false, testUrl, manufacturer,
                                                             hwVersion, swVersion));
            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return devicesAddresses;
    }

}
