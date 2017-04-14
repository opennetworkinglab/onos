/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;

/**
 * Configuration to push devices to the SNMP provider.
 */
@Beta
public class SnmpDeviceConfig extends Config<DeviceId> {

    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @Override
    public boolean isValid() {
        return hasOnlyFields(IP, PORT, USERNAME, PASSWORD) &&
                ip() != null;
    }

    /**
     * Gets the Ip of the SNMP device.
     *
     * @return ip
     */
    public IpAddress ip() {
        return IpAddress.valueOf(get(IP, extractIpPort().getKey()));
    }

    /**
     * Gets the port of the SNMP device.
     *
     * @return port
     */
    public int port() {
        return get(PORT, extractIpPort().getValue());
    }

    /**
     * Gets the username of the SNMP device.
     *
     * @return username
     */
    public String username() {
        return get(USERNAME, "");
    }

    /**
     * Gets the password of the SNMP device.
     *
     * @return password
     */
    public String password() {
        return get(PASSWORD, "");
    }


    private Pair<String, Integer> extractIpPort() {
        String info = subject.toString();
        if (info.startsWith(SnmpDeviceProvider.SCHEME)) {
            //+1 is due to length of colon separator
            String ip = info.substring(info.indexOf(":") + 1, info.lastIndexOf(":"));
            int port = Integer.parseInt(info.substring(info.lastIndexOf(":") + 1));
            return Pair.of(ip, port);
        }
        return null;
    }
}