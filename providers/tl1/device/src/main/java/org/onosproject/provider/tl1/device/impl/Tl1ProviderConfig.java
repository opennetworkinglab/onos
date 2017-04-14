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
package org.onosproject.provider.tl1.device.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;
import org.onosproject.tl1.impl.DefaultTl1Device;
import org.onosproject.tl1.Tl1Device;

import java.util.Set;

/**
 * Configuration for TL1 provider.
 * @deprecated 1.10.0 Kingfisher
 *
 */
@Deprecated
public class Tl1ProviderConfig extends Config<ApplicationId> {
    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    Set<Tl1Device> readDevices() throws ConfigException {
        Set<Tl1Device> devices = Sets.newHashSet();

        try {
            for (JsonNode node : array) {
                String ip = node.path(IP).asText();
                IpAddress ipAddress = IpAddress.valueOf(ip);
                int port = node.path(PORT).asInt();
                String username = node.path(USERNAME).asText();
                String password = node.path(PASSWORD).asText();
                devices.add(new DefaultTl1Device(ipAddress, port, username, password));
            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return devices;
    }
}
